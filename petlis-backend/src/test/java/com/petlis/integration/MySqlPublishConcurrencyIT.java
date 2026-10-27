package com.petlis.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.petlis.common.security.*;
import com.petlis.service.impl.*;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assumptions.assumeTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class MySqlPublishConcurrencyIT {
    @Test
    void mysql84FirstConcurrentPublishCompletesWholeOrderWithoutRetry() throws Exception {
        String url = System.getProperty("PETLIS_MYSQL_TEST_URL");
        assumeTrue(url != null && !url.isBlank(), "仅隔离 MySQL 专项运行");
        DriverManagerDataSource dataSource = new DriverManagerDataSource(url,
                System.getProperty("PETLIS_MYSQL_TEST_USER", "root"), System.getProperty("PETLIS_MYSQL_TEST_PASSWORD", "integration-only"));
        JdbcTemplate jdbc = new JdbcTemplate(dataSource);
        assertEquals("8.4", jdbc.queryForObject("SELECT LEFT(VERSION(),3)", String.class));
        schema(jdbc);
        FeatureStore store = new FeatureStore(jdbc, new ObjectMapper());
        AccessService access = mock(AccessService.class);
        when(access.current()).thenReturn(new CurrentActor(5, Set.of(PlatformRole.ADMIN), Set.of(), Set.of(), new DataScope(true, Set.of(), Set.of(), null)));
        BusinessNotifications notifications = mock(BusinessNotifications.class);
        ReportWorkflowService first = service(store, access, notifications);
        ReportWorkflowService second = service(store, access, notifications);
        TransactionTemplate transactions = new TransactionTemplate(new DataSourceTransactionManager(dataSource));
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService pool = Executors.newFixedThreadPool(2);
        try {
            Future<?> one = pool.submit(() -> { await(start); transactions.executeWithoutResult(status -> first.publish(41)); });
            Future<?> two = pool.submit(() -> { await(start); transactions.executeWithoutResult(status -> second.publish(42)); });
            start.countDown();
            one.get(20, TimeUnit.SECONDS);
            two.get(20, TimeUnit.SECONDS);
        } finally {
            pool.shutdownNow();
        }
        assertEquals(3, jdbc.queryForObject("SELECT SampleStatus FROM lin_lb_listdiagnosisproject WHERE ListDiagnosisProjectId=8", Integer.class));
        assertEquals("COMPLETED", jdbc.queryForObject("SELECT status FROM lin_lab_task WHERE id=11", String.class));
        assertEquals(2, jdbc.queryForObject("SELECT COUNT(*) FROM lin_report_workflow WHERE status='PUBLISHED'", Integer.class));
    }

    private void await(CountDownLatch start) {
        try { start.await(); }
        catch (InterruptedException error) { Thread.currentThread().interrupt(); throw new IllegalStateException(error); }
    }

    private ReportWorkflowService service(FeatureStore store, AccessService access, BusinessNotifications notifications) {
        var service = new ReportWorkflowService(store, access, mock(LabWorkflowService.class), notifications, mock(ReportPdfGenerator.class), mock(PlatformFileService.class));
        ReflectionTestUtils.setField(service, "serverDomain", "https://laboklin.cn");
        return service;
    }

    private void schema(JdbcTemplate jdbc) {
        jdbc.execute("CREATE TABLE lin_lb_listdiagnosisproject(ListDiagnosisProjectId INT PRIMARY KEY,CreateSampleId INT,IsPayMoney INT,SampleStatus INT,SendReportDt DATETIME) ENGINE=InnoDB");
        jdbc.execute("CREATE TABLE lin_lb_infohospital(InfoHospitalId INT PRIMARY KEY,PayType INT) ENGINE=InnoDB");
        jdbc.execute("CREATE TABLE lin_order_extension(order_id INT PRIMARY KEY,settlement_mode VARCHAR(20)) ENGINE=InnoDB");
        jdbc.execute("CREATE TABLE lin_lab_task(id BIGINT PRIMARY KEY,status VARCHAR(32),completed_at DATETIME,version INT) ENGINE=InnoDB");
        jdbc.execute("CREATE TABLE lin_test_result(id BIGINT PRIMARY KEY,lab_task_id BIGINT,batch_no VARCHAR(32),result_status VARCHAR(32)) ENGINE=InnoDB");
        jdbc.execute("CREATE TABLE lin_file(id BIGINT PRIMARY KEY,storage_path VARCHAR(255)) ENGINE=InnoDB");
        jdbc.execute("CREATE TABLE lin_report_workflow(id BIGINT PRIMARY KEY,order_id INT,detail_id INT,test_result_id BIGINT,hospital_id INT,laboratory_id INT,status VARCHAR(32),pdf_file_id BIGINT,payment_checked INT,published_by INT,published_at DATETIME,legacy_synced_at DATETIME,version INT) ENGINE=InnoDB");
        jdbc.execute("CREATE TABLE lin_lb_diagnosisdetail(DiagnosisDetailId INT PRIMARY KEY,ListDiagnosisProjectId INT,IsCheckPass INT,IsActive INT,DiagnosisStatus INT,IsSendMail INT,PDFHttpUrl VARCHAR(500),SendMailDt DATETIME) ENGINE=InnoDB");
        jdbc.execute("CREATE TABLE lin_lb_diagnosisdetail_info(diagnosisdetail_id INT PRIMARY KEY,ReportResult LONGTEXT,MailReport LONGTEXT) ENGINE=InnoDB");
        jdbc.execute("CREATE TABLE lin_audit_record(id BIGINT AUTO_INCREMENT PRIMARY KEY,resource_type VARCHAR(64),resource_id VARCHAR(64),action VARCHAR(64),after_json LONGTEXT) ENGINE=InnoDB");
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
