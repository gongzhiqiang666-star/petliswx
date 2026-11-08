package com.petlis.controller.api;
import com.petlis.common.response.Result;
import com.petlis.service.impl.LegacyClinicalService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import java.util.Map;
@RestController
@RequestMapping("orders")
@RequiredArgsConstructor
public class OrderClinicalController {
    private final LegacyClinicalService clinical;
    @GetMapping("/{id}/clinical")
    public Result<Map<String,Object>> read(@PathVariable int id) { return Result.success(clinical.read(id)); }
}
