package com.petlis.controller.api;

import com.petlis.common.response.Result;
import com.petlis.common.security.SecurityUtils;
import com.petlis.dto.response.BillingCheckResponse;
import com.petlis.service.BillingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

/**
 * 账单控制器
 *
 * @author PETLIS
 * @since 1.0.0
 */
@Slf4j
@RestController
@RequestMapping("billing")
@RequiredArgsConstructor
@Tag(name = "账单管理", description = "月结客户账单相关接口")
public class BillingController {

    private final BillingService billingService;

    /**
     * 检查月结客户账单状态
     * 用于查看PDF前的账单检查
     */
    @GetMapping("/check")
    @Operation(summary = "检查账单状态", description = "检查月结客户是否有未支付账单，用于PDF查看权限验证")
    public Result<BillingCheckResponse> checkBilling() {
        // 从当前登录用户获取医院ID
        Integer hospitalId = SecurityUtils.getCurrentHospitalId();

        log.info("检查医院账单状态, 医院ID: {}", hospitalId);
        BillingCheckResponse response = billingService.checkMonthlyBilling(hospitalId);

        return Result.success(response);
    }

    /**
     * 获取当前账单月份
     */
    @GetMapping("/current-month")
    @Operation(summary = "获取当前账单月份", description = "获取当前需要结清的账单月份")
    public Result<String> getCurrentBillMonth() {
        String billMonth = billingService.getCurrentBillMonth();
        return Result.success(billMonth);
    }
}
