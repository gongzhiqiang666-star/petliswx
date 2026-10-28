package com.petlis.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.petlis.common.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
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
        Long id = jdbc.execute((org.springframework.jdbc.core.ConnectionCallback<Long>) connection -> {
            try (var statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                for (int i = 0; i < args.length; i++) statement.setObject(i + 1, args[i]);
                statement.executeUpdate();
                try (var keys = statement.getGeneratedKeys()) {
                    if (keys != null && keys.next()) return keys.getLong(1);
                }
                try (var query = connection.createStatement(); var result = query.executeQuery("SELECT LAST_INSERT_ID()")) {
                    if (result.next()) {
                        long value = result.getLong(1);
                        if (!result.wasNull() && value > 0) return value;
                    }
                }
            }
            throw new BusinessException("记录保存失败，未获取到生成主键");
        });
        if (id == null) throw new BusinessException("记录保存失败，未获取到生成主键");
        return id;
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
