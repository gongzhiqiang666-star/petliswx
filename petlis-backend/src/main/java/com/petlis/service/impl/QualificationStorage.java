package com.petlis.service.impl;
import com.petlis.common.exception.BusinessException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import java.nio.file.*;
import java.util.*;
@Component
public class QualificationStorage {
    @Value("${app.file.upload-path}") private String uploadPath;
    public void validate(List<String> references){
        if(references==null || references.isEmpty() || references.size()>5 || new HashSet<>(references).size()!=references.size())throw new BusinessException("请上传1至5份不重复的医生资质");
        Path root=Path.of(uploadPath,"qualifications").toAbsolutePath().normalize();
        for(String reference:references){
            if(reference==null || !reference.matches("/files/qualifications/[a-f0-9]{32}\\.(pdf|png|jpg)"))throw new BusinessException("资质文件地址无效，请重新上传");
            Path file=root.resolve(reference.substring(reference.lastIndexOf('/')+1)).normalize();
            try{if(!file.startsWith(root) || !Files.isRegularFile(file,LinkOption.NOFOLLOW_LINKS) || Files.size(file)>2*1024*1024 || Files.size(file)==0)throw new BusinessException("资质文件不存在或已失效，请重新上传");}
            catch(java.io.IOException e){throw new BusinessException("资质文件暂不可用，请重新上传");}
        }
    }
}
