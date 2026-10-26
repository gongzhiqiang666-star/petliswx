package com.petlis.service.impl;

import com.petlis.common.exception.BusinessException;
import com.petlis.common.security.SecurityUtils;
import com.petlis.dto.response.PaymentResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.*;

@Service
@RequiredArgsConstructor
public class RechargeService {
    private final FeatureStore store;
    private final WeChatPaymentGateway gateway;
    private final BusinessNotifications notifications;
    private static final BigDecimal MAX_BALANCE = new BigDecimal("99999999.99");
    public record Request(BigDecimal amount, String requestKey) {}

    @Transactional
    public PaymentResponse create(Request request) {
        if (request.amount()==null || request.amount().scale()>2 || request.amount().compareTo(BigDecimal.ONE)<0
                || request.amount().compareTo(new BigDecimal("50000"))>0
                || request.requestKey()==null || !request.requestKey().matches("[A-Za-z0-9_-]{16,64}"))
            throw new BusinessException("充值金额需为1至50,000元，最多两位小数");
        int userId=SecurityUtils.getCurrentUserId();
        var user=store.one("SELECT money,openid FROM lin_user WHERE id=? FOR UPDATE",userId);
        var existing=store.jdbc().queryForList("SELECT * FROM lin_recharge_order WHERE user_id=? AND request_key=?",userId,request.requestKey());
        if (!existing.isEmpty()) {
            var previous=existing.get(0);
            if (amount(previous).compareTo(request.amount())!=0) throw new BusinessException(409,"重复请求的充值金额不一致");
            return response(previous);
        }
        BigDecimal balance=new BigDecimal(user.get("money").toString());
        if (balance.add(request.amount()).compareTo(MAX_BALANCE)>0) throw new BusinessException("充值后余额超出账户上限");
        Long pending=store.jdbc().queryForObject("SELECT COUNT(*) FROM lin_recharge_order WHERE user_id=? AND status='pending'",Long.class,userId);
        if (pending!=null && pending>=3) throw new BusinessException("请先完成或关闭已有待支付充值记录");
        String paymentNo="RC"+UUID.randomUUID().toString().replace("-", "");
        BigDecimal amount=request.amount().setScale(2);
        store.jdbc().update("INSERT INTO lin_recharge_order(user_id,payment_no,amount,request_key) VALUES(?,?,?,?)",userId,paymentNo,amount,request.requestKey());
        String prepay=gateway.create(paymentNo,amount,(String)user.get("openid"),"Petlis 个人账户充值");
        store.jdbc().update("UPDATE lin_recharge_order SET prepay_id=? WHERE payment_no=?",prepay,paymentNo);
        return gateway.parameters(paymentNo,amount,prepay,false);
    }

    public Map<String,Object> list(int page,int size) {
        return store.page("SELECT COUNT(*) FROM lin_recharge_order WHERE user_id=?",
                "SELECT payment_no AS paymentNo,amount,status,created_at AS createdAt,paid_at AS paidAt FROM lin_recharge_order WHERE user_id=? ORDER BY id DESC",
                page,size,SecurityUtils.getCurrentUserId());
    }

    public Map<String,Object> status(String paymentNo) {
        return store.one("SELECT payment_no AS paymentNo,amount,status,created_at AS createdAt,paid_at AS paidAt FROM lin_recharge_order WHERE payment_no=? AND user_id=?",paymentNo,SecurityUtils.getCurrentUserId());
    }

    @Transactional
    public PaymentResponse resume(String paymentNo) {
        var row=store.one("SELECT * FROM lin_recharge_order WHERE payment_no=? AND user_id=? FOR UPDATE",paymentNo,SecurityUtils.getCurrentUserId());
        return response(row);
    }

    private PaymentResponse response(Map<String,Object> row) {
        if ("closed".equals(row.get("status"))) throw new BusinessException("该充值记录已关闭，请重新发起充值");
        boolean paid="paid".equals(row.get("status"));
        if (!paid && row.get("created_at") instanceof Timestamp t
                && t.toInstant().isBefore(Instant.now().minusSeconds(110*60))) throw new BusinessException("支付记录已过期，请关闭后重新充值");
        return gateway.parameters(row.get("payment_no").toString(),amount(row),(String)row.get("prepay_id"),paid);
    }

    @Transactional
    public void close(String paymentNo) {
        var row=store.one("SELECT * FROM lin_recharge_order WHERE payment_no=? AND user_id=? FOR UPDATE",paymentNo,SecurityUtils.getCurrentUserId());
        if ("closed".equals(row.get("status"))) return;
        if (!"pending".equals(row.get("status"))) throw new BusinessException("只能关闭未支付充值记录");
        gateway.close(paymentNo,(String)row.get("prepay_id"));
        store.jdbc().update("UPDATE lin_recharge_order SET status='closed' WHERE id=?",row.get("id"));
    }

    /** Called only after the shared callback verifier authenticates merchant, signature and result. */
    @Transactional
    public void verifiedCallback(String paymentNo, int totalFee, String transactionId) {
        if (transactionId==null || transactionId.isBlank() || transactionId.length()>100) throw new BusinessException("微信交易号无效");
        var row=store.one("SELECT * FROM lin_recharge_order WHERE payment_no=? FOR UPDATE",paymentNo);
        if (amount(row).movePointRight(2).intValueExact()!=totalFee) throw new BusinessException("充值回调金额不一致");
        if ("paid".equals(row.get("status"))) {
            if (!Objects.equals(transactionId,row.get("transaction_id"))) throw new BusinessException("重复回调的交易号不一致");
            return;
        }
        if (!"pending".equals(row.get("status"))) throw new BusinessException("充值订单状态不允许入账");
        if (Boolean.TRUE.equals(store.jdbc().queryForObject("SELECT EXISTS(SELECT 1 FROM lin_wechat_payment WHERE transaction_id=? AND status=1)",Boolean.class,transactionId)))
            throw new BusinessException("微信交易号已用于其他业务");
        int userId=((Number)row.get("user_id")).intValue();
        var user=store.one("SELECT money FROM lin_user WHERE id=? FOR UPDATE",userId);
        BigDecimal before=new BigDecimal(user.get("money").toString()),after=before.add(amount(row));
        if (after.compareTo(MAX_BALANCE)>0) throw new BusinessException("充值入账超出账户上限，请联系管理员核对");
        store.jdbc().update("UPDATE lin_recharge_order SET status='paid',transaction_id=?,paid_at=NOW() WHERE id=?",transactionId,row.get("id"));
        store.jdbc().update("UPDATE lin_user SET money=?,updatetime=UNIX_TIMESTAMP() WHERE id=?",after,userId);
        store.jdbc().update("INSERT INTO lin_user_money_log(user_id,money,`before`,`after`,memo,createtime) VALUES(?,?,?,?,?,UNIX_TIMESTAMP())",userId,amount(row),before,after,"个人充值 "+paymentNo);
        notifications.emit(userId,"wallet",paymentNo,"充值已到账","个人账户充值 ¥"+amount(row).toPlainString()+" 已到账。","pages/account/account");
    }

    private static BigDecimal amount(Map<String,Object> row) { return new BigDecimal(row.get("amount").toString()); }
}
