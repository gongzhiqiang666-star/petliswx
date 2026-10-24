package com.petlis.service.impl;

import com.petlis.common.exception.BusinessException;
import com.petlis.dto.response.BillingCheckResponse;
import com.petlis.entity.business.ListDiagnosisProject;
import com.petlis.entity.dict.InfoHospital;
import com.petlis.repository.InfoHospitalRepository;
import com.petlis.repository.ListDiagnosisProjectRepository;
import com.petlis.service.BillingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

/**
 * 账单服务实现类
 *
 * @author PETLIS
 * @since 1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BillingServiceImpl implements BillingService {

    private final InfoHospitalRepository hospitalRepository;
    private final ListDiagnosisProjectRepository orderRepository;
    private final com.petlis.repository.DiagnosisDetailRepository diagnosisDetailRepository;

    @Override
    @Transactional(readOnly = true)
    public BillingCheckResponse checkMonthlyBilling(Integer hospitalId) {
        log.info("检查月结客户账单, 医院ID: {}", hospitalId);

        // 1. 查询医院信息
        InfoHospital hospital = hospitalRepository.findById(hospitalId)
                .orElseThrow(() -> new BusinessException("医院不存在"));

        BillingCheckResponse response = new BillingCheckResponse();
        response.setBillMonth(getCurrentBillMonth());

        // 2. 判断是否为月结客户（PayType == 1）
        boolean isMonthlyCustomer = (hospital.getPayType() != null && hospital.getPayType() == 1);
        response.setIsMonthlyCustomer(isMonthlyCustomer);

        if (!isMonthlyCustomer) {
            // 非月结客户，不需要检查账单
            response.setHasUnpaidBills(false);
            response.setUnpaidAmount(0.0);
            response.setUnpaidCount(0);
            response.setAllowViewPdf(true);
            response.setMessage("非月结客户，无需检查账单");
            return response;
        }

        // 3. 计算账单月份的起始和结束日期
        String billMonth = getCurrentBillMonth();
        Date[] dateRange = getMonthDateRange(billMonth);
        Date monthStart = dateRange[0];
        Date monthEnd = dateRange[1];

        // 4. 查询该月份是否有未支付的检测项目
        // 查询条件（按文档要求）：
        // - p.InfoHospitalId = hospitalId (从ListDiagnosisProject)
        // - d.IsCheckPass = 0 (检查通过的有效检测项)
        // - d.PayStatus2 = 0 (未支付状态)
        // - p.ReceiveSampleDt 在账单周期内 (样本接收日期在指定月份)

        // 先查询符合条件的ListDiagnosisProject
        java.util.List<ListDiagnosisProject> projectsInMonth = orderRepository
                .findAll()
                .stream()
                .filter(order -> order.getInfoHospitalId().equals(hospitalId))
                .filter(order -> {
                    Date receiveSampleDt = order.getReceiveSampleDt();
                    if (receiveSampleDt == null) return false;
                    return !receiveSampleDt.before(monthStart) && !receiveSampleDt.after(monthEnd);
                })
                .toList();

        // 获取这些项目的ID列表
        java.util.List<Integer> projectIds = projectsInMonth.stream()
                .map(ListDiagnosisProject::getListDiagnosisProjectId)
                .toList();

        // 查询这些项目中未支付的DiagnosisDetail
        java.util.List<com.petlis.entity.business.DiagnosisDetail> unpaidDetails = projectIds.isEmpty()
                ? java.util.Collections.emptyList()
                : diagnosisDetailRepository.findAll().stream()
                        .filter(detail -> projectIds.contains(detail.getListDiagnosisProjectId()))
                        .filter(detail -> detail.getIsCheckPass() != null && detail.getIsCheckPass() == 0)
                        .filter(detail -> detail.getPayStatus2() != null && detail.getPayStatus2() == 0)
                        .toList();

        // 5. 计算未支付金额和数量
        double unpaidAmount = unpaidDetails.stream()
                .mapToDouble(detail -> detail.getTestPrice() != null ? detail.getTestPrice().doubleValue() : 0.0)
                .sum();

        int unpaidCount = unpaidDetails.size();

        response.setHasUnpaidBills(unpaidCount > 0);
        response.setUnpaidAmount(unpaidAmount);
        response.setUnpaidCount(unpaidCount);

        // 6. 判断是否允许查看PDF
        if (unpaidCount > 0) {
            response.setAllowViewPdf(false);
            response.setMessage(String.format("有%d笔未支付账单，共计%.2f元，请先结清账单", unpaidCount, unpaidAmount));
        } else {
            response.setAllowViewPdf(true);
            response.setMessage("无未支付账单，可以查看PDF");
        }

        log.info("账单检查完成: {}", response);
        return response;
    }

    @Override
    public String getCurrentBillMonth() {
        // 根据当前日期计算账单月份
        // 规则：每月20日是分界点
        // - 当前日期 >= 20日：需结清【上上个月】的账单
        //   示例：5月20日 → 需结清3月账单
        // - 当前日期 < 20日：需结清【上个月】的账单
        //   示例：5月10日 → 需结清4月账单

        Calendar cal = Calendar.getInstance();
        int dayOfMonth = cal.get(Calendar.DAY_OF_MONTH);

        if (dayOfMonth >= 20) {
            // 20日及以后，返回上上个月
            cal.add(Calendar.MONTH, -2);
        } else {
            // 20日以前，返回上个月
            cal.add(Calendar.MONTH, -1);
        }

        // 返回格式：yyyy-MM
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM");
        return sdf.format(cal.getTime());
    }

    /**
     * 获取月份的起始和结束日期
     *
     * @param billMonth 账单月份（格式：yyyy-MM）
     * @return [起始日期, 结束日期]
     */
    private Date[] getMonthDateRange(String billMonth) {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM");
            Date month = sdf.parse(billMonth);

            Calendar cal = Calendar.getInstance();
            cal.setTime(month);

            // 设置为月初第一天 00:00:00
            cal.set(Calendar.DAY_OF_MONTH, 1);
            cal.set(Calendar.HOUR_OF_DAY, 0);
            cal.set(Calendar.MINUTE, 0);
            cal.set(Calendar.SECOND, 0);
            cal.set(Calendar.MILLISECOND, 0);
            Date monthStart = cal.getTime();

            // 设置为月末最后一天 23:59:59
            cal.set(Calendar.DAY_OF_MONTH, cal.getActualMaximum(Calendar.DAY_OF_MONTH));
            cal.set(Calendar.HOUR_OF_DAY, 23);
            cal.set(Calendar.MINUTE, 59);
            cal.set(Calendar.SECOND, 59);
            cal.set(Calendar.MILLISECOND, 999);
            Date monthEnd = cal.getTime();

            return new Date[]{monthStart, monthEnd};
        } catch (Exception e) {
            log.error("解析账单月份失败: {}", billMonth, e);
            throw new BusinessException("账单月份格式错误");
        }
    }
}
