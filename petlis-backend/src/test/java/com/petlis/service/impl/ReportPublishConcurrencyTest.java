package com.petlis.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.petlis.common.security.*;
import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

class ReportPublishConcurrencyTest {
    @Test
    void concurrentLastDetailPublishesSerializeOnSharedOrderAndTask() throws Exception {
        JdbcDataSource dataSource = new JdbcDataSource();
        dataSource.setURL("jdbc:h2:mem:publish_concurrency;MODE=MySQL;DATABASE_TO_UPPER=false;DB_CLOSE_DELAY=-1;LOCK_MODE=0");
        JdbcTemplate jdbc = new JdbcTemplate(dataSource);
        schema(jdbc);
        FeatureStore delegate = new FeatureStore(jdbc, new ObjectMapper());
        CyclicBarrier completionBarrier = new CyclicBarrier(2);
        AtomicBoolean sharedOrderLocked = new AtomicBoolean();
        FeatureStore store = new FeatureStore(jdbc, new ObjectMapper()) {
            @Override public Map<String, Object> one(String sql, Object... args) {
                if (sql.startsWith("SELECT ListDiagnosisProjectId") && sql.contains("FOR UPDATE")) sharedOrderLocked.set(true);
                if (sql.contains("COUNT(DISTINCT") && !sharedOrderLocked.get()) {
                    try { completionBarrier.await(5, TimeUnit.SECONDS); }
                    catch (Exception error) { throw new IllegalStateException(error); }
                }
                String effective = sql.startsWith("SELECT w.*") && sql.endsWith("FOR UPDATE") ? sql.substring(0, sql.length() - " FOR UPDATE".length()) : sql;
                return delegate.one(effective, args);
            }
        };
        AccessService access = mock(AccessService.class);
        when(access.current()).thenReturn(new CurrentActor(5, Set.of(PlatformRole.ADMIN), Set.of(), Set.of(), new DataScope(true, Set.of(), Set.of(), null)));
        BusinessNotifications notifications = mock(BusinessNotifications.class);
        ReportWorkflowService first = new ReportWorkflowService(store, access, mock(LabWorkflowService.class), notifications, mock(ReportPdfGenerator.class), mock(PlatformFileService.class));
        ReportWorkflowService second = new ReportWorkflowService(store, access, mock(LabWorkflowService.class), notifications, mock(ReportPdfGenerator.class), mock(PlatformFileService.class));
        ReflectionTestUtils.setField(first, "serverDomain", "https://laboklin.cn");
        ReflectionTestUtils.setField(second, "serverDomain", "https://laboklin.cn");
        TransactionTemplate transactions = new TransactionTemplate(new DataSourceTransactionManager(dataSource));
        ExecutorService pool = Executors.newFixedThreadPool(2);
        try {
            Future<?> one = pool.submit(() -> transactions.executeWithoutResult(status -> first.publish(41)));
            Future<?> two = pool.submit(() -> transactions.executeWithoutResult(status -> second.publish(42)));
            two.get(15, TimeUnit.SECONDS);
            one.get(15, TimeUnit.SECONDS);
        } finally {
            pool.shutdownNow();
        }
        assertEquals(3, jdbc.queryForObject("SELECT SampleStatus FROM lin_lb_listdiagnosisproject WHERE ListDiagnosisProjectId=8", Integer.class));
        assertEquals("COMPLETED", jdbc.queryForObject("SELECT status FROM lin_lab_task WHERE id=11", String.class));
    }

    private void schema(JdbcTemplate jdbc) {
        jdbc.execute("CREATE TABLE lin_lb_listdiagnosisproject(ListDiagnosisProjectId INT PRIMARY KEY,CreateSampleId INT,IsPayMoney INT,SampleStatus INT,SendReportDt TIMESTAMP)");
        jdbc.execute("CREATE TABLE lin_lb_infohospital(InfoHospitalId INT PRIMARY KEY,PayType INT)");
        jdbc.execute("CREATE TABLE lin_order_extension(order_id INT PRIMARY KEY,settlement_mode VARCHAR(20))");
        jdbc.execute("CREATE TABLE lin_lab_task(id BIGINT PRIMARY KEY,status VARCHAR(32),completed_at TIMESTAMP,version INT)");
        jdbc.execute("CREATE TABLE lin_test_result(id BIGINT PRIMARY KEY,lab_task_id BIGINT,batch_no VARCHAR(32),result_status VARCHAR(32))");
        jdbc.execute("CREATE TABLE lin_file(id BIGINT PRIMARY KEY,storage_path VARCHAR(255))");
        jdbc.execute("CREATE TABLE lin_report_workflow(id BIGINT PRIMARY KEY,order_id INT,detail_id INT,test_result_id BIGINT,hospital_id INT,laboratory_id INT,status VARCHAR(32),pdf_file_id BIGINT,payment_checked INT,published_by INT,published_at TIMESTAMP,legacy_synced_at TIMESTAMP,version INT)");
        jdbc.execute("CREATE TABLE lin_lb_diagnosisdetail(DiagnosisDetailId INT PRIMARY KEY,ListDiagnosisProjectId INT,IsCheckPass INT,IsActive INT,DiagnosisStatus INT,IsSendMail INT,PDFHttpUrl VARCHAR(500),SendMailDt TIMESTAMP)");
        jdbc.execute("CREATE TABLE lin_lb_diagnosisdetail_info(diagnosisdetail_id INT PRIMARY KEY,ReportResult CLOB,MailReport CLOB)");
        jdbc.execute("CREATE TABLE lin_audit_record(id BIGINT AUTO_INCREMENT PRIMARY KEY,resource_type VARCHAR(64),resource_id VARCHAR(64),action VARCHAR(64),after_json CLOB)");
        jdbc.update("INSERT INTO lin_lb_listdiagnosisproject VALUES(8,7,0,2,NULL)");
        jdbc.update("INSERT INTO lin_lb_infohospital VALUES(1,0)");
        jdbc.update("INSERT INTO lin_order_extension VALUES(8,'monthly')");
        jdbc.update("INSERT INTO lin_lab_task VALUES(11,'REPORT_REVIEW',NULL,0)");
        jdbc.update("INSERT INTO lin_test_result VALUES(31,11,'B-1','ENTERED')");
        jdbc.update("INSERT INTO lin_file VALUES(51,'report.pdf')");
        jdbc.update("INSERT INTO lin_lb_diagnosisdetail VALUES(21,8,0,1,0,0,'',NULL),(22,8,0,1,0,0,'',NULL)");
        jdbc.update("INSERT INTO lin_report_workflow VALUES(41,8,21,31,1,2,'APPROVED',51,0,NULL,NULL,NULL,0),(42,8,22,31,1,2,'APPROVED',51,0,NULL,NULL,NULL,0)");
    }
}
