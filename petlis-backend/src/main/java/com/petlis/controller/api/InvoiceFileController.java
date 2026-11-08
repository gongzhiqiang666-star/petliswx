package com.petlis.controller.api;
import com.petlis.common.response.Result;
import com.petlis.service.impl.InvoiceFileService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
@RestController @RequestMapping("invoices") @RequiredArgsConstructor
public class InvoiceFileController {
    private final InvoiceFileService service;
    @PostMapping("/{id}/file") public Result<Void> issue(@PathVariable long id,@RequestPart("file") MultipartFile file) {service.issue(id,file);return Result.success("电子发票已上传并开具");}
    @GetMapping("/{id}/download") public ResponseEntity<Resource> download(@PathVariable long id){return service.download(id);}
}
