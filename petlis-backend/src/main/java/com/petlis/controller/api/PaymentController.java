package com.petlis.controller.api;

import com.petlis.dto.request.PaymentCreateRequest;
import com.petlis.dto.response.PaymentResponse;
import com.petlis.common.response.Result;
import com.petlis.service.PaymentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.io.BufferedReader;
import java.util.HashMap;
import java.util.Map;

/**
 * 支付控制器
 *
 * @author PETLIS
 * @since 1.0.0
 */
@Slf4j
@RestController
@RequestMapping("payment")
@RequiredArgsConstructor
@Tag(name = "支付管理", description = "支付相关接口")
public class PaymentController {

    private final PaymentService paymentService;

    /**
     * 创建支付订单
     */
    @PostMapping("/create")
    @Operation(summary = "创建支付订单", description = "创建支付订单并获取支付参数")
    public Result<PaymentResponse> createPayment(@RequestBody PaymentCreateRequest request) {
        log.info("创建支付订单请求: {}", request);
        PaymentResponse response = paymentService.createPayment(request);
        return Result.success(response);
    }

    /**
     * 微信支付回调
     */
    @PostMapping("/notify")
    @Operation(summary = "微信支付回调", description = "处理微信支付异步通知")
    public String handlePaymentNotify(HttpServletRequest request) {
        try {
            // 读取回调数据
            BufferedReader reader = request.getReader();
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }

            log.info("收到微信支付回调: {}", sb.toString());

            // TODO: 解析XML，验证签名
            // TODO: 获取订单号和交易号
            // TODO: 调用paymentService.handlePaymentCallback()

            // 返回成功响应
            Map<String, String> response = new HashMap<>();
            response.put("return_code", "SUCCESS");
            response.put("return_msg", "OK");

            return buildXmlResponse(response);
        } catch (Exception e) {
            log.error("处理支付回调失败", e);
            Map<String, String> response = new HashMap<>();
            response.put("return_code", "FAIL");
            response.put("return_msg", e.getMessage());

            return buildXmlResponse(response);
        }
    }

    /**
     * 查询支付状态
     */
    @GetMapping("/status/{paymentNo}")
    @Operation(summary = "查询支付状态", description = "查询支付订单状态")
    public Result<Map<String, Object>> getPaymentStatus(@PathVariable String paymentNo) {
        Integer status = paymentService.getPaymentStatus(paymentNo);

        Map<String, Object> result = new HashMap<>();
        result.put("paymentNo", paymentNo);
        result.put("status", status);
        result.put("statusDesc", getStatusDesc(status));

        return Result.success(result);
    }

    /**
     * 关闭支付订单
     */
    @PostMapping("/close/{paymentNo}")
    @Operation(summary = "关闭支付订单", description = "关闭未支付的订单")
    public Result<Void> closePayment(@PathVariable String paymentNo) {
        paymentService.closePayment(paymentNo);
        return Result.success();
    }

    /**
     * 模拟支付回调（测试接口）
     * 仅用于开发环境模拟支付成功后的回调处理
     */
    @PostMapping("/mock-callback/{paymentNo}")
    @Operation(summary = "模拟支付回调", description = "测试接口：模拟微信支付成功回调，更新支付和订单状态")
    public Result<String> mockPaymentCallback(
            @PathVariable String paymentNo,
            @RequestParam(defaultValue = "true") Boolean success) {
        log.info("模拟支付回调 - 支付订单号: {}, 支付结果: {}", paymentNo, success);

        if (success) {
            // 模拟微信交易号
            String transactionId = "MOCK_TXN_" + System.currentTimeMillis();
            boolean result = paymentService.handlePaymentCallback(paymentNo, transactionId);

            if (result) {
                return Result.success("支付回调处理成功，订单已更新为已支付状态");
            } else {
                return Result.fail("支付回调处理失败");
            }
        } else {
            return Result.fail("模拟支付失败");
        }
    }

    /**
     * 构建XML响应
     */
    private String buildXmlResponse(Map<String, String> params) {
        StringBuilder xml = new StringBuilder();
        xml.append("<xml>");
        for (Map.Entry<String, String> entry : params.entrySet()) {
            xml.append("<").append(entry.getKey()).append(">")
               .append(entry.getValue())
               .append("</").append(entry.getKey()).append(">");
        }
        xml.append("</xml>");
        return xml.toString();
    }

    /**
     * 获取状态描述
     */
    private String getStatusDesc(Integer status) {
        if (status == null) {
            return "未知";
        }
        switch (status) {
            case 0:
                return "未支付";
            case 1:
                return "已支付";
            case 2:
                return "已关闭";
            case 3:
                return "已退款";
            default:
                return "未知";
        }
    }
}
