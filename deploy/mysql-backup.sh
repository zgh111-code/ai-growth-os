#!/bin/bash
# ============================================================
# AI 聊天平台 — MySQL 自动备份脚本
# ============================================================
# 功能：
#   1. 使用 mysqldump 导出 ai_chat 数据库
#   2. 按日期命名：ai_chat_20260511_030001.sql.gz
#   3. 自动删除 7 天前的旧备份
#   4. 记录日志到 /var/log/ai-chat-backup.log
#
# 凭证：从 /etc/mysql/backup.cnf 读取（权限 600，不含密码）
# 定时：crontab -e → 0 3 * * * /opt/ai-chat/deploy/mysql-backup.sh
# ============================================================

# 注意：不设 set -e，用显式错误处理（否则 if 检查失效）
BACKUP_DIR="/opt/ai-chat/backups"
DATABASE="ai_chat"
MYSQL_CNF="/etc/mysql/backup.cnf"
LOG_FILE="/var/log/ai-chat-backup.log"
RETENTION_DAYS=7

# ========== 日志函数 ==========
log() {
    echo "[$(date '+%Y-%m-%d %H:%M:%S')] $1" | tee -a "$LOG_FILE"
}

# ========== 创建备份目录 ==========
mkdir -p "$BACKUP_DIR"

# ========== 检查凭证文件 ==========
if [ ! -f "$MYSQL_CNF" ]; then
    log "错误: MySQL 凭证文件不存在: $MYSQL_CNF"
    exit 1
fi

# ========== 生成备份文件名 ==========
TIMESTAMP=$(date '+%Y%m%d_%H%M%S')
BACKUP_FILE="${BACKUP_DIR}/${DATABASE}_${TIMESTAMP}.sql.gz"

log "开始备份数据库 $DATABASE ..."

# ========== 执行备份 ==========
if mysqldump \
    --defaults-extra-file="$MYSQL_CNF" \
    --single-transaction \
    --routines \
    --triggers \
    --events \
    --set-gtid-purged=OFF \
    --databases "$DATABASE" 2>> "$LOG_FILE" \
    | gzip > "$BACKUP_FILE"; then

    BACKUP_SIZE=$(ls -lh "$BACKUP_FILE" | awk '{print $5}')
    log "备份成功: $BACKUP_FILE ($BACKUP_SIZE)"
else
    log "错误: 备份失败！请检查 MySQL 连接和磁盘空间"
    exit 1
fi

# ========== 清理过期备份 ==========
DELETED_COUNT=$(find "$BACKUP_DIR" -name "${DATABASE}_*.sql.gz" -mtime +$RETENTION_DAYS -delete -print | wc -l)

if [ "$DELETED_COUNT" -gt 0 ]; then
    log "清理完成: 删除了 ${DELETED_COUNT} 个过期备份（>${RETENTION_DAYS}天）"
fi

# ========== 显示当前备份占用 ==========
TOTAL_SIZE=$(du -sh "$BACKUP_DIR" | awk '{print $1}')
BACKUP_COUNT=$(find "$BACKUP_DIR" -name "${DATABASE}_*.sql.gz" | wc -l)
log "备份目录: $BACKUP_COUNT 个文件, 共 $TOTAL_SIZE"

exit 0
