package com.petlis.service.impl;

import com.petlis.common.exception.BusinessException;
import com.petlis.common.security.AccessService;
import com.petlis.common.security.CurrentActor;
import com.petlis.common.security.PlatformRole;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AdminCatalogService {
    private final FeatureStore store;
    private final AccessService access;
    public record ItemCommand(String name, String code, BigDecimal price) { }

    public Map<String, Object> laboratories(int page, int size) {
        CurrentActor actor = access.current();
        if (!actor.isAdmin() && !actor.hasRole(PlatformRole.LAB_STAFF)) throw new BusinessException(403, "无权查看实验室目录");
        return store.page("SELECT COUNT(*) FROM lin_laboratory", "SELECT id,name,code,description,active,sort_order AS sortOrder FROM lin_laboratory ORDER BY sort_order,id", page, size);
    }
    public Map<String, Object> items(int laboratoryId, int page, int size) {
        access.requireLaboratory(laboratoryId);
        return store.page("SELECT COUNT(*) FROM lin_lab_test WHERE laboratory_id=?", "SELECT lt.laboratory_id AS laboratoryId,lt.test_type_id AS id,t.TestName AS name,t.TestNO AS code,COALESCE(lt.price,t.Price) AS price,lt.active,lt.test_type_id AS sortOrder FROM lin_lab_test lt JOIN lin_lb_infotesttype t ON t.InfoTestTypeId=lt.test_type_id WHERE lt.laboratory_id=? ORDER BY lt.test_type_id", page, size, laboratoryId);
    }
    @Transactional public long createItem(int laboratoryId, ItemCommand command) {
        access.requireLaboratory(laboratoryId);
        validate(command);
        long id = store.insert("INSERT INTO lin_lb_infotesttype(TestName,TestNO,Price,CtoG,GtoC,DiscountReal,DiscountNext,TextModel,CreateDt,type,IsActive,createtime,updatetime) VALUES(?,?,?,0,0,1,1,'',NOW(),1,1,UNIX_TIMESTAMP(),UNIX_TIMESTAMP())", command.name().trim(), command.code().trim(), command.price());
        store.insert("INSERT INTO lin_lab_test(laboratory_id,test_type_id,price,active) VALUES(?,?,?,1)", laboratoryId, id, command.price());
        return id;
    }
    @Transactional public void updateItem(int laboratoryId, int testTypeId, ItemCommand command) {
        access.requireLaboratory(laboratoryId);
        validate(command);
        store.one("SELECT test_type_id FROM lin_lab_test WHERE laboratory_id=? AND test_type_id=?", laboratoryId, testTypeId);
        store.jdbc().update("UPDATE lin_lb_infotesttype SET TestName=?,TestNO=?,Price=?,updatetime=UNIX_TIMESTAMP() WHERE InfoTestTypeId=?", command.name().trim(), command.code().trim(), command.price(), testTypeId);
        store.jdbc().update("UPDATE lin_lab_test SET price=? WHERE laboratory_id=? AND test_type_id=?", command.price(), laboratoryId, testTypeId);
    }
    @Transactional public void deleteItem(int laboratoryId, int testTypeId) {
        access.requireLaboratory(laboratoryId);
        store.one("SELECT test_type_id FROM lin_lab_test WHERE laboratory_id=? AND test_type_id=?", laboratoryId, testTypeId);
        store.jdbc().update("UPDATE lin_lb_infotesttype SET IsActive=0,updatetime=UNIX_TIMESTAMP() WHERE InfoTestTypeId=?", testTypeId);
        store.jdbc().update("UPDATE lin_lab_test SET active=0 WHERE laboratory_id=? AND test_type_id=?", laboratoryId, testTypeId);
    }
    @Transactional public void setItemStatus(int laboratoryId, int testTypeId, boolean active) {
        access.requireLaboratory(laboratoryId);
        store.jdbc().update("UPDATE lin_lab_test SET active=? WHERE laboratory_id=? AND test_type_id=?", active ? 1 : 0, laboratoryId, testTypeId);
        store.jdbc().update("UPDATE lin_lb_infotesttype SET IsActive=?,updatetime=UNIX_TIMESTAMP() WHERE InfoTestTypeId=?", active ? 1 : 0, testTypeId);
    }
    private void validate(ItemCommand command) {
        if (command == null || command.name() == null || command.name().isBlank() || command.name().length() > 250
                || command.code() == null || command.code().isBlank() || command.code().length() > 250
                || command.price() == null || command.price().signum() < 0 || command.price().scale() > 2) throw new BusinessException("检测项目名称、编码和价格无效");
    }
}
