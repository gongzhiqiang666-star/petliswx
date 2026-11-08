package com.petlis.controller.api;
import com.petlis.common.response.Result;
import com.petlis.service.impl.LegacyHospitalContactService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import java.util.Map;
@RestController @RequestMapping("admin/legacy/hospital-contacts") @RequiredArgsConstructor
public class LegacyHospitalContactController {
 private final LegacyHospitalContactService service;
 @GetMapping("/capabilities") public Result<Map<String,Boolean>> capabilities(){return Result.success(service.capabilities());}
 @GetMapping("/{hospital}") public Result<Map<String,Object>> list(@PathVariable long hospital,@RequestParam(defaultValue="0") int page,@RequestParam(defaultValue="20") int size){return Result.success(service.list(hospital,page,size));}
 @GetMapping("/{hospital}/{id}") public Result<Map<String,Object>> detail(@PathVariable long hospital,@PathVariable long id){return Result.success(service.detail(hospital,id));}
 @GetMapping("/{hospital}/{id}/recipient") public Result<Map<String,Object>> recipient(@PathVariable long hospital,@PathVariable long id){return Result.success(service.recipient(hospital,id));}
 @PostMapping("/{hospital}") public Result<Long> create(@PathVariable long hospital,@RequestBody LegacyHospitalContactService.Command command){return Result.success(service.save(hospital,null,command));}
 @PutMapping("/{hospital}/{id}") public Result<Long> update(@PathVariable long hospital,@PathVariable long id,@RequestBody LegacyHospitalContactService.Command command){return Result.success(service.save(hospital,id,command));}
 @DeleteMapping("/{hospital}/{id}") public Result<Long> delete(@PathVariable long hospital,@PathVariable long id,@RequestBody LegacyHospitalContactService.DeleteCommand command){return Result.success(service.delete(hospital,id,command));}
}
