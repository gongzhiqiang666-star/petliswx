package com.petlis.controller.api;
import com.petlis.common.response.Result;
import com.petlis.service.impl.HospitalSubmissionService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.util.*;
@RestController @RequiredArgsConstructor
public class HospitalSubmissionController {
 private final HospitalSubmissionService service;
 @GetMapping("hospital-documents/hospitals") public Result<List<Map<String,Object>>> hospitals(){return Result.success(service.hospitals());}
 @GetMapping("hospitals/{hospital}/document-submissions") public Result<Map<String,Object>> list(@PathVariable int hospital,@RequestParam(defaultValue="0")int page,@RequestParam(defaultValue="20")int size){return Result.success(service.list(hospital,page,size));}
 @PostMapping("hospitals/{hospital}/document-submissions") public Result<Map<String,Object>> submit(@PathVariable int hospital,@RequestPart("file")MultipartFile file,@RequestParam String purpose,@RequestParam String reason,@RequestParam String requestKey,@RequestParam(required=false)Long previousFileId){return Result.success(service.submit(hospital,file,purpose,reason,requestKey,previousFileId));}
 @GetMapping("admin/hospital-documents/pending") public Result<Map<String,Object>> pending(@RequestParam(defaultValue="0")int page,@RequestParam(defaultValue="20")int size){return Result.success(service.pending(page,size));}
 @PostMapping("admin/hospital-documents/{id}/review") public Result<Map<String,Object>> review(@PathVariable long id,@RequestBody HospitalSubmissionService.Decision decision){return Result.success(service.review(id,decision));}
}
