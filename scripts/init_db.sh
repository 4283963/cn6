#!/bin/bash
# PostgreSQL 初始化脚本 - 创建数据库和用户

DB_NAME="flight_sensor"
DB_USER="postgres"
DB_PASS="postgres"

echo "正在创建 PostgreSQL 数据库..."

export PGPASSWORD=$DB_PASS

psql -U $DB_USER -h localhost -c "CREATE DATABASE $DB_NAME;" 2>/dev/null || echo "数据库 $DB_NAME 已存在或创建失败，请手动检查"

echo "检查/创建数据库完成。"
echo "请确保 PostgreSQL 服务已启动，用户 postgres 密码为 postgres"
echo ""
echo "如需手动创建，请执行："
echo "  createdb -U postgres flight_sensor"
echo "  或在 psql 中执行: CREATE DATABASE flight_sensor;"
