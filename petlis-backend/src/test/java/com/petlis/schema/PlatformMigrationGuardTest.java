package com.petlis.schema;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PlatformMigrationGuardTest {
    private static final Path BACKEND = Path.of(System.getProperty("user.dir"));
    private static final Path DDL = BACKEND.resolve("src/main/resources/db/platform/V1__create_platform_workflow_tables.sql");
    private static final Set<String> TABLES = new LinkedHashSet<>(List.of(
            "lin_user_role", "lin_shipment", "lin_lab_task", "lin_test_result",
            "lin_report_workflow", "lin_report_template", "lin_order_clinical",
            "lin_order_idempotency", "lin_contract", "lin_contract_item_price",
            "lin_monthly_bill", "lin_monthly_bill_item", "lin_file",
            "lin_audit_record", "lin_operational_config"));

    private String readDdl() throws IOException {
        assertTrue(Files.isRegularFile(DDL), "缺少独立平台工作流 DDL");
        return Files.readString(DDL);
    }

    @Test
    void platformDdlContainsOnlyTheApprovedAdditiveTables() throws IOException {
        var sql = readDdl();
        var matcher = Pattern.compile("(?i)CREATE\\s+TABLE\\s+IF\\s+NOT\\s+EXISTS\\s+`?([a-z0-9_]+)`?").matcher(sql);
        Set<String> actual = new LinkedHashSet<>();
        while (matcher.find()) actual.add(matcher.group(1));
        assertEquals(TABLES, actual);
        assertFalse(Pattern.compile("(?i)\\b(ALTER|DROP|TRUNCATE|RENAME|CREATE\\s+TRIGGER)\\b").matcher(sql).find());
        assertFalse(Pattern.compile("(?i)CREATE\\s+TABLE[^;]+\\b(users|test_orders)\\b").matcher(sql).find());
    }

    @Test
    void workflowColumnsCoverLaterModulesWithLegacyIntReferences() throws IOException {
        var sql = readDdl();
        for (String column : List.of("user_id", "order_id", "detail_id", "hospital_id", "laboratory_id")) {
            assertTrue(Pattern.compile("(?i)`" + column + "`\\s+INT\\b").matcher(sql).find(), column + " 必须匹配旧 INT ID");
        }
        for (String fragment : List.of("tracking_no", "expected_arrived_at", "assigned_to", "batch_no",
                "medical_history", "request_hash", "settlement_type", "discount_rate", "credit_amount",
                "reconciled_at", "settled_at", "storage_path", "before_json", "config_value")) {
            assertTrue(sql.contains("`" + fragment + "`"), "缺少后续模块字段 " + fragment);
        }
        assertTrue(sql.contains("`storage_path` VARCHAR(768)"), "utf8mb4 唯一索引路径不得超过 768 字符");
    }

    @Test
    void platformDdlIsNotAutomaticallyExecuted() throws IOException {
        var config = Files.readString(BACKEND.resolve("src/main/resources/application.yml"));
        assertTrue(config.contains("enabled: ${DB_MIGRATIONS_ENABLED:false}"));
        assertTrue(config.contains("locations: classpath:db/migration"));
        assertFalse(config.contains("locations: classpath:db/migration,classpath:db/platform"));
    }

    @Test
    void excelMatrixHasEveryFeatureAndRequiredOperationalRows() throws IOException {
        var path = BACKEND.getParent().resolve("docs/FUNCTION-MATRIX.md");
        assertTrue(Files.isRegularFile(path), "缺少 Excel 功能矩阵");
        var matrix = Files.readString(path);
        var rows = Pattern.compile("(?m)^\\| F\\d{2} \\|").matcher(matrix).results().count();
        assertEquals(42, rows, "Excel 的 42 个功能主项必须逐项登记");
        for (String feature : List.of("标本揽收", "配送追踪", "实验室签收确认", "检测任务分配", "退样/复检处理",
                "医院入驻签约", "协议折扣", "欠款管理", "打印功能", "退款流程", "角色权限")) {
            assertTrue(matrix.contains(feature), "功能矩阵遗漏 " + feature);
        }
        for (String column : List.of("处理决策", "前端位置", "后端位置", "验收标准")) {
            assertTrue(matrix.contains(column), "功能矩阵缺少列 " + column);
        }
    }

    @Test
    void acceptanceTrackingUsesActualTask6InsteadOfMissingTask7() throws IOException {
        var root = BACKEND.getParent();
        var matrix = Files.readString(root.resolve("docs/FUNCTION-MATRIX.md"));
        var plan = Files.readString(root.resolve("docs/superpowers/plans/2026-10-27-confirmed-miniprogram-merge.md"));
        var reviewFocus = plan.split("## Review Focus", 2)[1].split("---", 2)[0];
        assertFalse(Pattern.compile("Task[s]?[^|\\n]*\\b7\\b").matcher(matrix).find(), "功能矩阵验收必须追踪到 Task 6");
        assertFalse(Pattern.compile("Task[s]?[^\\n]*\\b7\\b").matcher(reviewFocus).find(), "Review Focus 必须追踪到 Task 6");
    }

    @Test
    void resultBatchesAndTemplatesPreserveVersionedHistory() throws IOException {
        var sql = readDdl();
        assertTrue(sql.contains("UNIQUE KEY `uk_result_task_batch` (`lab_task_id`, `batch_no`)"));
        assertTrue(sql.contains("`result_status` VARCHAR(32) NOT NULL"));
        assertTrue(sql.contains("UNIQUE KEY `uk_report_template_version` (`laboratory_id`, `template_code`, `version`)"));
    }

    @Test
    void reportWorkflowContainsLegacyPublicationSynchronizationMetadata() throws IOException {
        var sql = readDdl();
        for (String column : List.of("pdf_file_id", "payment_checked", "published_at", "legacy_synced_at", "template_version")) {
            assertTrue(sql.contains("`" + column + "`"), column);
        }
    }

    @Test
    void fileTableContainsMetadataButNoSecondBinaryMaster() throws IOException {
        var sql = readDdl();
        String fileTable = sql.split("CREATE TABLE IF NOT EXISTS `lin_file`", 2)[1].split(";", 2)[0];
        for (String column : List.of("storage_path", "mime_type", "file_size", "sha256", "visibility")) assertTrue(fileTable.contains("`" + column + "`"));
        assertFalse(Pattern.compile("(?i)\\b(BLOB|LONGBLOB|MEDIUMBLOB)\\b").matcher(fileTable).find());
    }

    @Test
    void platformDdlDoesNotDuplicateOrderOrNotificationMasters() throws IOException {
        var sql = readDdl();
        assertFalse(Pattern.compile("(?i)CREATE\\s+TABLE[^;]+`(lin_notification_event|lin_order|lin_lb_listdiagnosisproject|lin_lb_diagnosisdetail)`").matcher(sql).find());
    }
}
