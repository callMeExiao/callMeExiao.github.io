---
title: 八股文TellMeWhy之Redis
createTime: 2025/06/21 19:20:25
permalink: /article/anoo9ub2/
---

OK Let's Dive in!🤿

## Redis 为什么快

### 内存操作

🔴访问内存 (DRAM) vs 访问磁盘 (SSD/HDD) 的速度差异是 100ns vs 100us vs 10ms (10 万倍差距)。这是 Redis 速度的物理基础。

⚫当数据全在内存后，瓶颈就从磁盘 I/O 变成了网络 I/O和CPU 处理能力。这也是 Redis 设计核心逻辑集中在网络和 CPU 效率优化的原因。

⚫内存掉电数据即失，引出持久化问题。

⚫内存比磁盘贵得多，限制了数据集大小。

### 单线程模型

纯单线程模型： 所有核心操作都在一个线程内完成：

- 监听网络连接 (bind, listen, accept)
- I/O 多路复用 (epoll/kqueue/select 事件循环)
- Socket 数据读写
- Redis 协议解析与请求构造
- 命令查找、参数解析、执行
- 内存数据结构操作
- 生成响应、写入 Socket
- 过期 Key 扫描（部分逻辑）
- RDB/AOF 持久化 fork 操作（bgsave, bgrewriteaof 由主线程 fork，但持久化本身在子进程）

🔴单线程模型避免了多线程的上下文切换和锁的开销。

- 锁竞争： 多线程环境下，共享数据结构需要锁保护（如全局哈希表）。锁的获取、释放、等待在高并发时消耗巨大 CPU，且极易引发性能瓶颈和复杂
  bug。
- 上下文切换： 线程数 > CPU 核数时，操作系统频繁切换线程执行，保存 / 恢复寄存器状态、更新内核数据结构等操作消耗可观 CPU 时间。
- 设计哲学：简单、高效、可控。 单线程消除了线程安全问题，让内部实现（如数据操作、事件处理）变得极其清晰和可预测。

⚫单线程吞吐量存在理论上限（取决于单核性能和网络带宽）

⚫单个命令计算不能并行加速（这是引入网络 I/O 线程的根本原因）。

⚫长命令阻塞： 任何耗时操作（如KEYS *, FLUSHALL) 或大 Key 操作（如读 / 写一个包含百万字段的
Hash），都会阻塞整个实例，导致所有后续请求延迟飙升。这是单线程模型与生俱来的问题。

- 绝对禁用 KEYS → 使用 SCAN 增量迭代。
- 避免大 Key → 拆分数据结构（如大 Hash 按字段 Hash 分桶）。
- 避免大 Value → 压缩数据（如使用 GZIP 压缩）。
- 查询优化 → HSCAN 替代 HGETALL，ZRANGE ... LIMIT 替代取全量。
- 合理使用管道 → 减少网络 RTT 次数，一次批处理多个命令，提升单线程利用率。

### I/O 多路复用(epoll)

"I/O 多路复用" 技术，就是一种让单线程（或少量线程）能够通过高效监控机制，同时管理服务大量网络连接（进行 I/O 操作）的方法，其核心精髓在于
共享使用操作系统提供的高效事件监控能力（epoll）以及有限的 CPU 线程资源，从而实现高并发、高效率。

- 复用 OS 事件监控机制： 内核一次系统调用epoll_wait可以报告多个连接的多个事件，不用每个连接单独查状态。
- 复用 CPU 线程时间： 线程只在有真实的 I/O 工作要做时才被唤醒执行，避免在等待 I/O 上浪费 CPU。沉睡时 CPU 可 “复用” 于其他任务。
- 复用同一套处理逻辑： 同一个线程按序处理所有就绪连接的 I/O 操作。

#### 技术实现

使用 epoll 实现 I/O 多路复用，支持海量连接。同时也是目前 Linux 上最高效的 I/O 多路复用机制。它提供了三个关键系统调用：

1. epoll_create：创建一个 epoll 实例，返回一个文件描述符(fd)指向内核中的 epoll 实例。。
2. epoll_ctl(epfd, op, fd, event)：向 epoll 实例添加、修改或删除文件描述符。（ 在连接生命周期中调用次数少，只在 fd
   状态改变时调用，如新连接加入、修改监听事件、关闭连接时删除 ）
    - epfd：epoll_create 返回的 fd。
    - op：操作类型（EPOLL_CTL_ADD 添加，EPOLL_CTL_MOD 修改，EPOLL_CTL_DEL 删除）。
    - fd：要操作的 Socket fd。
    - event：关心的事件（EPOLLIN 有数据可读，EPOLLOUT 可写，EPOLLET 边缘触发模式）。
3. epoll_wait(epfd, events, maxevents, timeout)：等待文件描述符上的事件。
    - epfd：epoll 实例 fd。
    - events：一个数组，由 epoll_wait 填充发生的事件（哪些 fd 有需求）。
    - maxevents：events 数组的大小（一次最多拿多少通知）。
    - timeout：超时时间（最多等多久，-1 表示无限等待）。
    - 返回值： 就绪事件列表。
    - 核心优势： 内核将就绪事件列表通过 events 直接返回给用户空间。避免全量扫描 fd 集合，复杂度 O (1) 或 O (就绪的 fd 数)。

#### 触发模式

##### 水平触发 (Level-Triggered, LT) - Redis 5.0 默认

只要 fd 满足监听的条件（如接收缓冲区有数据未读完），每次调用 epoll_wait 都会通知。

🔴编码简单安全。应用程序只需关心读 / 写操作即可，不用一次性处理完所有数据。

⚫可能会多次通知同一个事件（如果程序不处理完）。

##### 边缘触发 (Edge-Triggered, ET)

只在 fd 状态变化时通知一次（如接收缓冲区从空变为非空）。后续即使缓冲区还有数据未读完，只要没有新数据到达（状态再次变化），就不再通知。

🔴理论上通知次数更少，性能略优。

⚫编程复杂度高， 应用必须在收到通知后，非阻塞地循环读 / 写 Socket，直到系统调用返回 EAGAIN 或
EWOULDBLOCK（表示暂时没有数据可读或发送缓冲区满），否则会遗漏事件。 更容易出错，可能导致数据读取不全或饿死（忘记处理导致认为
fd 没事了）。

#### 高效内核

1. 红黑树 (Red-Black Tree)
    - 内核用红黑树组织 epoll_ctl 注册的所有 fd（高效支持 ADD, MOD, DEL 操作，O (log n)）。
    - 解决 select/poll 中内核需线性扫描的问题。
2. 就绪链表 (Ready List)
    - 当 fd 状态变化（有事件就绪）时，内核中断处理程序将其插入一个就绪链表中（而不是立即通知用户）。
    - epoll_wait 只需检查这个链表是否有节点即可（近乎 O (1)）。
3. 回调机制 (Callback)
    - 内核为每个注册的 fd 关联一个回调函数。
    - 当 fd 的 I/O 状态发生变化时（如网卡数据到达填入接收缓冲区），中断处理会触发回调函数。
    - 回调函数的工作：检查这个变化是否让 fd 产生了我们关心的事件？如果是，就把该 fd 插入就绪链表。

### 数据结构

Redis 5.0 的数据结构优化已基本成熟。它们不仅提供了丰富的语义（String, Hash, List, Set, Sorted Set
等），更在内存使用和操作效率之间找到了精妙的平衡，完美服务于 Redis 的单线程内存数据库场景。关键在于：

1. 动态、高效： 针对不同数据规模和类型，采用多种底层编码，自动转换（Redis会在后台悄悄改变数据结构的内部编码 以适应数据变化）。
2. 空间敏感： 对小对象、整数、短字符串有极致优化（ziplist, intset）。
3. 时间复杂度保证： 大部分常用操作达到 O (1) 或对数复杂度。
4. 内存布局紧凑： 减少碎片，提高缓存命中率。

#### 字符串 (String)

最基本类型，可存储文本、二进制数据、整数、浮点数。最大 512MB。

核心设计：

- Strlen：Strlen 命令用于获取指定 key 所储存的字符串值的长度。当 key 储存的不是字符串值时，返回一个错误。
- 内存预分配：每次分配内存时会高出实际字符串的length（长度 <1MB 则加倍，>=1MB 则加 1MB），避免频繁的小幅度内存分配和缓冲区溢出风险。
- 惰性释放：缩短字符串后，多出来的空间先留着不立刻还给系统，下次扩容可能直接用上，减少系统调用。

应用场景：

- 数据缓存：缓存用户信息（JSON 字符串）、商品详情
   ```redis
   SET user:1001 '{name:"Lucy", age:28}' EX 300
   ```
- 计数器：秒杀库存、文章阅读量
   ```redis
   INCR article:1001:views → 利用整数编码省内存
   ```
- 分布式锁：利用原子性实现互斥
   ```redis
   SET lock_key unique_value NX EX 30
   ```
- 限流器：滑动窗口计数（INCR + EXPIRE）

#### 列表 (List)

有序元素集合，可重复。支持头尾高效插入 / 删除 (LPUSH/RPUSH, LPOP/RPOP)，中间相对低效。

核心设计：

- Quicklist（链表 + 小型压缩列表）：平衡头尾操作效率与内存消耗
- list-max-ziplist-size：限制单个节点大小防止连锁更新

典型场景：

- 消息队列：简易版任务队列（生产消费模型）
   ```redis
    生产者 LPUSH task:queue job_data
    消费者 RPOP task:queue
   ```
- 时间线：微信朋友圈动态
   ```redis
    LPUSH user:1001:timeline "新动态内容"（新内容插头部）
  ```
- 最新商品列表：电商首页推荐最新 50 个商品
   ```redis
    LTRIM hot_products 0 49 裁剪旧数据
  ```
- 阻塞队列：**BRPOP** 实现消息阻塞获取

#### 哈希（Hash）

键值对集合（field-value 映射）。

核心设计：

- 双编码策略：小对象压缩存储 (ziplist)，大对象哈希表 (hashtable)
- 单命令操作字段：避免传输整个对象

典型场景：

- 购物车： 用户 ID 为 key，商品 ID 为 field，数量为 value
   ```redis
    添加某商品数量 HSET cart:1001 prod_001 3
    修改某商品数量 HINCRBY cart:1001 prod_001 -1
    ```
- 用户属性存储：储存用户资料片段（优于 String 存整段 JSON）
   ```redis
    HMSET user:1001 name Lucy age 28 city Beijing
  ```
- 配置中心：存储服务动态开关项（如feature_flag:auto_audit）

#### 集合（Set）

无序唯一元素集合。支持交集、并集、差集。

核心设计：

- 整数集合优化 (intset)：纯小整数节省空间
- 哈希表兜底：支持非整数 / 大数据量 O (1) 查找

典型场景：

- 标签系统：用户兴趣标签
    ```redis
    SADD user:1001:tags 科技 音乐 旅行 桌游
    SADD user:1002:tags 音乐 旅行 烘焙 骑行
    取出交集：SINTER user:1001:tags user:1002:tags
     ```
- 抽奖去重：确保用户不重复中奖
    ```redis
    SADD lottery:2023 1001 1002 1003
    ```
- 关注关系：微博粉丝列表
    ```redis
    SADD user:1001:followers 2001 2002 2003 （需配合 SCARD 计数）
    ```

#### 有序集合（Sorted Set）

唯一元素集合，每个元素关联一个分数 (score)。可按键 (member) 或按分数范围高效访问。

核心设计：

- 双引擎驱动：跳表（范围操作）+ 哈希表（单点查询）
- 分数排序：支持浮点数，可自定义权重计算（如时间 + 热度）

典型场景：

- 实时排行榜：
    ```redis
    游戏玩家战力榜：ZADD leaderboard 2500 player:1001
    获取 Top10：ZREVRANGE leaderboard 0 9 WITHSCORES
    ```
- 延迟队列：订单超时处理
    ```redis
    ZADD delay_queue <超时时间戳> order:1001
    消费：ZRANGEBYSCORE delay_queue 0 <当前时间戳> LIMIT 0 1
    ```
- 热点搜索词统计：按点击量排序关键词
    ```redis
    点击 “Java”：ZINCRBY hot_keywords 1 "Java"
    ```

#### HyperLogLog

用于估计一个集合中不重复元素数量 (基数) 的算法。占用内存极小（~12KB），误差率约 0.81%。

核心设计：
- 概率算法：12KB 内存估算亿级独立访客（误差 0.81%）
- 去重统计：只计数不存原始数据

典型场景：
- 网站 UV 统计：每日独立访客数（无需存储每个 UserID）
    ```redis
    PFADD uv:20231105 user_ip1 user_ip2
    PFCOUNT uv:20231105 → 返回估算值
    ```
- 裂变活动统计：分享链路中独立用户触达量

#### Bitmap

本质上不是独立数据结构，而是 基于 String 类型 操作的位数组。

核心设计：
- 位操作指令：基于 String 的二进制位存取
- 内存压缩：连续位自动内存紧凑

典型场景：

- 用户签到日历：按日期偏移量标记状态
    ```redis
    签到 10 月 1 日：SETBIT sign:1001 0 1
    统计当月签到天数：BITCOUNT sign:1001
    ```
- 活跃用户画像：标记具有某特征的用户（如 VIP）
    ```redis
    标记用户 1001 为 VIP：SETBIT vip_users 1001 1
    ```
- 布隆过滤器 (需客户端实现)：快速判断元素是否可能存在

#### Geospatial

用于存储地理位置信息，并支持基于距离的查询。

核心设计：
- GeoHash 编码：将经纬度映射为 ZSet 的 Score
- 底层 ZSet 支撑：所有操作复用有序集合能力

典型场景：

- 附近的人：基于半径范围查询
    ```redis
    GEORADIUS users:location 116.40 39.90 5 km WITHDIST
    ```
- 网点选址：查询某区域内所有便利店
    ```redis
    GEORADIUS stores 116.40 39.90 2 km
    ```
- 配送路径优化：计算骑手到商家的距离
    ```redis
    GEODIST rider:001 store:1001 km
    ```

## 持久化机制

## 过期键删除策略

## 内存淘汰策略

## 主从复制原理

## Sentinel 高可用

## Cluster 集群分片

## 底层数据结构实现

## 缓存问题经典三问

## 分布式锁