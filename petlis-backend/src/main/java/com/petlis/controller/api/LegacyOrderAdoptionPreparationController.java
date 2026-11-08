package com.petlis.controller.api;
import com.petlis.common.response.Result;import com.petlis.service.impl.LegacyOrderAdoptionPreparationService;
import lombok.RequiredArgsConstructor;import org.springframework.web.bind.annotation.*;import java.util.Map;
@RestController @RequestMapping("admin/legacy/order-adoption") @RequiredArgsConstructor
public class LegacyOrderAdoptionPreparationController {
 private final LegacyOrderAdoptionPreparationService service;
 @GetMapping("/capabilities") public Result<Map<String,Boolean>> capabilities(){return Result.success(service.capabilities());}
 @GetMapping("/{id}") public Result<Map<String,Object>> preview(@PathVariable int id){return Result.success(service.preview(id));}
 @PostMapping("/{id}") public Result<Map<String,Object>> prepare(@PathVariable int id,@RequestBody LegacyOrderAdoptionPreparationService.Command command){return Result.success(service.prepare(id,command));}
}
