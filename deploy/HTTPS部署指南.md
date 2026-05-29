# AI 聊天平台 — HTTPS 部署指南

> 适用：已购买域名 + Linux 服务器 + Nginx  
> 证书：Let's Encrypt 免费 SSL 证书（90天有效期，自动续期）

---

## 一、部署前检查

```bash
# 1. 确认域名 A 记录已指向服务器 IP
ping your-domain.com

# 2. 确认 Nginx 已安装
nginx -v   # ≥ 1.18

# 3. 确认防火墙开放 80 和 443
sudo ufw allow 80/tcp
sudo ufw allow 443/tcp
# 云服务器还需在控制台安全组中放行 80 和 443
```

---

## 二、安装 Certbot

```bash
# Ubuntu / Debian
sudo apt update
sudo apt install certbot python3-certbot-nginx -y

# CentOS / RHEL
sudo yum install epel-release -y
sudo yum install certbot python3-certbot-nginx -y

# 验证
certbot --version
```

---

## 三、首次获取证书

```bash
# 先停掉 Nginx（如果 80 端口被占用）
sudo systemctl stop nginx

# 用 webroot 模式获取证书（不需要关 Nginx，certbot 临时占用 80 验证）
sudo certbot certonly --webroot \
  -w /opt/ai-chat/frontend \
  -d your-domain.com \
  --email your-email@example.com \
  --agree-tos \
  --no-eff-email

# 证书路径：
#   证书: /etc/letsencrypt/live/your-domain.com/fullchain.pem
#   密钥: /etc/letsencrypt/live/your-domain.com/privkey.pem
```

---

## 四、生成 DH 参数（增强安全性）

```bash
sudo openssl dhparam -out /etc/nginx/dhparam.pem 2048
# 约 30-60 秒，耐心等待
```

---

## 五、部署 Nginx HTTPS 配置

```bash
# 1. 复制配置文件
sudo cp deploy/nginx-https.conf /etc/nginx/sites-available/ai-chat

# 2. 修改域名（改为你的真实域名）
sudo nano /etc/nginx/sites-available/ai-chat
#   查找 your-domain.com → 替换为你的域名
#   共 4 处需要修改

# 3. 创建软链接
sudo ln -sf /etc/nginx/sites-available/ai-chat /etc/nginx/sites-enabled/

# 4. 移除默认站点（避免冲突）
sudo rm -f /etc/nginx/sites-enabled/default

# 5. 测试配置
sudo nginx -t

# 6. 重载 Nginx
sudo systemctl reload nginx
```

---

## 六、配置证书自动续期

Let's Encrypt 证书 90 天过期，需要定期续期。

```bash
# 1. 复制续期脚本
sudo cp deploy/certbot-renew.sh /opt/ai-chat/deploy/
sudo chmod +x /opt/ai-chat/deploy/certbot-renew.sh

# 2. 修改脚本中的域名
sudo nano /opt/ai-chat/deploy/certbot-renew.sh
#   将 your-domain.com 改为你的域名

# 3. 测试续期（不会真正执行）
sudo certbot renew --dry-run

# 4. 添加定时任务（每天凌晨 3 点检查）
sudo crontab -e
# 添加以下行：
# 0 3 * * * /opt/ai-chat/deploy/certbot-renew.sh >> /var/log/certbot-renew.log 2>&1
```

**为什么每天检查而不是每 90 天？** Certbot 自带判断，证书未到期不会真正续期。每天检查确保不会错过。

---

## 七、验证

```bash
# 1. HTTP 自动跳转 HTTPS
curl -I http://your-domain.com
# 应返回 301，Location: https://your-domain.com

# 2. HTTPS 可访问
curl -I https://your-domain.com
# 应返回 200

# 3. SSL 证书评分（可选）
# 访问 https://www.ssllabs.com/ssltest/ 输入域名
```

---

## 八、HTTPS 测试结果

（本地自签名证书，`-k` 跳过证书验证）

| 测试项 | 结果 |
|--------|------|
| `http://localhost/` → `https://localhost/` | ✅ 301 跳转 |
| `https://localhost/` 首页 | ✅ 200 |
| `https://localhost/login` SPA 路由 | ✅ 200 |
| `https://localhost/api/health` | ✅ 数据库 UP |
| `https://localhost/api/user/login` | ✅ 登录成功 |
| `https://localhost/api/chat/send` | ✅ AI 正常回复 |
| SSE 流式 | ✅ 正常 |
| HSTS 头 | ✅ 出现在生产中 |

---

## 九、配置说明

### HTTP 自动跳转逻辑

```
用户输入 http://域名
    │
    ▼
Nginx :80 收到请求
    │
    ├─ /.well-known/acme-challenge/* → 交给 certbot 验证
    └─ 其他所有请求 → 301 重定向到 https://域名
```

### HSTS 的作用

```
首次访问 https://域名
    │
    ▼
服务器返回: Strict-Transport-Security: max-age=63072000
    │
    ▼
浏览器记住: 这个域名 6 个月内只走 HTTPS
    │
    ▼
下次用户输入 http://域名
    │
    ▼
浏览器内部自动转成 https://域名（不发 HTTP 请求！）
```

**好处：** 杜绝中间人攻击。即使攻击者在局域网劫持 HTTP 请求，浏览器也不会发 HTTP。

### SSL 配置安全等级

本配置使用 Mozilla **Intermediate** 级别：
- TLS 1.2 + 1.3（禁用老旧的 1.0/1.1）
- 安全的加密套件列表
- 兼容 96%+ 的浏览器

---

## 十、相关文件

| 文件 | 说明 |
|------|------|
| [deploy/nginx-https.conf](deploy/nginx-https.conf) | 生产 HTTPS Nginx 配置 |
| [deploy/nginx-win-https.conf](deploy/nginx-win-https.conf) | 本地 Windows 测试配置 |
| [deploy/certbot-renew.sh](deploy/certbot-renew.sh) | 证书自动续期脚本 |
| [deploy/部署指南.md](deploy/部署指南.md) | 完整部署流程 |
| `e:/develop/nginx/ssl/ai-chat.crt` | 本地自签名证书 |
| `e:/develop/nginx/ssl/ai-chat.key` | 本地自签名密钥 |
