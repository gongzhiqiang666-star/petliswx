package com.petlis.controller.api;

import com.petlis.common.response.Result;
import com.petlis.service.impl.PlatformFileService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@RestController
@RequestMapping("platform-files")
@RequiredArgsConstructor
public class PlatformFileController {
    private final PlatformFileService files;

    @PostMapping
    public Result<Map<String, Object>> upload(@RequestPart("file") MultipartFile file,
            @RequestParam(defaultValue = "GENERAL") String bizType,
            @RequestParam(defaultValue = "0") String bizId) {
        return Result.success(files.upload(file, bizType, bizId));
    }

    @GetMapping("/{id}/download")
    public ResponseEntity<Resource> download(@PathVariable long id) { return files.download(id); }
}
