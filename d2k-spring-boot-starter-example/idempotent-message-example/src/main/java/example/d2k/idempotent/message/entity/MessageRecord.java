package example.d2k.idempotent.message.entity;

import javax.persistence.*;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * 消息记录实体类
 * 用于记录消息的处理状态和幂等性信息
 * @author xiajuan96
 */
@Entity
@Table(name = "message_record", indexes = {
    @Index(name = "idx_message_id", columnList = "messageId", unique = true),
    @Index(name = "idx_business_key", columnList = "businessKey"),
    @Index(name = "idx_message_type", columnList = "messageType"),
    @Index(name = "idx_status", columnList = "status"),
    @Index(name = "idx_created_time", columnList = "createdTime"),
    @Index(name = "idx_consumer_group", columnList = "consumerGroup")
})
public class MessageRecord {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    /**
     * 消息唯一标识
     */
    @Column(name = "message_id", nullable = false, unique = true, length = 128)
    private String messageId;
    
    /**
     * 业务唯一键（用于业务层面的幂等性判断）
     */
    @Column(name = "business_key", length = 128)
    private String businessKey;
    
    /**
     * 消息类型
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "message_type", nullable = false, length = 50)
    private MessageType messageType;
    
    /**
     * 消息内容
     */
    @Column(name = "message_content", columnDefinition = "TEXT")
    private String messageContent;
    
    /**
     * 消息头信息
     */
    @ElementCollection
    @CollectionTable(name = "message_headers", joinColumns = @JoinColumn(name = "message_record_id"))
    @MapKeyColumn(name = "header_key")
    @Column(name = "header_value")
    private Map<String, String> headers;
    
    /**
     * 消费者组
     */
    @Column(name = "consumer_group", length = 100)
    private String consumerGroup;
    
    /**
     * 消费者实例ID
     */
    @Column(name = "consumer_instance_id", length = 100)
    private String consumerInstanceId;
    
    /**
     * 处理状态
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private ProcessingStatus status;
    
    /**
     * 重试次数
     */
    @Column(name = "retry_count", nullable = false)
    private Integer retryCount = 0;
    
    /**
     * 最大重试次数
     */
    @Column(name = "max_retry_count", nullable = false)
    private Integer maxRetryCount = 3;
    
    /**
     * 处理结果
     */
    @Column(name = "process_result", columnDefinition = "TEXT")
    private String processResult;
    
    /**
     * 错误信息
     */
    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;
    
    /**
     * 错误堆栈
     */
    @Column(name = "error_stack", columnDefinition = "TEXT")
    private String errorStack;
    
    /**
     * 创建时间
     */
    @Column(name = "created_time", nullable = false)
    private LocalDateTime createdTime;
    
    /**
     * 更新时间
     */
    @Column(name = "updated_time")
    private LocalDateTime updatedTime;
    
    /**
     * 首次处理时间
     */
    @Column(name = "first_processed_time")
    private LocalDateTime firstProcessedTime;
    
    /**
     * 最后处理时间
     */
    @Column(name = "last_processed_time")
    private LocalDateTime lastProcessedTime;
    
    /**
     * 下次重试时间
     */
    @Column(name = "next_retry_time")
    private LocalDateTime nextRetryTime;
    
    /**
     * 完成时间
     */
    @Column(name = "completed_time")
    private LocalDateTime completedTime;
    
    /**
     * 处理耗时（毫秒）
     */
    @Column(name = "process_duration_ms")
    private Long processDurationMs;
    
    /**
     * 消息来源
     */
    @Column(name = "message_source", length = 100)
    private String messageSource;
    
    /**
     * 消息目标
     */
    @Column(name = "message_target", length = 100)
    private String messageTarget;
    
    /**
     * 消息优先级
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "priority", length = 10)
    private MessagePriority priority = MessagePriority.NORMAL;
    
    /**
     * 是否需要确认
     */
    @Column(name = "need_ack")
    private Boolean needAck = true;
    
    /**
     * 确认时间
     */
    @Column(name = "ack_time")
    private LocalDateTime ackTime;
    
    /**
     * 消息版本（用于乐观锁）
     */
    @Version
    @Column(name = "version")
    private Long version;
    
    // 构造函数
    public MessageRecord() {
        this.createdTime = LocalDateTime.now();
        this.status = ProcessingStatus.PENDING;
    }
    
    public MessageRecord(String messageId, MessageType messageType, String messageContent) {
        this();
        this.messageId = messageId;
        this.messageType = messageType;
        this.messageContent = messageContent;
    }
    
    // 业务方法
    
    /**
     * 开始处理消息
     */
    public void startProcessing() {
        this.status = ProcessingStatus.PROCESSING;
        this.updatedTime = LocalDateTime.now();
        if (this.firstProcessedTime == null) {
            this.firstProcessedTime = this.updatedTime;
        }
        this.lastProcessedTime = this.updatedTime;
    }
    
    /**
     * 标记处理成功
     */
    public void markSuccess(String result) {
        this.status = ProcessingStatus.SUCCESS;
        this.processResult = result;
        this.completedTime = LocalDateTime.now();
        this.updatedTime = this.completedTime;
        
        if (this.firstProcessedTime != null) {
            this.processDurationMs = java.time.Duration.between(
                this.firstProcessedTime, this.completedTime
            ).toMillis();
        }
    }
    
    /**
     * 标记处理失败
     */
    public void markFailure(String errorMessage, String errorStack) {
        this.status = ProcessingStatus.FAILED;
        this.errorMessage = errorMessage;
        this.errorStack = errorStack;
        this.updatedTime = LocalDateTime.now();
        this.lastProcessedTime = this.updatedTime;
    }
    
    /**
     * 增加重试次数
     */
    public void incrementRetryCount() {
        this.retryCount++;
        this.updatedTime = LocalDateTime.now();
    }
    
    /**
     * 设置下次重试时间
     */
    public void setNextRetryTime(int delaySeconds) {
        this.nextRetryTime = LocalDateTime.now().plusSeconds(delaySeconds);
        this.updatedTime = LocalDateTime.now();
    }
    
    /**
     * 判断是否可以重试
     */
    public boolean canRetry() {
        return this.retryCount < this.maxRetryCount && 
               (this.status == ProcessingStatus.FAILED || this.status == ProcessingStatus.RETRY_FAILED);
    }
    
    /**
     * 判断是否需要重试
     */
    public boolean needRetry() {
        return canRetry() && 
               this.nextRetryTime != null && 
               LocalDateTime.now().isAfter(this.nextRetryTime);
    }
    
    /**
     * 标记为重试状态
     */
    public void markRetry() {
        this.status = ProcessingStatus.RETRYING;
        this.updatedTime = LocalDateTime.now();
    }
    
    /**
     * 标记为跳过状态
     */
    public void markSkipped(String reason) {
        this.status = ProcessingStatus.SKIPPED;
        this.processResult = reason;
        this.completedTime = LocalDateTime.now();
        this.updatedTime = this.completedTime;
    }
    
    /**
     * 确认消息
     */
    public void acknowledge() {
        this.ackTime = LocalDateTime.now();
        this.updatedTime = this.ackTime;
    }
    
    /**
     * 判断是否为最终状态
     */
    public boolean isFinalStatus() {
        return this.status == ProcessingStatus.SUCCESS || 
               this.status == ProcessingStatus.SKIPPED || 
               (this.status == ProcessingStatus.FAILED && !canRetry());
    }
    
    /**
     * 判断是否处理成功
     */
    public boolean isSuccess() {
        return this.status == ProcessingStatus.SUCCESS;
    }
    
    /**
     * 判断是否处理失败
     */
    public boolean isFailed() {
        return this.status == ProcessingStatus.FAILED;
    }
    
    /**
     * 判断是否正在处理
     */
    public boolean isProcessing() {
        return this.status == ProcessingStatus.PROCESSING;
    }
    
    /**
     * 获取处理耗时描述
     */
    public String getProcessDurationDescription() {
        if (this.processDurationMs == null) {
            return "未知";
        }
        
        if (this.processDurationMs < 1000) {
            return this.processDurationMs + "ms";
        } else if (this.processDurationMs < 60000) {
            return String.format("%.2fs", this.processDurationMs / 1000.0);
        } else {
            return String.format("%.2fmin", this.processDurationMs / 60000.0);
        }
    }
    
    /**
     * 获取状态描述
     */
    public String getStatusDescription() {
        return this.status.getDescription();
    }
    
    // Getter 和 Setter 方法
    
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public String getMessageId() {
        return messageId;
    }
    
    public void setMessageId(String messageId) {
        this.messageId = messageId;
    }
    
    public String getBusinessKey() {
        return businessKey;
    }
    
    public void setBusinessKey(String businessKey) {
        this.businessKey = businessKey;
    }
    
    public MessageType getMessageType() {
        return messageType;
    }
    
    public void setMessageType(MessageType messageType) {
        this.messageType = messageType;
    }
    
    public String getMessageContent() {
        return messageContent;
    }
    
    public void setMessageContent(String messageContent) {
        this.messageContent = messageContent;
    }
    
    public Map<String, String> getHeaders() {
        return headers;
    }
    
    public void setHeaders(Map<String, String> headers) {
        this.headers = headers;
    }
    
    public String getConsumerGroup() {
        return consumerGroup;
    }
    
    public void setConsumerGroup(String consumerGroup) {
        this.consumerGroup = consumerGroup;
    }
    
    public String getConsumerInstanceId() {
        return consumerInstanceId;
    }
    
    public void setConsumerInstanceId(String consumerInstanceId) {
        this.consumerInstanceId = consumerInstanceId;
    }
    
    public ProcessingStatus getStatus() {
        return status;
    }
    
    public void setStatus(ProcessingStatus status) {
        this.status = status;
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
    
    public String getProcessResult() {
        return processResult;
    }
    
    public void setProcessResult(String processResult) {
        this.processResult = processResult;
    }
    
    public String getErrorMessage() {
        return errorMessage;
    }
    
    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }
    
    public String getErrorStack() {
        return errorStack;
    }
    
    public void setErrorStack(String errorStack) {
        this.errorStack = errorStack;
    }
    
    public LocalDateTime getCreatedTime() {
        return createdTime;
    }
    
    public void setCreatedTime(LocalDateTime createdTime) {
        this.createdTime = createdTime;
    }
    
    public LocalDateTime getUpdatedTime() {
        return updatedTime;
    }
    
    public void setUpdatedTime(LocalDateTime updatedTime) {
        this.updatedTime = updatedTime;
    }
    
    public LocalDateTime getFirstProcessedTime() {
        return firstProcessedTime;
    }
    
    public void setFirstProcessedTime(LocalDateTime firstProcessedTime) {
        this.firstProcessedTime = firstProcessedTime;
    }
    
    public LocalDateTime getLastProcessedTime() {
        return lastProcessedTime;
    }
    
    public void setLastProcessedTime(LocalDateTime lastProcessedTime) {
        this.lastProcessedTime = lastProcessedTime;
    }
    
    public LocalDateTime getNextRetryTime() {
        return nextRetryTime;
    }
    
    public void setNextRetryTime(LocalDateTime nextRetryTime) {
        this.nextRetryTime = nextRetryTime;
    }
    
    public LocalDateTime getCompletedTime() {
        return completedTime;
    }
    
    public void setCompletedTime(LocalDateTime completedTime) {
        this.completedTime = completedTime;
    }
    
    public Long getProcessDurationMs() {
        return processDurationMs;
    }
    
    public void setProcessDurationMs(Long processDurationMs) {
        this.processDurationMs = processDurationMs;
    }
    
    public String getMessageSource() {
        return messageSource;
    }
    
    public void setMessageSource(String messageSource) {
        this.messageSource = messageSource;
    }
    
    public String getMessageTarget() {
        return messageTarget;
    }
    
    public void setMessageTarget(String messageTarget) {
        this.messageTarget = messageTarget;
    }
    
    public MessagePriority getPriority() {
        return priority;
    }
    
    public void setPriority(MessagePriority priority) {
        this.priority = priority;
    }
    
    public Boolean getNeedAck() {
        return needAck;
    }
    
    public void setNeedAck(Boolean needAck) {
        this.needAck = needAck;
    }
    
    public LocalDateTime getAckTime() {
        return ackTime;
    }
    
    public void setAckTime(LocalDateTime ackTime) {
        this.ackTime = ackTime;
    }
    
    public Long getVersion() {
        return version;
    }
    
    public void setVersion(Long version) {
        this.version = version;
    }
    
    @Override
    public String toString() {
        return "MessageRecord{" +
                "id=" + id +
                ", messageId='" + messageId + '\'' +
                ", businessKey='" + businessKey + '\'' +
                ", messageType=" + messageType +
                ", status=" + status +
                ", retryCount=" + retryCount +
                ", createdTime=" + createdTime +
                '}';
    }
}