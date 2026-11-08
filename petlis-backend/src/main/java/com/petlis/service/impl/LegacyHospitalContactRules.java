package com.petlis.service.impl;
import com.petlis.common.exception.BusinessException;
/** PHP basedata/lb_infohospitalcontact add/edit: required Mail, original varchar(250), finance flag.
 * LoginName/LoginPwd are historical storage fields, not ordinary contact-maintenance inputs. */
final class LegacyHospitalContactRules {
 record Contact(String name,String tel,String mail,String fax,String remark,int financeContact){}
 static Contact validate(Contact c){
  if(c==null)throw new BusinessException(400,"联系人内容缺失");
  if(c.mail()==null||c.mail().isBlank())throw new BusinessException(400,"邮箱不能为空");
  if(c.financeContact()!=0&&c.financeContact()!=1)throw new BusinessException(400,"财务专用标志无效");
  for(String value:new String[]{c.name(),c.tel(),c.mail(),c.fax(),c.remark()})if(value!=null&&value.codePointCount(0,value.length())>250)throw new BusinessException(400,"联系人字段不能超过250个字符");
  return new Contact(c.name()==null?"":c.name(),c.tel()==null?"":c.tel(),c.mail(),c.fax()==null?"":c.fax(),c.remark(),c.financeContact());
 }
}
