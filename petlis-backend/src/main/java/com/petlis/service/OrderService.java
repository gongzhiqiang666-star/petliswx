package com.petlis.service;

import com.petlis.dto.request.OrderCreateRequest;
import com.petlis.dto.request.OrderQueryRequest;
import com.petlis.dto.request.OrderUpdateRequest;
import com.petlis.dto.response.OrderDetailResponse;
import com.petlis.dto.response.OrderResponse;
import org.springframework.data.domain.Page;

import java.math.BigDecimal;

/**
 * 检测申请Service接口
 *
 * @author PETLIS
 * @since 1.0.0
 */
public interface OrderService {

    /**
     * 创建检测申请
     *
     * @param request 创建请求
     * @return 申请单ID
     */
    Integer createOrder(OrderCreateRequest request);

    void resubmit(Integer orderId, OrderCreateRequest request);

    /**
     * 添加检测项目到申请单
     *
     * @param orderId     申请单ID
     * @param testTypeId  检测项目ID
     * @param cityId      城市ID(用于获取价格)
     * @return 明细ID
     */
    Integer addOrderItem(Integer orderId, Integer testTypeId, Integer cityId);

    /**
     * 删除检测项目
     *
     * @param orderId 申请单ID
     * @param itemId  明细ID
     */
    void removeOrderItem(Integer orderId, Integer itemId);

    /**
     * 更新检测申请信息
     *
     * @param orderId 申请单ID
     * @param request 更新请求
     */
    void updateOrder(Integer orderId, OrderUpdateRequest request);

    /**
     * 更新申请状态
     *
     * @param orderId    申请单ID
     * @param targetStatus 目标状态
     */
    void updateOrderStatus(Integer orderId, Integer targetStatus);

    /**
     * 查询检测申请列表(分页)
     *
     * @param request 查询请求
     * @return 分页结果
     */
    Page<OrderResponse> queryOrders(OrderQueryRequest request);

    /**
     * 获取申请详情(包含明细)
     *
     * @param orderId 申请单ID
     * @return 申请详情
     */
    OrderDetailResponse getOrderDetail(Integer orderId);

    /**
     * 计算申请总费用
     *
     * @param orderId 申请单ID
     * @return 总费用
     */
    BigDecimal calculateTotalFee(Integer orderId);
}
