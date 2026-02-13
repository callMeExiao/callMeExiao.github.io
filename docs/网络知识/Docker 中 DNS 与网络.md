---
title: Docker DNS 与网络详解：Compose 到 ECS 实战全指南
description: 一篇讲透 Docker DNS 解析与容器网络的实战文章，覆盖 bridge/host/overlay、<DOCKER_DNS_IP>、extra_hosts、dns 配置、ECS 部署排障与最佳实践。
keywords:
  - Docker DNS
  - Docker 网络
  - Docker Compose DNS
  - extra_hosts
  - ECS Docker 部署
  - Docker 网络排障
slug: docker-dns-network-compose-ecs-guide
createTime: 2026/02/09 15:53:36
permalink: /article/rdmhyjn0/
---

# Docker DNS 与网络详解：Compose 到 ECS 实战全指南

---

## 一、先给你答案

1. **容器网络通信**核心是 Linux Namespace + veth + bridge + NAT。
2. **容器 DNS 默认并不是直接查公网 DNS**，在自定义网络中通常先走 Docker 内置 DNS（`<DOCKER_DNS_IP>`）。
3. `dns` 的作用是配置“上游 DNS 服务器”，不负责把某个域名固定到某个 IP。
4. `extra_hosts` 的作用是把映射写进容器 `/etc/hosts`，适合“强制域名 -> IP”场景。
5. 服务间互访优先使用 Compose 服务名（如 `service-a:8001`），而不是容器 IP。
6. 排查时先看三处：`/etc/hosts`、`/etc/resolv.conf`、`docker network inspect`。

---

## 二、本文适合谁

你可能正处于以下场景之一：

- Docker Compose 项目里，部分容器能访问域名，部分不行。
- 在阿里云 ECS 上部署后，容器里 `curl` 某域名失败。
- 你不确定应该用 `dns` 还是 `extra_hosts`。
- 你用 sidecar（比如 `sidecar-runtime`）后，发现主容器与 sidecar 解析行为不一致。
- 你要给生产环境写一份可维护的网络与 DNS 运维规范。

如果你符合其中任意一条，继续往下看就对了。

---

## 三、目录

- [四、Docker 网络底层：容器为什么能通信](#四docker-网络底层容器为什么能通信)
- [五、Docker 网络模式对比](#五docker-网络模式对比)
- [六、Docker DNS 解析全流程](#六docker-dns-解析全流程)
- [七、Compose 中 `dns` 与 `extra_hosts` 的边界](#七compose-中-dns-与-extra_hosts-的边界)
- [八、实战案例：仅给 `service-a` 添加域名映射](#八实战案例仅给-service-a-添加域名映射)
- [九、ECS 场景排障手册（可直接执行）](#九ecs-场景排障手册可直接执行)
- [十、常见误区与最佳实践](#十常见误区与最佳实践)
- [十一、FAQ（高频搜索问题）](#十一faq高频搜索问题)
- [十二、总结](#十二总结)

---

## 四、Docker 网络底层：容器为什么能通信

### 4.1 四个关键组件

- **Network Namespace**：容器各自拥有独立网络栈（网卡、路由、端口空间）。
- **veth pair**：一端在容器内（通常是 `eth0`），另一端挂到宿主机 bridge。
- **bridge（网桥）**：同网桥容器二层互通。
- **iptables/NAT**：容器访问外网时 SNAT，外部访问容器端口时 DNAT。

### 4.2 三条典型流量路径

- **容器到容器（同网络）**：容器 A -> bridge -> 容器 B。
- **容器到公网**：容器 -> bridge -> 宿主机 NAT -> Internet。
- **公网到容器**：公网 -> ECS 端口 -> DNAT -> 容器端口。

理解这三条路径，基本就能解释 80% 的“Docker 网络不通”问题。

---

## 五、Docker 网络模式对比

### 5.1 `bridge`（默认推荐）

- 隔离性和可维护性最好，最适合大多数业务。
- 自定义 bridge 网络下支持 Docker DNS 服务发现（服务名解析）。

### 5.2 `host`

- 容器直接使用宿主机网络栈，不需要 `-p` 映射。
- 性能高，但隔离性差，端口冲突风险高。

### 5.3 `none`

- 仅 loopback，无外部网络连接。
- 用于高隔离任务或离线作业。

### 5.4 `overlay`

- 常见于 Swarm/K8s 跨主机场景。
- 跨节点网络能力强，但复杂度更高。

---

## 六、Docker DNS 解析全流程

### 6.1 为什么容器里会出现 `<DOCKER_DNS_IP>`

在 user-defined network 中，容器常见 `resolv.conf`：

```conf
nameserver <DOCKER_DNS_IP>
options ndots:0
```

这意味着容器先问 Docker 内置 DNS，再由它向上游 DNS 递归查询。

### 6.2 服务名解析是怎么来的

在同一 Compose 网络中可直接访问：

```text
http://service-a:8001
http://redis:6379
```

这不是公网 DNS 解析，而是 Docker 内置服务发现。

### 6.3 解析优先级（非常关键）

多数 Linux 发行版 `nsswitch.conf` 为：

```text
hosts: files dns
```

即：

1. 先查 `/etc/hosts`
2. 再查 DNS

所以 `extra_hosts` 生效后，经常会覆盖 DNS 返回结果。

---

## 七、Compose 中 `dns` 与 `extra_hosts` 的边界

这是搜索量最高、也最容易混淆的一点。

### 7.1 `dns`：指定上游 DNS 服务器

```yaml
services:
  gateway-service:
    dns:
      - <DNS_SERVER_1>
      - <DNS_SERVER_2>
```

适用场景：

- 你要指定企业内网 DNS。
- 宿主机默认 DNS 不可靠。
- 想统一容器 DNS 递归路径。

不适用场景：

- 把某一个域名强制绑定到某一个 IP。

### 7.2 `extra_hosts`：静态写入 `/etc/hosts`

```yaml
services:
  service-a:
    extra_hosts:
      - "<TARGET_DOMAIN>:<TARGET_IP>"
```

适用场景：

- 联调临时切流。
- 上游 DNS 还没发布或不在你控制范围。
- 你必须保证某个容器固定访问某个 IP。

注意事项：

- 修改后需重建容器才能稳定生效。
- IP 变更要人工维护，长期使用要有审计机制。

### 7.3 `x-extra_hosts` 是什么

`x-` 前缀是 Compose 扩展字段，常用于锚点复用：

```yaml
x-extra_hosts: &default-extra_hosts
  - "<INTERNAL_DOMAIN>:${HOST_IP_PLACEHOLDER}"

services:
  web:
    extra_hosts: *default-extra_hosts
```

有些 IDE 会对扩展字段做严格 schema 校验，出现“应为单值/类型不匹配”提示。  
这通常是编辑器规则提示，不等于运行时一定报错。

---

## 八、实战案例：仅给 `service-a` 添加域名映射

> 目标：只让 `service-a` 具备 `<TARGET_DOMAIN> -> <TARGET_IP>`，避免影响其他容器。

### 8.1 推荐配置（最小影响面）

```yaml
services:
  service-a:
    extra_hosts:
      - "<INTERNAL_DOMAIN_A>:<TARGET_IP>"
      - "<TARGET_DOMAIN>:<TARGET_IP>"
```

这样做的好处：

- 只影响目标服务。
- 风险最小，回滚简单。
- 避免公共锚点扩散带来的隐性副作用。

### 8.2 生效命令（ECS 常用）

```bash
docker compose -f deploy/docker-compose.yml up -d --force-recreate service-a
```

## 九、ECS 场景排障手册（可直接执行）

### 9.1 检查容器内 hosts/resolv

```bash
docker exec service-a sh -lc 'cat /etc/hosts'
docker exec service-a sh -lc 'cat /etc/resolv.conf'
```

### 9.2 检查解析结果

```bash
docker exec service-a sh -lc 'getent hosts <TARGET_DOMAIN>'
# 若镜像无 getent，可退化为
# docker exec service-a sh -lc 'grep -n "<TARGET_DOMAIN>" /etc/hosts'
```

### 9.3 检查 Compose 静态配置

```bash
docker compose -f deploy/docker-compose.yml config
```

### 9.4 检查容器网络归属

```bash
docker inspect service-a --format '{{json .NetworkSettings.Networks}}'
docker network ls
# 将 <network_name> 替换为上面查到的实际网络名（常见如 project_default）
docker network inspect <network_name>
```

### 9.5 一张排障决策表

| 现象 | 高概率原因 | 优先检查 |
|---|---|---|
| 容器中域名解析到旧 IP | `/etc/hosts` 有历史映射 | `cat /etc/hosts` |
| 同域名在不同容器结果不同 | `extra_hosts` 作用域不一致 | 各容器 `extra_hosts` 配置 |
| 服务名无法解析 | 不在同一 Docker 网络 | `docker network inspect` |
| 偶发解析超时 | 上游 DNS 不稳定 | `dns` 配置与连通性 |
| Compose 通过但运行不通 | 运行态网络/防火墙问题 | 安全组、路由、容器日志 |

---

## 十、常见误区与最佳实践

### 10.1 常见误区

1. 配了 `dns` 就等于把域名绑到 IP。
2. DNS 问题一定是公网 DNS 坏了。
3. 所有容器都应该共享同一组 `extra_hosts`。
4. `docker compose config` 成功就等于线上可用。

### 10.2 推荐实践

- 服务互调优先用服务名，不硬编码容器 IP。
- 仅在“需要该域名解析”的服务中配置 `extra_hosts`。
- 给每条静态映射标注用途、责任人、计划回收时间。
- 将 DNS 排障命令写入 Runbook，减少故障恢复时间。
- 定期审计历史映射，避免“僵尸配置”。

---

## 十一、FAQ

### Q1：Docker `dns` 和 `extra_hosts` 到底有什么区别？

`dns` 决定“问谁（DNS 服务器）”；`extra_hosts` 决定“先写死答案（/etc/hosts）”。

### Q2：为什么我在宿主机能解析，容器里不行？

容器 DNS 可能走了不同链路（`<DOCKER_DNS_IP>` + 上游 DNS），且容器内 `/etc/hosts` 可能覆盖了解析结果。

### Q3：`extra_hosts` 会不会覆盖真实 DNS？

通常会。因为多数系统优先查 `files`（`/etc/hosts`）再查 `dns`。

### Q4：在 ECS 上应该优先 `extra_hosts` 还是内网 DNS？

短期联调/临时切流优先 `extra_hosts`；长期稳定策略优先内网 DNS，运维成本更低。

### Q5：只改一个服务能不能生效？

可以。`extra_hosts` 是容器级配置，按服务粒度生效，最符合“最小影响面”原则。

---

## 十二、总结

Docker 网络与 DNS 本质上是三个层面的组合：

1. **网络可达性**：namespace + bridge + route + NAT。
2. **名称解析链路**：`/etc/hosts` 与 DNS 的优先级关系。
3. **配置作用域控制**：仅在必要服务配置 `extra_hosts`。

如果你把这三层拆开排障，绝大多数 “Docker DNS/网络问题” 都能快速定位。

---

## 附录 A：可直接复用的最小配置片段

```yaml
services:
  service-a:
    image: your-image
    extra_hosts:
      - "<INTERNAL_DOMAIN_A>:<TARGET_IP>"
      - "<TARGET_DOMAIN>:<TARGET_IP>"
    networks:
      - net

networks:
  net:
    external: true
    name: eth0
```

---
