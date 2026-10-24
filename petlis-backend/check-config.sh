#!/bin/bash
# PETLIS Backend - 配置检查脚本
# 用于验证通知系统配置是否正确

echo "======================================"
echo "PETLIS 通知系统配置检查"
echo "======================================"
echo ""

# 颜色定义
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# 检查计数
PASS=0
FAIL=0
WARN=0

# 检查函数
check_pass() {
    echo -e "${GREEN}✓${NC} $1"
    ((PASS++))
}

check_fail() {
    echo -e "${RED}✗${NC} $1"
    ((FAIL++))
}

check_warn() {
    echo -e "${YELLOW}⚠${NC} $1"
    ((WARN++))
}

# 1. 检查环境变量文件
echo "1. 检查环境变量配置"
echo "--------------------------------------"
if [ -f .env ]; then
    check_pass ".env 文件存在"

    source .env

    # 检查必需的环境变量
    [ -n "$DB_HOST" ] && check_pass "DB_HOST 已配置" || check_fail "DB_HOST 未配置"
    [ -n "$DB_NAME" ] && check_pass "DB_NAME 已配置" || check_fail "DB_NAME 未配置"
    [ -n "$DB_USER" ] && check_pass "DB_USER 已配置" || check_fail "DB_USER 未配置"
    [ -n "$DB_PASSWORD" ] && check_pass "DB_PASSWORD 已配置" || check_fail "DB_PASSWORD 未配置"
    [ -n "$WECHAT_APPID" ] && check_pass "WECHAT_APPID 已配置" || check_fail "WECHAT_APPID 未配置"
    [ -n "$WECHAT_SECRET" ] && check_pass "WECHAT_SECRET 已配置" || check_fail "WECHAT_SECRET 未配置"
else
    check_fail ".env 文件不存在，请复制 .env.example 并填写配置"
fi
echo ""

# 2. 检查配置文件
echo "2. 检查应用配置文件"
echo "--------------------------------------"
if [ -f "src/main/resources/application.yml" ]; then
    check_pass "application.yml 存在"

    # 检查Flyway配置
    if grep -q "flyway:" src/main/resources/application.yml; then
        check_pass "Flyway 配置存在"
        if grep -q "enabled: true" src/main/resources/application.yml; then
            check_pass "Flyway 已启用"
        else
            check_fail "Flyway 未启用"
        fi
    else
        check_fail "Flyway 配置缺失"
    fi
else
    check_fail "application.yml 不存在"
fi
echo ""

# 3. 检查Migration脚本
echo "3. 检查数据库Migration脚本"
echo "--------------------------------------"
MIGRATION_DIR="src/main/resources/db/migration"
if [ -d "$MIGRATION_DIR" ]; then
    check_pass "Migration 目录存在"

    # 检查所有需要的migration文件
    MIGRATIONS=(
        "V1__create_notification_table.sql"
        "V2__create_order_status_trigger.sql"
        "V3__add_notification_details.sql"
        "V4__create_notification_log_table.sql"
        "V5__add_order_subscribe_field.sql"
        "V6__update_order_status_trigger.sql"
    )

    for migration in "${MIGRATIONS[@]}"; do
        if [ -f "$MIGRATION_DIR/$migration" ]; then
            check_pass "$migration 存在"
        else
            check_fail "$migration 缺失"
        fi
    done
else
    check_fail "Migration 目录不存在"
fi
echo ""

# 4. 检查pom.xml依赖
echo "4. 检查Maven依赖"
echo "--------------------------------------"
if [ -f "pom.xml" ]; then
    check_pass "pom.xml 存在"

    if grep -q "flyway-core" pom.xml; then
        check_pass "flyway-core 依赖已添加"
    else
        check_fail "flyway-core 依赖缺失"
    fi

    if grep -q "flyway-mysql" pom.xml; then
        check_pass "flyway-mysql 依赖已添加"
    else
        check_fail "flyway-mysql 依赖缺失"
    fi
else
    check_fail "pom.xml 不存在"
fi
echo ""

# 5. 检查Java代码
echo "5. 检查Java代码文件"
echo "--------------------------------------"
JAVA_FILES=(
    "src/main/java/com/petlis/entity/NotificationLog.java"
    "src/main/java/com/petlis/repository/NotificationLogRepository.java"
)

for file in "${JAVA_FILES[@]}"; do
    if [ -f "$file" ]; then
        check_pass "$(basename $file) 存在"
    else
        check_fail "$(basename $file) 缺失"
    fi
done
echo ""

# 6. 检查数据库连接（如果配置了）
echo "6. 检查数据库连接"
echo "--------------------------------------"
if [ -n "$DB_HOST" ] && [ -n "$DB_USER" ] && [ -n "$DB_PASSWORD" ]; then
    if command -v mysql &> /dev/null; then
        if mysql -h"$DB_HOST" -u"$DB_USER" -p"$DB_PASSWORD" -e "USE $DB_NAME;" 2>/dev/null; then
            check_pass "数据库连接成功"

            # 检查Flyway历史表
            FLYWAY_TABLE=$(mysql -h"$DB_HOST" -u"$DB_USER" -p"$DB_PASSWORD" -D"$DB_NAME" -N -s -e "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = '$DB_NAME' AND table_name = 'flyway_schema_history';" 2>/dev/null)
            if [ "$FLYWAY_TABLE" = "1" ]; then
                check_pass "flyway_schema_history 表存在"

                # 检查migration记录
                MIGRATION_COUNT=$(mysql -h"$DB_HOST" -u"$DB_USER" -p"$DB_PASSWORD" -D"$DB_NAME" -N -s -e "SELECT COUNT(*) FROM flyway_schema_history;" 2>/dev/null)
                if [ "$MIGRATION_COUNT" = "6" ]; then
                    check_pass "Flyway migration记录完整（6条）"
                else
                    check_warn "Flyway migration记录数量: $MIGRATION_COUNT (预期6条)"
                fi
            else
                check_warn "flyway_schema_history 表不存在（首次部署时正常）"
            fi

            # 检查通知表
            NOTIFICATION_TABLE=$(mysql -h"$DB_HOST" -u"$DB_USER" -p"$DB_PASSWORD" -D"$DB_NAME" -N -s -e "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = '$DB_NAME' AND table_name = 'lin_lb_notification';" 2>/dev/null)
            if [ "$NOTIFICATION_TABLE" = "1" ]; then
                check_pass "lin_lb_notification 表存在"
            else
                check_warn "lin_lb_notification 表不存在（需要运行migration）"
            fi

            # 检查通知日志表
            LOG_TABLE=$(mysql -h"$DB_HOST" -u"$DB_USER" -p"$DB_PASSWORD" -D"$DB_NAME" -N -s -e "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = '$DB_NAME' AND table_name = 'lin_lb_notification_log';" 2>/dev/null)
            if [ "$LOG_TABLE" = "1" ]; then
                check_pass "lin_lb_notification_log 表存在"
            else
                check_warn "lin_lb_notification_log 表不存在（需要运行migration）"
            fi

            # 检查触发器
            TRIGGER_COUNT=$(mysql -h"$DB_HOST" -u"$DB_USER" -p"$DB_PASSWORD" -D"$DB_NAME" -N -s -e "SELECT COUNT(*) FROM information_schema.TRIGGERS WHERE TRIGGER_SCHEMA = '$DB_NAME' AND TRIGGER_NAME = 'trg_order_status_change';" 2>/dev/null)
            if [ "$TRIGGER_COUNT" = "1" ]; then
                check_pass "trg_order_status_change 触发器存在"
            else
                check_warn "trg_order_status_change 触发器不存在（需要运行migration）"
            fi
        else
            check_fail "数据库连接失败，请检查配置"
        fi
    else
        check_warn "MySQL客户端未安装，跳过数据库检查"
    fi
else
    check_warn "数据库环境变量未配置，跳过数据库检查"
fi
echo ""

# 7. 总结
echo "======================================"
echo "检查结果汇总"
echo "======================================"
echo -e "${GREEN}通过: $PASS${NC}"
echo -e "${YELLOW}警告: $WARN${NC}"
echo -e "${RED}失败: $FAIL${NC}"
echo ""

if [ $FAIL -eq 0 ]; then
    echo -e "${GREEN}✓ 配置检查通过！可以启动应用。${NC}"
    exit 0
else
    echo -e "${RED}✗ 配置检查失败！请修复上述问题后再启动应用。${NC}"
    exit 1
fi
