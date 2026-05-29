# AI 聊天平台 — MySQL 自动备份部署指南

> 备份工具：mysqldump + gzip  
> 调度：crontab  
> 保留：7 天

---

## 一、原理

```
crontab（每天凌晨 3:00）
    │
    ▼
mysql-backup.sh
    ├─ ① mysqldump → 导出 ai_chat 数据库
    ├─ ② gzip 压缩 → ai_chat_20260511_030001.sql.gz
    ├─ ③ 清理 >7 天的旧备份
    └─ ④ 写日志 → /var/log/ai-chat-backup.log
```

## 二、部署步骤

### 步骤 1：确保 mysqldump 可用

```bash
which mysqldump
# 如果没有：sudo apt install mysql-client -y  (Ubuntu)
#          sudo yum install mysql -y         (CentOS)
```

### 步骤 2：创建备份用户（使用已有用户或新建）

```sql
-- 使用已有应用用户（推荐，已有 select 权限）
-- 或创建专用只读用户：
CREATE USER 'backup'@'localhost' IDENTIFIED BY '强密码';
GRANT SELECT, LOCK TABLES, SHOW VIEW, EVENT, TRIGGER ON ai_chat.* TO 'backup'@'localhost';
FLUSH PRIVILEGES;
```

### 步骤 3：部署凭证文件

```bash
# 复制模板
sudo cp deploy/mysql-backup.cnf /etc/mysql/backup.cnf

# 填入真实密码
sudo nano /etc/mysql/backup.cnf
# 将 "替换为真实密码" 改为实际密码

# 设置权限（仅 root 可读，关键！）
sudo chmod 600 /etc/mysql/backup.cnf
sudo chown root:root /etc/mysql/backup.cnf
```

### 步骤 4：部署备份脚本

```bash
# 复制脚本
sudo cp deploy/mysql-backup.sh /opt/ai-chat/deploy/
sudo chmod +x /opt/ai-chat/deploy/mysql-backup.sh

# 创建备份目录
sudo mkdir -p /opt/ai-chat/backups
```

### 步骤 5：手动执行一次验证

```bash
# 先试运行一次
sudo /opt/ai-chat/deploy/mysql-backup.sh

# 检查结果
ls -lh /opt/ai-chat/backups/
cat /var/log/ai-chat-backup.log
```

### 步骤 6：配置定时任务

```bash
# 编辑 root 的 crontab（备份需要读 /etc/mysql/backup.cnf）
sudo crontab -e

# 添加以下行（每天凌晨 3:00 执行）
0 3 * * * /opt/ai-chat/deploy/mysql-backup.sh
```

**为什么用 root crontab？** 备份脚本需要读取 `/etc/mysql/backup.cnf`（权限 600，owner root）。

### 步骤 7：验证定时任务

```bash
# 查看 crontab 是否保存成功
sudo crontab -l

# 查看 cron 日志确认执行
grep -i cron /var/log/syslog | tail -5    # Ubuntu
grep -i cron /var/log/cron | tail -5      # CentOS
```

## 三、恢复数据库

```bash
# 1. 找到要恢复的备份文件
ls -lh /opt/ai-chat/backups/

# 2. 解压并导入
gunzip -c /opt/ai-chat/backups/ai_chat_20260511_030001.sql.gz | mysql -u root -p ai_chat

# 3. 验证
mysql -u root -p -e "SELECT COUNT(*) FROM ai_chat.user;"
```

## 四、脚本关键设计说明

### 密码安全

密码不写在脚本里，不写在命令行参数里（命令行参数会被 `ps aux` 看到）：

```bash
# ❌ 密码暴露在进程列表中
mysqldump -u root -p123456 ai_chat | gzip > backup.sql.gz

# ✅ 密码存在独立的凭证文件（权限 600）
mysqldump --defaults-extra-file=/etc/mysql/backup.cnf ai_chat | gzip > backup.sql.gz
```

### mysqldump 参数说明

| 参数 | 作用 |
|------|------|
| `--single-transaction` | 不锁表，一致性备份（InnoDB） |
| `--routines` | 备份存储过程和函数 |
| `--triggers` | 备份触发器 |
| `--events` | 备份定时事件 |
| `--set-gtid-purged=OFF` | 避免还原时的 GTID 冲突 |

### 过期清理逻辑

```bash
# 删除 7 天前的备份文件
find "$BACKUP_DIR" -name "ai_chat_*.sql.gz" -mtime +7 -delete
```

`-mtime +7` = 最后修改时间超过 7 天的文件。不是按文件名日期，是按文件系统时间戳，更可靠。

## 五、监控建议

```bash
# 每天检查备份是否成功
tail -3 /var/log/ai-chat-backup.log

# 检查磁盘空间
df -h /opt/ai-chat/backups

# 可选：添加到你的日常工作流
# 每周手动还原一次到测试库验证备份可用
```

## 六、文件清单

| 文件 | 说明 |
|------|------|
| [deploy/mysql-backup.sh](deploy/mysql-backup.sh) | 备份脚本 |
| [deploy/mysql-backup.cnf](deploy/mysql-backup.cnf) | MySQL 凭证模板（需填密码） |
| [deploy/MySQL备份部署指南.md](deploy/MySQL备份部署指南.md) | 本文档 |
