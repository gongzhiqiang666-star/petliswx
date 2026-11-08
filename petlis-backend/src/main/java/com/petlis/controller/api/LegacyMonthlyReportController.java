package com.petlis.controller.api;
import com.petlis.common.response.Result;import com.petlis.service.impl.LegacyMonthlyReportService;import lombok.RequiredArgsConstructor;import org.springframework.web.bind.annotation.*;import java.util.Map;
@RestController @RequestMapping("admin/legacy/monthly-reports") @RequiredArgsConstructor
public class LegacyMonthlyReportController {
 private final LegacyMonthlyReportService service;
 @GetMapping("/capabilities") public Result<Map<String,Boolean>> capabilities(){return Result.success(service.capabilities());}
 @GetMapping public Result<Map<String,Object>> list(@RequestParam(required=false)Long hospital,@RequestParam(required=false)String fromMonth,@RequestParam(required=false)String toMonth,@RequestParam(required=false)Integer notified,@RequestParam(defaultValue="0")int page,@RequestParam(defaultValue="50")int size){return Result.success(service.list(hospital,fromMonth,toMonth,notified,page,size));}
 @GetMapping("/templates/{kind}") public Result<Map<String,Object>> templates(@PathVariable String kind){return Result.success(service.templates(kind));}
 @GetMapping("/{hospital}/{month}/draft") public Result<Map<String,Object>> draft(@PathVariable long hospital,@PathVariable String month,@RequestParam(required=false)Long reportTemplate,@RequestParam(required=false)Long mailTemplate){return Result.success(service.draft(hospital,month,reportTemplate,mailTemplate));}
 @GetMapping("/{hospital}/range-draft") public Result<Map<String,Object>> rangeDraft(@PathVariable long hospital,@RequestParam String from,@RequestParam String to,@RequestParam(required=false)Long reportTemplate,@RequestParam(required=false)Long mailTemplate){return Result.success(service.rangeDraft(hospital,from,to,reportTemplate,mailTemplate));}

}
