package com.petlis.controller.api;
import com.petlis.common.response.Result;import com.petlis.service.impl.LegacyFinanceMailService;import lombok.RequiredArgsConstructor;import org.springframework.web.bind.annotation.*;import java.util.Map;
@RestController @RequestMapping("admin/legacy/finance-mails") @RequiredArgsConstructor public class LegacyFinanceMailController {
 private final LegacyFinanceMailService service;
 @GetMapping("/capabilities") public Result<Map<String,Boolean>> capabilities(){return Result.success(service.capabilities());}
 @GetMapping("/hospitals/{hospital}/compose") public Result<Map<String,Object>> compose(@PathVariable long hospital,@RequestParam(required=false)String month,@RequestParam(required=false)String from,@RequestParam(required=false)String to){return Result.success(service.compose(hospital,new LegacyFinanceMailService.Period(month,from,to)));}
 @PostMapping("/hospitals/{hospital}") public Result<Long> queue(@PathVariable long hospital,@RequestBody LegacyFinanceMailService.FinanceCommand command){return Result.success(service.queueFinance(hospital,command));}
 @GetMapping("/hospitals/{hospital}/messages") public Result<Map<String,Object>> messages(@PathVariable long hospital,@RequestParam(required=false)String month,@RequestParam(required=false)String from,@RequestParam(required=false)String to){return Result.success(service.financeMessages(hospital,new LegacyFinanceMailService.Period(month,from,to)));}
 @GetMapping("/messages/{id}") public Result<Map<String,Object>> message(@PathVariable long id){return Result.success(service.message(id));}
 @PostMapping("/messages/{id}/dispatch") public Result<Map<String,Object>> dispatch(@PathVariable long id){return Result.success(service.dispatch(id));}
 @PostMapping("/messages/{id}/cancel") public Result<Map<String,Object>> cancel(@PathVariable long id){return Result.success(service.cancel(id));}
 @PostMapping("/messages/{id}/reconcile") public Result<Map<String,Object>> reconcile(@PathVariable long id,@RequestBody LegacyFinanceMailService.ReconcileCommand command){return Result.success(service.reconcile(id,command));}
}
