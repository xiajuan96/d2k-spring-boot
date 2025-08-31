package example.d2k.order.timeout.entity;

/**
 * 订单状态枚举
 * @author xiajuan96
 */
public enum OrderStatus {
    
    /**
     * 待支付
     */
    PENDING("待支付"),
    
    /**
     * 已支付
     */
    PAID("已支付"),
    
    /**
     * 已取消
     */
    CANCELLED("已取消"),
    
    /**
     * 超时取消
     */
    TIMEOUT_CANCELLED("超时取消"),
    
    /**
     * 已完成
     */
    COMPLETED("已完成");
    
    private final String description;
    
    OrderStatus(String description) {
        this.description = description;
    }
    
    public String getDescription() {
        return description;
    }
    
    /**
     * 判断订单是否可以取消
     */
    public boolean canCancel() {
        return this == PENDING;
    }
    
    /**
     * 判断订单是否已完成（包括各种结束状态）
     */
    public boolean isFinished() {
        return this == PAID || this == CANCELLED || this == TIMEOUT_CANCELLED || this == COMPLETED;
    }
    
    /**
     * 判断订单是否为取消状态
     */
    public boolean isCancelled() {
        return this == CANCELLED || this == TIMEOUT_CANCELLED;
    }
}