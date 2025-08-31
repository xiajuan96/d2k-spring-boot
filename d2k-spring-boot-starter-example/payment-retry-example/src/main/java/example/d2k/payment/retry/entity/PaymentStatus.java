package example.d2k.payment.retry.entity;

/**
 * 支付状态枚举
 * @author xiajuan96
 */
public enum PaymentStatus {
    
    /**
     * 支付中
     */
    PROCESSING("支付中"),
    
    /**
     * 支付成功
     */
    SUCCESS("支付成功"),
    
    /**
     * 支付失败
     */
    FAILED("支付失败"),
    
    /**
     * 支付取消
     */
    CANCELLED("支付取消"),
    
    /**
     * 支付超时
     */
    TIMEOUT("支付超时"),
    
    /**
     * 退款中
     */
    REFUNDING("退款中"),
    
    /**
     * 已退款
     */
    REFUNDED("已退款");
    
    private final String description;
    
    PaymentStatus(String description) {
        this.description = description;
    }
    
    public String getDescription() {
        return description;
    }
    
    /**
     * 判断是否为最终状态（不会再变化）
     */
    public boolean isFinalStatus() {
        return this == SUCCESS || this == FAILED || this == CANCELLED || 
               this == TIMEOUT || this == REFUNDED;
    }
    
    /**
     * 判断是否为成功状态
     */
    public boolean isSuccess() {
        return this == SUCCESS;
    }
    
    /**
     * 判断是否为失败状态
     */
    public boolean isFailure() {
        return this == FAILED || this == CANCELLED || this == TIMEOUT;
    }
    
    /**
     * 判断是否需要通知商户
     */
    public boolean needsNotification() {
        return isFinalStatus();
    }
}