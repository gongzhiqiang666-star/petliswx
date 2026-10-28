package com.petlis.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.petlis.common.security.AccessService;
import com.petlis.common.security.CurrentActor;
import com.petlis.common.security.DataScope;
import com.petlis.common.security.PlatformRole;
import com.petlis.service.impl.BusinessNotifications;
import com.petlis.service.impl.FeatureStore;
import com.petlis.service.impl.LabWorkflowService;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class MySqlRealFlowCompatibilityIT {
    @Test
    void insertReturnsGeneratedKeyFromSameConnectionWithoutDuplicateRows() {
        JdbcTemplate jdbc = jdbc();
        assumeTrue(jdbc != null, "仅真实 MySQL 副本专项运行");
        FeatureStore store = new FeatureStore(jdbc, new ObjectMapper());
        TransactionTemplate transaction = new TransactionTemplate(new DataSourceTransactionManager(
                (DriverManagerDataSource) jdbc.getDataSource()));
        transaction.executeWithoutResult(status -> {
            long before = count(jdbc);
            long id = store.insert("INSERT INTO lin_lb_infotesttype(TestName,TestNO,Price,CtoG,GtoC,DiscountReal,DiscountNext,TextModel,CreateDt,type,IsActive,createtime,updatetime) VALUES(?,?,?,0,0,1,1,'',NOW(),1,1,UNIX_TIMESTAMP(),UNIX_TIMESTAMP())",
                    "QA真实主键回读", "QA_REAL_FLOW_KEY", 1);
            assertTrue(id > 0);
            assertEquals(id, jdbc.queryForObject("SELECT InfoTestTypeId FROM lin_lb_infotesttype WHERE TestNO='QA_REAL_FLOW_KEY'", Long.class));
            assertEquals(before + 1, count(jdbc));
            status.setRollbackOnly();
        });
    }

    @Test
    void assigneesQueryRunsAgainstRealLegacyUserSchema() {
        JdbcTemplate jdbc = jdbc();
        assumeTrue(jdbc != null, "仅真实 MySQL 副本专项运行");
        FeatureStore store = new FeatureStore(jdbc, new ObjectMapper());
        AccessService access = mock(AccessService.class);
        when(access.current()).thenReturn(new CurrentActor(1, Set.of(PlatformRole.ADMIN), Set.of(), Set.of(),
                new DataScope(true, Set.of(), Set.of(), null)));
        LabWorkflowService service = new LabWorkflowService(store, access, mock(BusinessNotifications.class));
        assertDoesNotThrow(() -> service.assignees(1));
    }

    private long count(JdbcTemplate jdbc) {
        Long value = jdbc.queryForObject("SELECT COUNT(*) FROM lin_lb_infotesttype WHERE TestNO='QA_REAL_FLOW_KEY'", Long.class);
        return value == null ? 0 : value;
    }

    private JdbcTemplate jdbc() {
        String url = System.getenv("DB_URL");
        assumeTrue(url != null && !url.isBlank(), "仅真实 MySQL 副本专项运行");
        DriverManagerDataSource dataSource = new DriverManagerDataSource(url,
                System.getenv("DB_USERNAME"), System.getenv("DB_PASSWORD"));
        return new JdbcTemplate(dataSource);
    }
}
