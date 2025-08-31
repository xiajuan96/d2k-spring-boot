package example.d2k.payment.retry.entity;

/**
 * 通知状态枚举
 * @author xiajuan96
 */
public enum NotificationStatus {
    
    /**
     * 待通知
     */
    PENDING("待通知"),
    
    /**
     * 通知中
     */
    NOTIFYING("通知中"),
    
    /**
     * 通知成功
     */
    SUCCESS("通知成功"),
    
    /**
     * 通知失败
     */
    FAILED("通知失败"),
    
    /**
     * 重试中
     */
    RETRYING("重试中"),
    
    /**
     * 重试失败（达到最大重试次数）
     */
    RETRY_FAILED("重试失败"),
    
    /**
     * 已放弃（不再重试）
     */
    ABANDONED("已放弃");
    
    private final String description;
    
    NotificationStatus(String description) {
        this.description = description;
    }
    
    public String getDescription() {
        return description;
    }
    
    /**
     * 判断是否为最终状态（不会再变化）
     */
    public boolean isFinalStatus() {
        return this == SUCCESS || this == RETRY_FAILED || this == ABANDONED;
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
        return this == FAILED || this == RETRY_FAILED || this == ABANDONED;
    }
    
    /**
     * 判断是否可以重试
     */
    public boolean canRetry() {
        return this == PENDING || this == FAILED || this == RETRYING;
    }
    
    /**
     * 判断是否正在处理中
     */
    public boolean isProcessing() {
        return this == NOTIFYING || this == RETRYING;
    }
}