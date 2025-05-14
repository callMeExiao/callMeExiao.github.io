---
title: Nginx
createTime: 2025/04/22 20:14:37
permalink: /article/m55hg40w/
---

# Nginx 在 Linux 环境中的应用

## 流量镜像Mirror

### 背景

将原来微信支付回调通知地址从C#项目服务器（以下统称为C）改为我的Java项目服务器（以下统称为J）。
但同时得保证现有业务的正常运行，所以需要将J的流量镜像到C，并且更改回调通知地址为J。

### 实现

```
    # /api/wxnotify/refund
    location /api/wxnotify/v3/refund {
        # 静默镜像到旧服务A
        mirror /refundMirror;
        mirror_request_body on;  # 复制请求体（POST/PUT需要）

        # 转发到本地
        proxy_pass http://localhost:8080;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;

        # 超时设置（可选）
        proxy_connect_timeout 60s;
        proxy_read_timeout 600s;
        proxy_send_timeout 600s;
        # 镜像专用日志
        access_log /var/log/nginx/mirror.log mirror_log;
    }
    
    # refundMirror
    location = /refundMirror {
        internal;  # 禁止外部直接访问
        proxy_pass http://{C域名}/API/WXTuiFeiNotify.aspx; 

        # 代理头配置（需匹配A服务的Host）
        proxy_set_header Host {C域名};
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;

        # 请求体处理
        proxy_pass_request_body on;
        proxy_set_body $request_body;

        # 超时配置（镜像请求可设置较短超时）
        proxy_connect_timeout 5s;
        proxy_read_timeout 5s;

        # 启用镜像日志
        access_log /var/log/nginx/mirror.log mirror_log;
    }
```