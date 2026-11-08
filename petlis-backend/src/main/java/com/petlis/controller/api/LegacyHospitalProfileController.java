package com.petlis.controller.api;
import com.petlis.common.response.Result;
import com.petlis.service.impl.LegacyHospitalProfileService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import java.util.Map;
@RestController @RequestMapping("admin/legacy/hospital-profiles") @RequiredArgsConstructor
public class LegacyHospitalProfileController {
 private final LegacyHospitalProfileService service;
 @GetMapping("/capabilities") public Result<Map<String,Boolean>> capabilities(){return Result.success(service.capabilities());}
 @GetMapping("/{id}") public Result<Map<String,Object>> detail(@PathVariable long id){return Result.success(service.detail(id));}
 @PutMapping("/{id}") public Result<Long> update(@PathVariable long id,@RequestBody LegacyHospitalProfileService.Command command){return Result.success(service.update(id,command));}
}
