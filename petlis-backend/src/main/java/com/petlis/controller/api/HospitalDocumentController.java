package com.petlis.controller.api;
import com.petlis.common.response.Result;
import com.petlis.service.impl.HospitalDocumentService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.util.Map;
@RestController @RequestMapping("admin/hospitals/{hospital}/documents") @RequiredArgsConstructor
public class HospitalDocumentController {
 private final HospitalDocumentService service;
 @GetMapping public Result<Map<String,Object>> list(@PathVariable int hospital,@RequestParam(defaultValue="0")int page,@RequestParam(defaultValue="20")int size){return Result.success(service.list(hospital,page,size));}
 @PostMapping public Result<Map<String,Object>> upload(@PathVariable int hospital,@RequestPart("file")MultipartFile file,@RequestParam String purpose,@RequestParam String reason,@RequestParam String requestKey,@RequestParam(required=false)Long previousFileId){return Result.success(service.upload(hospital,file,purpose,reason,requestKey,previousFileId));}
}
