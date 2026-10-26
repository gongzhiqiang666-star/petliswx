package com.petlis.service.impl;

import com.petlis.common.exception.BusinessException;
import com.petlis.common.security.ReviewPermission;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;

@Service
@RequiredArgsConstructor
public class LaboratoryService {
    private final FeatureStore store;
    private final ReviewPermission permission;
    public List<Map<String,Object>> list(){return store.jdbc().queryForList("SELECT id,code,name,description FROM lin_laboratory WHERE active=1 ORDER BY sort_order,id");}
    public Map<String,Object> tests(int laboratoryId,String search,int page,int size){
        store.one("SELECT id FROM lin_laboratory WHERE id=? AND active=1",laboratoryId);
        String from=" FROM lin_lab_test lt JOIN lin_lb_infotesttype t ON t.InfoTestTypeId=lt.test_type_id WHERE lt.laboratory_id=? AND lt.active=1 AND t.IsActive=1 AND (t.TestName LIKE ? OR t.TestNO LIKE ?)";
        String keyword="%"+(search==null?"":search.replace("%","\\%").replace("_","\\_"))+"%";
        return store.page("SELECT COUNT(*)"+from,"SELECT t.InfoTestTypeId AS infoTestTypeId,t.TestName AS testName,t.TestNO AS testNo,COALESCE(lt.price,(SELECT p.price FROM lin_lb_infotesttype_price p WHERE p.InfoTestTypeid=t.InfoTestTypeId AND p.IsActive=1 ORDER BY p.IID DESC LIMIT 1),t.Price) AS price"+from+" ORDER BY t.TestNO,t.InfoTestTypeId",page,size,laboratoryId,keyword,keyword);
    }
    public record Laboratory(String code,String name,String description){}
    @Transactional public long create(Laboratory r){
        permission.requireReviewer();
        if(r.code()==null || !r.code().matches("[A-Z0-9_-]{2,50}") || r.name()==null || r.name().isBlank() || r.name().length()>200)throw new BusinessException("实验室编号和名称无效");
        return store.insert("INSERT INTO lin_laboratory(code,name,description) VALUES(?,?,?)",r.code(),r.name(),r.description()==null?"":r.description());
    }
    @Transactional public void assignTest(int laboratoryId,int testTypeId,java.math.BigDecimal price){
        permission.requireReviewer();
        store.one("SELECT id FROM lin_laboratory WHERE id=?",laboratoryId);
        store.one("SELECT InfoTestTypeId FROM lin_lb_infotesttype WHERE InfoTestTypeId=? AND IsActive=1",testTypeId);
        if(price!=null && (price.signum()<0 || price.scale()>2))throw new BusinessException("价格无效");
        store.jdbc().update("INSERT INTO lin_lab_test(laboratory_id,test_type_id,price) VALUES(?,?,?) ON DUPLICATE KEY UPDATE price=VALUES(price),active=1",laboratoryId,testTypeId,price);
    }
}
