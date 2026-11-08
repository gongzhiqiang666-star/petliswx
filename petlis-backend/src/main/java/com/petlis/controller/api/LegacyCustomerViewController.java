package com.petlis.controller.api;
import com.petlis.common.response.Result;import com.petlis.service.impl.LegacyCustomerViewService;import lombok.RequiredArgsConstructor;import org.springframework.web.bind.annotation.*;import java.util.*;
@RestController @RequestMapping("admin/legacy/customer-views") @RequiredArgsConstructor
public class LegacyCustomerViewController {
 private final LegacyCustomerViewService service;
 @GetMapping("/capabilities") public Result<Map<String,Boolean>> capabilities(){return Result.success(service.capabilities());}
 @PostMapping("/preview") public Result<Map<String,Object>> preview(@RequestBody LegacyCustomerViewService.Command command){return Result.success(service.preview(command));}
 @PostMapping("/month-preview") public Result<Map<String,Object>> month(@RequestBody LegacyCustomerViewService.MonthCommand command){return Result.success(service.month(command));}
}
