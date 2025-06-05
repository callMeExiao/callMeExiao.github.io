---
title: Websocket
createTime: 2025/06/05 16:47:53
permalink: /article/3j5iyl79/
---

# Websocket

## 诞生背景

### HTTP 协议处理实时通信的核心痛点

1. 单向通信模型

    - 本质问题：HTTP 基于请求 - 响应模式（客户端发起 → 服务端响应）
    - 后果：服务端无法主动推送数据，例如股价变动、聊天消息需等待客户端轮询
    - 案例：使用 AJAX 轮询时，95% 的请求可能仅返回 "无新数据"

2. 高延迟与高开销

    - 短轮询：客户端定时频繁请求，大量无效请求占用带宽
    - 长轮询：服务端阻塞请求直到有数据，服务端线程 / 连接资源浪费
    - 协议冗余：单次 HTTP 请求平均头信息 800+ 字节，每次请求重复传输 Cookie、User-Agent 等头部

### Websocket核心优势

1. 全双工通信：破除单向枷锁

    - 客户端和服务端可以同时发送和接收数据，无需额外的轮询或请求

2. 低延迟与低开销：帧结构优化

    - 最小 2 字节帧头
    - 相同网络环境下，WebSocket 消息延迟 ≈ HTTP 轮询的 1/20

3. 连接复用：降低系统损耗

    - 资源节省：1 个 WebSocket 连接 ≈ 减少 10,000+ 次 HTTP 连接重建
    - 线程压力：Tomcat 默认支持 10K HTTP 并发线程 → 同等硬件支持 100K+ WebSocket 连接

4. 协议兼容性：无缝嵌入现有架构

    - 握手阶段：复用 HTTP 端口（80 / 443），通过 Upgrade: websocket 切换协议
    - 穿透性：兼容企业防火墙策略（模拟 HTTP 流量）

5. 二进制支持：高性能数据传输

    - 性能提升：二进制传输效率比 HTTP Base64 编码高 40%
    - 适用场景：在线游戏状态同步、物联网传感器数据

6. 心跳保活：自动维持连接

    - 解决痛点：NAT 网关超时断开问题（默认 5 分钟）
    - 自主设置：客户端和服务端通过 ping/pong 帧保持连接，服务端可以设置超时时间，自动断开连接

### 应用场景

在需要高频双向交互的场景中，WebSocket 是唯一能同时满足低延迟、高吞吐、低资源消耗的方案。

- 金融交易系统（高频低延迟）
- 在线游戏（实时状态同步）
- 协同编辑（冲突解决）
- 物联网监控（海量设备接入）
- 实时通讯（IM 系统）
- 直播互动（高并发写入）
- 在线教育（双路流协同）

## 基本概念

WebSocket 是一种基于 TCP 的全双工通信协议，通过在单个长连接上实现双向数据流通，彻底解决了 HTTP 协议在实时场景的瓶颈。

### 关键特性

- 双向通信：客户端 / 服务端可随时主动发送消息
- 持久化连接：单次握手建立连接，后续无需重复握手
- 低延迟传输：最小 2 字节帧头
- 二进制支持：原生支持文本 / 二进制数据传输
- 跨域兼容：基于 HTTP 端口（80/443），避免防火墙拦截

### 工作原理

1. 协议握手（HTTP Upgrade）

   通过 HTTP 101 状态码完成协议切换，后续通信脱离 HTTP 规范：
    1. 客户端发起 HTTP 请求，Upgrade: websocket 切换协议
    2. 服务端响应 101 Switching Protocols，建立 WebSocket 连接
   ```plaintext
   GET /chat HTTP/1.1
   Host: server.example.com
   Upgrade: websocket          # 协议升级标识
   Connection: Upgrade          # 连接升级指令
   Sec-WebSocket-Key: dGhlIHNhbXBsZSBub25jZQ==  # 客户端密钥
   Sec-WebSocket-Version: 13    # 协议版本
   
   HTTP/1.1 101 Switching Protocols
   Upgrade: websocket
   Connection: Upgrade
   Sec-WebSocket-Accept: s3pPLMBiTxaQ9kYGzzhZRbK+xOo=  # 服务端校验值
   ```

2. 数据帧结构（RFC 6455）

    - FIN (1 bit)：标识是否为最终数据帧
    - Opcode (4 bits)：帧类型标识符
    - %x0：延续帧 %x1：文本帧 %x2：二进制帧
    - %x8：关闭帧 %x9：Ping 帧 %xA：Pong 帧
    - Mask (1 bit)：客户端到服务端的消息必须掩码处理
    - Payload Len (7/16/64 bits)：自适应长度标识

   ```plaintext
    0                   1                   2                   3
    0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1
   +-+-+-+-+-------+-+-------------+-------------------------------+
   |F|R|R|R| opcode|M| Payload len |    Extended payload length    |
   |I|S|S|S|  (4)  |A|     (7)     |             (16/64)           |
   |N|V|V|V|       |S|             |   (if payload len==126/127)   |
   | |1|2|3|       |K|             |                               |
   +-+-+-+-+-------+-+-------------+ - - - - - - - - - - - - - - - +
   |     Extended payload length continued, if payload len == 127  |
   + - - - - - - - - - - - - - - - +-------------------------------+
   |                               | Masking-key, if MASK set to 1 |
   +-------------------------------+-------------------------------+
   | Masking-key (continued)       |          Payload Data         |
   +-------------------------------- - - - - - - - - - - - - - - - +
   :                     Payload Data continued ...                :
   + - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - +
   |                     Payload Data continued ...                |
   +---------------------------------------------------------------+
   ```

### 核心组件

1. 会话管理（Session 对象）

2. 端点配置（EndpointConfig）

3. 心跳机制（Ping/Pong）

## 代码示例

### 连接生命周期

1. 建立连接（OnOpen）

   ```java
   
   ```
2. 消息接收（OnMessage）

   ```java
   
   ```
3. 连接关闭（OnClose）

   ```java
   
   ```
4. 异常处理（OnError）

   ```java
   
   ```

---
`⌚` 2025-06-06 20:26 | `📍` 国大西西弗 | `📝` 复健模式
