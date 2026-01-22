# MQTT over WSS（TLS）连接与证书校验

## 1. 协议栈与连接阶段

目标连接地址：


wss://<broker-host>:<port>/<path>


协议栈自底向上：


TCP -> TLS -> HTTP Upgrade -> WebSocket -> MQTT


典型阶段：

1. TCP 三次握手（端口可达）
2. TLS Handshake（证书校验、SNI、协商套件）
3. HTTP Upgrade（WebSocket 握手）
4. MQTT CONNECT / CONNACK（认证/会话参数）

**注意**：`telnet` 仅验证 TCP 端口可达，不覆盖 TLS/WebSocket/MQTT。

## 2. TLS 校验机制

TLS 校验包含两类校验：

- **证书链验证**：证书是否由可信 CA 签发
- **主机名校验**：证书 SAN/CN 是否匹配目标主机

常见失败原因：

- 使用 IP 访问，但证书仅包含域名
- 证书链不完整（缺中间证书）
- 证书过期或系统时间偏差
- SNI 不正确导致返回默认证书

## 3. Paho 客户端的关键点（WSS）

Paho 使用 `WebSocketSecureNetworkModule` → `SSLNetworkModule` 处理 WSS：

- TLS 握手前设置 SNI（`host`）
- 依据 `MqttConnectOptions` 控制主机名校验与证书校验
- WebSocket handshake 在 TLS 完成后执行

## 4. 本次改动：信任所有证书 + 关闭主机名校验

配置（prod）：

  ```yml
  mqtt:
    china-tower:
      insecure-ssl: true
  ```
  逻辑：

  1. 构造 TrustManager 实现（空校验）
  2. SSLContext.getInstance("TLS") → init(null, trustManagers, ...)
  3. options.setSocketFactory(sslContext.getSocketFactory())
  4. options.setHttpsHostnameVerificationEnabled(false)
  5. options.setSSLHostnameVerifier((h, s) -> true)

  结果：

  - 证书链不校验
  - 主机名不校验
  - 仍是 TLS 加密通道，但认证失效

  ## 5. 风险评估（安全视角）

  关闭校验后：

  - MITM 可伪造服务器证书
  - 凭证泄露风险 ↑
  - 消息篡改/监听风险 ↑

  适用场景：

  - 受控内网
  - 短期排障
  - 强监管条件下允许的临时方案

  ## 6. 连接确认的工程指标

  建议使用“三层确认”：

  1. connectComplete（连接已建立）
  2. 订阅成功事件
  3. 收到测试消息

  仅有“配置加载/bean started”日志不代表连接成功。

  ## 7. 安全恢复方案（推荐）

  排障结束后恢复安全校验：

  - 用证书匹配的域名访问
  - 配置正确的 CA 链
  - 由对方补齐证书链
  - 确保系统时间正确

  ## 8. 脱敏日志示例

  MQTT连接启动: broker=wss://<broker-host>:<port>/<path>, clientId=<client-id> ...
  MQTT连接成功: broker=wss://<broker-host>:<port>/<path>, clientId=<client-id>, reconnect=false
  MQTT订阅成功: clientId=<client-id>, detail=<detail>
  MQTT连接失败: clientId=<client-id>, reason=<exception>

  ## 9. 术语对照（技术简表）

  - TLS: Transport Layer Security
  - SNI: Server Name Indication
  - SAN: Subject Alternative Name
  - MITM: Man-In-The-Middle
  - CONNACK: MQTT 连接确认响应
