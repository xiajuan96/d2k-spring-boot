package example.d2k.payment.retry.entity;

import javax.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 支付通知实体类
 * @author xiajuan96
 */
@Entity
@Table(name = "payment_notifications")
public class PaymentNotification {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "payment_id", unique = true, nullable = false)
    private String paymentId;
    
    @Column(name = "order_no", nullable = false)
    private String orderNo;
    
    @Column(name = "amount", nullable = false)
    private BigDecimal amount;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private PaymentStatus status;
    
    @Column(name = "payment_method")
    private String paymentMethod;
    
    @Column(name = "transaction_id")
    private String transactionId;
    
    @Column(name = "callback_url", nullable = false)
    private String callbackUrl;
    
    @Column(name = "retry_count")
    private Integer retryCount = 0;
    
    @Column(name = "max_retry_count")
    private Integer maxRetryCount = 5;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "notification_status")
    private NotificationStatus notificationStatus = NotificationStatus.PENDING;
    
    @Column(name = "create_time", nullable = false)
    private LocalDateTime createTime;
    
    @Column(name = "update_time")
    private LocalDateTime updateTime;
    
    @Column(name = "last_retry_time")
    private LocalDateTime lastRetryTime;
    
    @Column(name = "next_retry_time")
    private LocalDateTime nextRetryTime;
    
    @Column(name = "error_message", length = 1000)
    private String errorMessage;
    
    public PaymentNotification() {
        this.createTime = LocalDateTime.now();
    }
    
    public PaymentNotification(String paymentId, String orderNo, BigDecimal amount, 
                             PaymentStatus status, String callbackUrl) {
        this();
        this.paymentId = paymentId;
        this.orderNo = orderNo;
        this.amount = amount;
        this.status = status;
        this.callbackUrl = callbackUrl;
    }
    
    // Getters and Setters
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public String getPaymentId() {
        return paymentId;
    }
    
    public void setPaymentId(String paymentId) {
        this.paymentId = paymentId;
    }
    
    public String getOrderNo() {
        return orderNo;
    }
    
    public void setOrderNo(String orderNo) {
        this.orderNo = orderNo;
    }
    
    public BigDecimal getAmount() {
        return amount;
    }
    
    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }
    
    public PaymentStatus getStatus() {
        return status;
    }
    
    public void setStatus(PaymentStatus status) {
        this.status = status;
        this.updateTime = LocalDateTime.now();
    }
    
    public String getPaymentMethod() {
        return paymentMethod;
    }
    
    public void setPaymentMethod(String paymentMethod) {
        this.paymentMethod = paymentMethod;
    }
    
    public String getTransactionId() {
        return transactionId;
    }
    
    public void setTransactionId(String transactionId) {
        this.transactionId = transactionId;
    }
    
    public String getCallbackUrl() {
        return callbackUrl;
    }
    
    public void setCallbackUrl(String callbackUrl) {
        this.callbackUrl = callbackUrl;
    }
    
    public Integer getRetryCount() {
        return retryCount;
    }
    
    public void setRetryCount(Integer retryCount) {
        this.retryCount = retryCount;
    }
    
    public Integer getMaxRetryCount() {
        return maxRetryCount;
    }
    
    public void setMaxRetryCount(Integer maxRetryCount) {
        this.maxRetryCount = maxRetryCount;
    }
    
    public NotificationStatus getNotificationStatus() {
        return notificationStatus;
    }
    
    public void setNotificationStatus(NotificationStatus notificationStatus) {
        this.notificationStatus = notificationStatus;
        this.updateTime = LocalDateTime.now();
    }
    
    public LocalDateTime getCreateTime() {
        return createTime;
    }
    
    public void setCreateTime(LocalDateTime createTime) {
        this.createTime = createTime;
    }
    
    public LocalDateTime getUpdateTime() {
        return updateTime;
    }
    
    public void setUpdateTime(LocalDateTime updateTime) {
        this.updateTime = updateTime;
    }
    
    public LocalDateTime getLastRetryTime() {
        return lastRetryTime;
    }
    
    public void setLastRetryTime(LocalDateTime lastRetryTime) {
        this.lastRetryTime = lastRetryTime;
    }
    
    public LocalDateTime getNextRetryTime() {
        return nextRetryTime;
    }
    
    public void setNextRetryTime(LocalDateTime nextRetryTime) {
        this.nextRetryTime = nextRetryTime;
    }
    
    public String getErrorMessage() {
        return errorMessage;
    }
    
    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }
    
    /**
     * 增加重试次数
     */
    public void incrementRetryCount() {
        this.retryCount++;
        this.lastRetryTime = LocalDateTime.now();
        this.updateTime = LocalDateTime.now();
    }
    
    /**
     * 判断是否可以重试
     */
    public boolean canRetry() {
        return this.retryCount < this.maxRetryCount && 
               this.notificationStatus != NotificationStatus.SUCCESS;
    }
    
    /**
     * 判断是否已达到最大重试次数
     */
    public boolean isMaxRetryReached() {
        return this.retryCount >= this.maxRetryCount;
    }
    
    @Override
    public String toString() {
        return "PaymentNotification{" +
                "id=" + id +
                ", paymentId='" + paymentId + '\'' +
                ", orderNo='" + orderNo + '\'' +
                ", amount=" + amount +
                ", status=" + status +
                ", paymentMethod='" + paymentMethod + '\'' +
                ", transactionId='" + transactionId + '\'' +
                ", callbackUrl='" + callbackUrl + '\'' +
                ", retryCount=" + retryCount +
                ", maxRetryCount=" + maxRetryCount +
                ", notificationStatus=" + notificationStatus +
                ", createTime=" + createTime +
                ", updateTime=" + updateTime +
                ", lastRetryTime=" + lastRetryTime +
                ", nextRetryTime=" + nextRetryTime +
                ", errorMessage='" + errorMessage + '\'' +
                '}';
    }
}