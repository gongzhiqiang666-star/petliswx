package com.petlis.service.impl;

import com.petlis.common.security.AccessService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class AdminCatalogService {
    private final FeatureStore store;
    private final AccessService access;

    public Map<String, Object> laboratories(int page, int size) {
        access.requireAdmin();
        return store.page("SELECT COUNT(*) FROM lin_laboratory", "SELECT id,name,code,description,active,sort_order AS sortOrder FROM lin_laboratory ORDER BY sort_order,id", page, size);
    }
    public Map<String, Object> items(int laboratoryId, int page, int size) {
        access.requireAdmin();
        return store.page("SELECT COUNT(*) FROM lin_lab_test WHERE laboratory_id=?", "SELECT lt.laboratory_id AS laboratoryId,lt.test_type_id AS id,t.TestName AS name,t.TestNameEn AS nameEn,COALESCE(lt.price,t.Price) AS price,lt.active,lt.sort_order AS sortOrder FROM lin_lab_test lt JOIN lin_lb_infotesttype t ON t.InfoTestTypeId=lt.test_type_id WHERE lt.laboratory_id=? ORDER BY lt.sort_order,lt.test_type_id", page, size, laboratoryId);
    }
    @Transactional public void setItemStatus(int laboratoryId, int testTypeId, boolean active) {
        access.requireAdmin();
        store.jdbc().update("UPDATE lin_lab_test SET active=? WHERE laboratory_id=? AND test_type_id=?", active ? 1 : 0, laboratoryId, testTypeId);
    }
}
