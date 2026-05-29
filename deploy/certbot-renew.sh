#!/bin/bash
# ============================================================
# AI 聊天平台 — Let's Encrypt 证书自动续期脚本
# ============================================================
# 安装 certbot:
#   Ubuntu: sudo apt install certbot python3-certbot-nginx
#   CentOS: sudo yum install certbot python3-certbot-nginx
#
# 首次获取证书:
#   sudo certbot certonly --webroot -w /opt/ai-chat/frontend -d your-domain.com
#
# 测试续期（不实际执行）:
#   sudo certbot renew --dry-run
#
# 设置定时任务:
#   sudo crontab -e
#   0 3 * * * /opt/ai-chat/deploy/certbot-renew.sh >> /var/log/certbot-renew.log 2>&1
# ============================================================

LOG_FILE="/var/log/certbot-renew.log"
DOMAIN="your-domain.com"

echo "[$(date '+%Y-%m-%d %H:%M:%S')] 开始证书续期检查..."

# 执行续期
if /usr/bin/certbot renew --quiet --deploy-hook "systemctl reload nginx"; then
    echo "[$(date '+%Y-%m-%d %H:%M:%S')] 续期检查完成（证书未到期或已成功续期）"

    # 检查证书有效期
    CERT_FILE="/etc/letsencrypt/live/${DOMAIN}/fullchain.pem"
    if [ -f "$CERT_FILE" ]; then
        EXPIRY=$(/usr/bin/openssl x509 -enddate -noout -in "$CERT_FILE" | cut -d= -f2)
        echo "  证书到期时间: $EXPIRY"
    fi
else
    echo "[$(date '+%Y-%m-%d %H:%M:%S')] 续期失败！请检查日志"
    exit 1
fi
