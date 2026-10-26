package com.petlis.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.petlis.common.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Component;
import java.sql.Statement;
import java.util.*;

@Component
@RequiredArgsConstructor
public class FeatureStore {
    private final JdbcTemplate jdbc;
    public JdbcTemplate jdbc() { return jdbc; }
    private final ObjectMapper mapper;
    public Map<String,Object> one(String sql,Object...args) {
        var rows=jdbc.queryForList(sql,args);
        if(rows.isEmpty())throw new BusinessException(404,"记录不存在或无权访问");
        return rows.get(0);
    }
    public long insert(String sql,Object...args) {
        var key=new GeneratedKeyHolder();
        jdbc.update(connection->{var statement=connection.prepareStatement(sql,Statement.RETURN_GENERATED_KEYS);for(int i=0;i<args.length;i++)statement.setObject(i+1,args[i]);return statement;},key);
        if(key.getKey()==null)throw new BusinessException("记录保存失败");
        return key.getKey().longValue();
    }
    public String encode(Object value) {
        try{return mapper.writeValueAsString(value);}catch(JsonProcessingException e){throw new BusinessException("资料格式无效");}
    }
    public Map<String,Object> page(String countSql,String listSql,int page,int size,Object...args) {
        page=Math.max(0,page);size=Math.max(1,Math.min(100,size));
        Long total=jdbc.queryForObject(countSql,Long.class,args);
        Object[] queryArgs=Arrays.copyOf(args,args.length+2);queryArgs[args.length]=size;queryArgs[args.length+1]=(long)page*size;
        return Map.of("items",jdbc.queryForList(listSql+" LIMIT ? OFFSET ?",queryArgs),"total",total==null?0:total,"page",page,"size",size);
    }
}
