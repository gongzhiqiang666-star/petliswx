package com.petlis.controller.api;
import com.petlis.common.response.Result;
import com.petlis.service.impl.LegacyOrderAdoptionService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
@RestController
@RequestMapping("legacy/orders")
@RequiredArgsConstructor
public class LegacyOrderController {
    private final LegacyOrderAdoptionService adoption;
    private final com.petlis.service.impl.LegacyOrderAdoptionPreparationService preparation;
    private final com.petlis.service.impl.LegacyClinicalService clinical;
    @GetMapping("/{id}/clinical")
    public Result<java.util.Map<String,Object>> clinical(@PathVariable int id) { return Result.success(clinical.read(id)); }
    @PostMapping("/{id}/clinical")
    public Result<Void> correct(@PathVariable int id, @RequestBody com.petlis.service.impl.LegacyClinicalService.Correction command) { clinical.correct(id,command);return Result.success("资料已修正"); }
    @PostMapping("/{id}/adopt")
    public Result<Void> adopt(@PathVariable int id, @RequestBody AdoptCommand command) {
        preparation.requireWrite();
        adoption.adopt(id, command.laboratoryId());
        return Result.success("旧单已接入统一流程，原金额和单号保留");
    }
    @PostMapping("/{id}/submit")
    public Result<Void> submit(@PathVariable int id, @RequestBody SubmitCommand command) {
        preparation.requireWrite();
        adoption.submitPrepared(id, command.doctorId(), command.laboratoryId(), command.confirmedAmount());
        return Result.success("原单已绑定医生并接入待审核流程");
    }
    public record SubmitCommand(int doctorId, int laboratoryId, java.math.BigDecimal confirmedAmount) { }
    public record AdoptCommand(int laboratoryId) { }
}
