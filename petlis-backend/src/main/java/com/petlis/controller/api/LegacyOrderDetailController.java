package com.petlis.controller.api;
import com.petlis.common.response.Result;
import com.petlis.service.impl.LegacyOrderDetailService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import java.util.Map;
@RestController @RequestMapping("admin/legacy/order-details") @RequiredArgsConstructor
public class LegacyOrderDetailController {
 private final LegacyOrderDetailService service;
 @GetMapping("/capabilities") public Result<Map<String,Boolean>> capabilities(){return Result.success(service.capabilities());}
 @GetMapping("/orders/{orderId}") public Result<Map<String,Object>> list(@PathVariable long orderId,@RequestParam(defaultValue="0") int page,@RequestParam(defaultValue="20") int size){return Result.success(service.list(orderId,page,size));}
 @GetMapping("/orders/{orderId}/{id}") public Result<Map<String,Object>> detail(@PathVariable long orderId,@PathVariable long id){return Result.success(service.detail(orderId,id));}
 @GetMapping("/orders/{orderId}/form-defaults") public Result<Map<String,Object>> formDefaults(@PathVariable long orderId){return Result.success(service.formDefaults(orderId));}
 @PostMapping("/orders/{orderId}") public Result<Long> create(@PathVariable long orderId,@RequestBody LegacyOrderDetailService.CreateCommand command){return Result.success(service.create(orderId,command));}
 @PutMapping("/orders/{orderId}/{id}") public Result<Long> update(@PathVariable long orderId,@PathVariable long id,@RequestBody LegacyOrderDetailService.UpdateCommand command){return Result.success(service.update(orderId,id,command));}
 @PostMapping("/orders/{orderId}/quote") public Result<Map<String,String>> quote(@PathVariable long orderId,@RequestBody LegacyOrderDetailService.QuoteCommand command){return Result.success(Map.of("price",service.quote(orderId,command)));}
 @GetMapping("/tests") public Result<Map<String,Object>> tests(@RequestParam(required=false) String q,@RequestParam(defaultValue="0") int page,@RequestParam(defaultValue="20") int size){return Result.success(service.tests(q,page,size));}
 @GetMapping("/tests/{testId}/prices") public Result<Map<String,Object>> prices(@PathVariable long testId,@RequestParam(defaultValue="0") int page,@RequestParam(defaultValue="20") int size){return Result.success(service.testPrices(testId,page,size));}
 @GetMapping("/tests/{testId}/templates") public Result<Map<String,Object>> templates(@PathVariable long testId,@RequestParam(defaultValue="0") int page,@RequestParam(defaultValue="20") int size){return Result.success(service.testTemplates(testId,page,size));}
 @GetMapping("/templates/{id}") public Result<Map<String,Object>> template(@PathVariable long id){return Result.success(service.reportTemplate(id));}
 @GetMapping("/mail-templates") public Result<Map<String,Object>> mails(@RequestParam(required=false) String q,@RequestParam(defaultValue="0") int page,@RequestParam(defaultValue="20") int size){return Result.success(service.mailTemplates(q,page,size));}
 @GetMapping("/mail-templates/{id}") public Result<Map<String,Object>> mail(@PathVariable long id){return Result.success(service.mailTemplate(id));}
 @GetMapping("/orders/{orderId}/test-mail-title") public Result<Map<String,String>> testMailTitle(@PathVariable long orderId,@RequestParam long testId){return Result.success(Map.of("title",service.testMailTitle(orderId,testId)));}
 @GetMapping("/orders/{orderId}/mail-title") public Result<Map<String,String>> mailTitle(@PathVariable long orderId,@RequestParam(required=false) String title){return Result.success(Map.of("title",service.mailTitle(orderId,title)));}
 @GetMapping("/orders/{orderId}/render/report/{templateId}") public Result<Map<String,String>> renderReport(@PathVariable long orderId,@PathVariable long templateId,@RequestParam(defaultValue="0") long detailId){return Result.success(Map.of("html",service.renderReport(orderId,detailId,templateId)));}
 @GetMapping("/orders/{orderId}/render/mail/{templateId}") public Result<Map<String,String>> renderMail(@PathVariable long orderId,@PathVariable long templateId,@RequestParam(defaultValue="0") long detailId){return Result.success(Map.of("html",service.renderMail(orderId,detailId,templateId)));}
}
