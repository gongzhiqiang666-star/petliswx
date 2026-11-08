package com.petlis.service.impl;
import com.petlis.common.exception.BusinessException;
import com.petlis.common.security.*;
import org.junit.jupiter.api.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionSynchronization;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;
class AdminStaffCreationTest {
    FeatureStore store=mock(FeatureStore.class);JdbcTemplate jdbc=mock(JdbcTemplate.class);AccessService access=mock(AccessService.class);UserRoleService roles=mock(UserRoleService.class);
    AdminService service=new AdminService(store,roles,mock(SessionService.class),mock(ReviewService.class),access);
    AdminService.StaffCreate valid(){return new AdminService.StaffCreate("local_lab_user","联调实验人员","LocalStaff123!","LAB_STAFF",1,"本机联调");}
    @BeforeEach void setup(){when(store.jdbc()).thenReturn(jdbc);when(jdbc.queryForObject(contains("GET_LOCK"),eq(Integer.class),anyString())).thenReturn(1);when(store.insert(contains("INSERT INTO lin_user"),any(Object[].class))).thenReturn(99L);TransactionSynchronizationManager.initSynchronization();SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(new UserPrincipal(42,null,null,null),null,List.of()));}
    @AfterEach void clear(){for(var sync:TransactionSynchronizationManager.getSynchronizations())sync.afterCompletion(TransactionSynchronization.STATUS_ROLLED_BACK);TransactionSynchronizationManager.clearSynchronization();SecurityContextHolder.clearContext();}
    @Test void dedicatedStaffHasHashedCredentialNoHospitalAndSingleRole() {
        assertEquals(99,service.createStaff(valid()));
        var hash=org.mockito.ArgumentCaptor.forClass(String.class);
        verify(jdbc).update(contains("INSERT INTO lin_doctor_profile"),eq(99L),hash.capture(),eq("管理员创建员工：本机联调"),eq(42));
        assertTrue(new org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder().matches("LocalStaff123!",hash.getValue()));
        verify(store).insert(contains("hospital_id"),eq("local_lab_user"),eq("联调实验人员"));
        verify(roles).assign(99,"LAB_STAFF",null,1,42);
        verify(jdbc).update(contains("lin_audit_record"),eq(42),eq("STAFF_CREATED"),eq("USER"),eq("99"),eq("本机联调"));
    }
    @Test void invalidInputsAdminEscalationAndCourierLabScopeNeverCreateUser() {
        assertThrows(BusinessException.class,()->service.createStaff(null));
        assertThrows(BusinessException.class,()->service.createStaff(new AdminService.StaffCreate("local_user","姓名","Local123!","ADMIN",null,"原因")));
        assertThrows(BusinessException.class,()->service.createStaff(new AdminService.StaffCreate("local_user","姓名","Local123!","LAB_STAFF",null,"原因")));
        assertThrows(BusinessException.class,()->service.createStaff(new AdminService.StaffCreate("local_user","姓名","Local123!","COURIER",1,"原因")));
        assertThrows(BusinessException.class,()->service.createStaff(new AdminService.StaffCreate("local_user","姓名","短1a","COURIER",null,"原因")));
        verify(store,never()).insert(anyString(),any(Object[].class));verifyNoInteractions(roles);
    }
    @Test void duplicateUsernameAndUnauthorizedRequestsCannotCreateAccounts() {
        when(jdbc.queryForObject(contains("EXISTS(SELECT 1 FROM lin_user"),eq(Boolean.class),anyString(),anyString())).thenReturn(true);
        assertThrows(BusinessException.class,()->service.createStaff(valid()));
        verify(store,never()).insert(anyString(),any(Object[].class));
        doThrow(new BusinessException(403,"无权限")).when(access).requireAdmin();
        assertThrows(BusinessException.class,()->service.createStaff(valid()));
        verifyNoInteractions(roles);
    }
}
