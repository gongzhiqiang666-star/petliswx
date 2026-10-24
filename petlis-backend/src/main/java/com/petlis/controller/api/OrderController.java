package com.petlis.controller.api;

import com.petlis.common.response.Result;
import com.petlis.dto.request.OrderCreateRequest;
import com.petlis.dto.request.OrderQueryRequest;
import com.petlis.dto.request.OrderUpdateRequest;
import com.petlis.dto.response.OrderCreateResponse;
import com.petlis.dto.response.OrderDetailResponse;
import com.petlis.dto.response.OrderResponse;
import com.petlis.entity.dict.InfoHospital;
import com.petlis.repository.InfoHospitalRepository;
import com.petlis.service.OrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

/**
 * 检测申请管理Controller
 *
 * @author PETLIS
 * @since 1.0.0
 */
@Slf4j
@RestController
@RequestMapping("orders")
@RequiredArgsConstructor
@Tag(name = "检测申请管理", description = "检测申请的创建、查询、更新等操作")
public class OrderController {

    private final OrderService orderService;
    private final InfoHospitalRepository hospitalRepository;

    /**
     * 服务器域名（用于生成安全PDF URL）
     */
    @Value("${server.domain}")
    private String serverDomain;

    /**
     * 创建检测申请
     *
     * @param request 创建请求
     * @return 申请单ID和支付信息
     */
    @PostMapping
    @Operation(summary = "创建检测申请", description = "创建新的检测申请单,初始状态为草稿。如果不是月结客户，返回需要支付的信息")
    public Result<OrderCreateResponse> createOrder(@Valid @RequestBody OrderCreateRequest request) {
        log.info("接收到创建检测申请请求");
        try {
            // 1. 创建订单
            Integer orderId = orderService.createOrder(request);

            // 2. 获取订单信息
            var order = orderService.getOrderDetail(orderId);
            BigDecimal totalFee = BigDecimal.valueOf(order.getTestFee() != null ? order.getTestFee() : 0);

            // 3. 检查医院是否是月结客户（PayType == 1 表示月结客户）
            Integer hospitalId = order.getHospitalId();
            InfoHospital hospital = hospitalRepository.findById(hospitalId).orElse(null);

            boolean isMonthlyCustomer = (hospital != null && hospital.getPayType() != null && hospital.getPayType() == 1);
            boolean needPayment = !isMonthlyCustomer;
            String payTypeDesc = isMonthlyCustomer ? "月结客户" : "现结客户";

            // 4. 构建响应
            OrderCreateResponse response = new OrderCreateResponse();
            response.setOrderId(orderId);
            response.setUserNO(order.getUserNO());
            response.setTotalFee(totalFee.doubleValue());
            response.setNeedPayment(needPayment);
            response.setPayTypeDesc(payTypeDesc);

            log.info("检测申请创建成功, 订单ID: {}, 是否需要支付: {}", orderId, needPayment);
            return Result.success("检测申请创建成功", response);
        } catch (Exception e) {
            log.error("创建检测申请失败", e);
            return Result.fail(e.getMessage());
        }
    }

    /**
     * 查询检测申请列表
     *
     * @param status    样本状态(可选)
     * @param startDate 开始日期(可选)
     * @param endDate   结束日期(可选)
     * @param page      页码(从0开始,默认0)
     * @param size      每页大小(默认10)
     * @return 分页结果
     */
    @GetMapping
    @Operation(summary = "查询检测申请列表", description = "根据条件查询检测申请列表,支持分页")
    public Result<Page<OrderResponse>> queryOrders(
            @Parameter(description = "样本状态") @RequestParam(required = false) Integer status,
            @Parameter(description = "开始日期") @RequestParam(required = false) String startDate,
            @Parameter(description = "结束日期") @RequestParam(required = false) String endDate,
            @Parameter(description = "页码") @RequestParam(required = false, defaultValue = "0") Integer page,
            @Parameter(description = "每页大小") @RequestParam(required = false, defaultValue = "10") Integer size
    ) {
        log.info("接收到查询检测申请列表请求");
        try {
            // 从token中获取医院ID
            Integer hospitalId = com.petlis.common.security.SecurityUtils.getCurrentHospitalId();

            OrderQueryRequest request = new OrderQueryRequest();
            request.setStatus(status);
            request.setHospitalId(hospitalId);
            request.setPage(page);
            request.setSize(size);

            Page<OrderResponse> result = orderService.queryOrders(request);
            return Result.success(result);
        } catch (Exception e) {
            log.error("查询检测申请列表失败", e);
            return Result.fail(e.getMessage());
        }
    }

    /**
     * 获取检测申请详情
     *
     * @param id 申请单ID
     * @param request HTTP请求（用于获取token）
     * @return 申请详情
     */
    @GetMapping("/{id}")
    @Operation(summary = "获取检测申请详情", description = "获取指定申请单的详细信息,包含检测项目明细")
    public Result<OrderDetailResponse> getOrderDetail(
            @Parameter(description = "申请单ID", required = true) @PathVariable Integer id,
            HttpServletRequest request
    ) {
        log.info("接收到获取检测申请详情请求, 申请单ID: {}", id);
        try {
            OrderDetailResponse response = orderService.getOrderDetail(id);

            // 填充安全PDF URL（带token验证）
            fillSecurePdfUrls(response, request);

            return Result.success(response);
        } catch (Exception e) {
            log.error("获取检测申请详情失败", e);
            return Result.fail(e.getMessage());
        }
    }

    /**
     * 填充安全PDF URL（带token验证）
     */
    private void fillSecurePdfUrls(OrderDetailResponse response, HttpServletRequest request) {
        if (response.getItems() == null || response.getItems().isEmpty()) {
            return;
        }

        // 获取token
        String token = extractToken(request);

        // 为每个检测项填充安全PDF URL
        response.getItems().forEach(item -> {
            if (item.getPdfFullUrl() != null && !item.getPdfFullUrl().isEmpty()) {
                // 生成安全访问URL：/api/pdf/view/{detailId}?token={token}
                String secureUrl = String.format("%s/api/pdf/view/%d?token=%s",
                        serverDomain,
                        item.getItemId(),
                        token);
                item.setSecurePdfUrl(secureUrl);
            }
        });
    }

    /**
     * 从请求中提取token
     */
    private String extractToken(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return "";
    }

    /**
     * 更新检测申请信息
     *
     * @param id      申请单ID
     * @param request 更新请求
     * @return 操作结果
     */
    @PutMapping("/{id}")
    @Operation(summary = "更新检测申请信息", description = "更新检测申请的基本信息(仅草稿状态可修改)")
    public Result<Void> updateOrder(
            @Parameter(description = "申请单ID", required = true) @PathVariable Integer id,
            @Valid @RequestBody OrderUpdateRequest request
    ) {
        log.info("接收到更新检测申请请求, 申请单ID: {}", id);
        try {
            orderService.updateOrder(id, request);
            return Result.success("检测申请更新成功");
        } catch (Exception e) {
            log.error("更新检测申请失败", e);
            return Result.fail(e.getMessage());
        }
    }

    /**
     * 更新检测申请状态
     *
     * @param id     申请单ID
     * @param status 目标状态
     * @return 操作结果
     */
    @PutMapping("/{id}/status")
    @Operation(summary = "更新检测申请状态", description = "更新检测申请的状态,支持状态流转验证")
    public Result<Void> updateOrderStatus(
            @Parameter(description = "申请单ID", required = true) @PathVariable Integer id,
            @Parameter(description = "目标状态", required = true) @RequestParam Integer status
    ) {
        log.info("接收到更新检测申请状态请求, 申请单ID: {}, 目标状态: {}", id, status);
        try {
            orderService.updateOrderStatus(id, status);
            return Result.success("状态更新成功");
        } catch (Exception e) {
            log.error("更新检测申请状态失败", e);
            return Result.fail(e.getMessage());
        }
    }

    /**
     * 添加检测项目
     *
     * @param id     申请单ID
     * @param request 检测项请求对象
     * @return 明细ID
     */
    @PostMapping("/{id}/items")
    @Operation(summary = "添加检测项目", description = "向申请单添加检测项目")
    public Result<Integer> addOrderItem(
            @Parameter(description = "申请单ID", required = true) @PathVariable Integer id,
            @Valid @RequestBody com.petlis.dto.request.OrderItemRequest request
    ) {
        log.info("接收到添加检测项目请求, 申请单ID: {}, 检测项目ID: {}, 城市ID: {}",
                id, request.getTestTypeId(), request.getCityId());
        try {
            Integer itemId = orderService.addOrderItem(id, request.getTestTypeId(), request.getCityId());
            return Result.success("检测项目添加成功", itemId);
        } catch (Exception e) {
            log.error("添加检测项目失败", e);
            return Result.fail(e.getMessage());
        }
    }

    /**
     * 删除检测项目
     *
     * @param id     申请单ID
     * @param itemId 明细ID
     * @return 操作结果
     */
    @DeleteMapping("/{id}/items/{itemId}")
    @Operation(summary = "删除检测项目", description = "从申请单中删除指定的检测项目")
    public Result<Void> removeOrderItem(
            @Parameter(description = "申请单ID", required = true) @PathVariable Integer id,
            @Parameter(description = "明细ID", required = true) @PathVariable Integer itemId
    ) {
        log.info("接收到删除检测项目请求, 申请单ID: {}, 明细ID: {}", id, itemId);
        try {
            orderService.removeOrderItem(id, itemId);
            return Result.success("检测项目删除成功");
        } catch (Exception e) {
            log.error("删除检测项目失败", e);
            return Result.fail(e.getMessage());
        }
    }

    /**
     * 计算申请总费用
     *
     * @param id 申请单ID
     * @return 总费用
     */
    @GetMapping("/{id}/fee")
    @Operation(summary = "计算申请总费用", description = "计算申请单的检测总费用")
    public Result<BigDecimal> calculateTotalFee(
            @Parameter(description = "申请单ID", required = true) @PathVariable Integer id
    ) {
        log.info("接收到计算申请总费用请求, 申请单ID: {}", id);
        try {
            BigDecimal fee = orderService.calculateTotalFee(id);
            return Result.success(fee);
        } catch (Exception e) {
            log.error("计算申请总费用失败", e);
            return Result.fail(e.getMessage());
        }
    }
}
