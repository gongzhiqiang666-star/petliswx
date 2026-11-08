package com.petlis.integration.sf;
import org.springframework.jdbc.core.JdbcTemplate;
/** One strict policy for API visibility and the locked reservation boundary. */
public final class SfReshipPolicy {
 private final JdbcTemplate jdbc;
 public SfReshipPolicy(JdbcTemplate jdbc){this.jdbc=jdbc;}
 public record Settings(boolean enabled,int maxAttempts,boolean valid,String message){}
 public Settings load(boolean lock){
  var rows=jdbc.queryForList("SELECT JSON_TYPE(JSON_EXTRACT(config_value,'$.enabled')) AS enabledType,JSON_UNQUOTE(JSON_EXTRACT(config_value,'$.enabled')) AS enabled,JSON_TYPE(JSON_EXTRACT(config_value,'$.maxAttempts')) AS maxType,JSON_UNQUOTE(JSON_EXTRACT(config_value,'$.maxAttempts')) AS maxAttempts FROM lin_operational_config WHERE config_key='sf-reship' AND scope_type='GLOBAL' AND scope_id=0 AND status='ACTIVE'"+(lock?" FOR SHARE":""));
  if(rows.isEmpty())return new Settings(false,3,true,"再次寄件尚未开启");
  if(rows.size()!=1)return invalid();var row=rows.get(0);if(!"BOOLEAN".equals(row.get("enabledType")))return invalid();
  int max=3;if(row.get("maxType")!=null){try{if(!"INTEGER".equals(row.get("maxType")))return invalid();max=Integer.parseInt(String.valueOf(row.get("maxAttempts")));}catch(NumberFormatException e){return invalid();}}
  if(max<2 || max>10)return invalid();boolean enabled="true".equals(row.get("enabled"));return new Settings(enabled,max,true,enabled?"":"再次寄件尚未开启");
 }
 private Settings invalid(){return new Settings(false,3,false,"再次寄件参数无效，请联系管理员");}
}
