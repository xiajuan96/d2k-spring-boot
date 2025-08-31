package example.d2k.user.behavior.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import javax.persistence.*;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * 用户行为事件实体
 * @author xiajuan96
 */
@Entity
@Table(name = "user_behavior_event")
@JsonIgnoreProperties(ignoreUnknown = true)
public class UserBehaviorEvent {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    /**
     * 事件ID（唯一标识）
     */
    @Column(name = "event_id", unique = true, nullable = false, length = 64)
    private String eventId;
    
    /**
     * 用户ID
     */
    @Column(name = "user_id", nullable = false, length = 64)
    private String userId;
    
    /**
     * 会话ID
     */
    @Column(name = "session_id", length = 64)
    private String sessionId;
    
    /**
     * 事件类型
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false)
    private BehaviorEventType eventType;
    
    /**
     * 事件名称
     */
    @Column(name = "event_name", nullable = false, length = 100)
    private String eventName;
    
    /**
     * 页面路径
     */
    @Column(name = "page_path", length = 500)
    private String pagePath;
    
    /**
     * 页面标题
     */
    @Column(name = "page_title", length = 200)
    private String pageTitle;
    
    /**
     * 引用页面
     */
    @Column(name = "referrer", length = 500)
    private String referrer;
    
    /**
     * 用户代理
     */
    @Column(name = "user_agent", length = 1000)
    private String userAgent;
    
    /**
     * IP地址
     */
    @Column(name = "ip_address", length = 45)
    private String ipAddress;
    
    /**
     * 设备类型
     */
    @Column(name = "device_type", length = 50)
    private String deviceType;
    
    /**
     * 操作系统
     */
    @Column(name = "operating_system", length = 50)
    private String operatingSystem;
    
    /**
     * 浏览器
     */
    @Column(name = "browser", length = 50)
    private String browser;
    
    /**
     * 地理位置（国家）
     */
    @Column(name = "country", length = 50)
    private String country;
    
    /**
     * 地理位置（城市）
     */
    @Column(name = "city", length = 100)
    private String city;
    
    /**
     * 事件属性（JSON格式）
     */
    @Column(name = "properties", columnDefinition = "TEXT")
    private String properties;
    
    /**
     * 事件值（用于数值型分析）
     */
    @Column(name = "event_value")
    private Double eventValue;
    
    /**
     * 货币单位
     */
    @Column(name = "currency", length = 10)
    private String currency;
    
    /**
     * 事件时间戳
     */
    @Column(name = "event_timestamp", nullable = false)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime eventTimestamp;
    
    /**
     * 处理状态
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "process_status", nullable = false)
    private ProcessStatus processStatus = ProcessStatus.PENDING;
    
    /**
     * 延迟处理时间（分钟）
     */
    @Column(name = "delay_minutes")
    private Integer delayMinutes;
    
    /**
     * 预定处理时间
     */
    @Column(name = "scheduled_process_time")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime scheduledProcessTime;
    
    /**
     * 实际处理时间
     */
    @Column(name = "actual_process_time")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime actualProcessTime;
    
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
     * 重试次数
     */
    @Column(name = "retry_count")
    private Integer retryCount = 0;
    
    /**
     * 最大重试次数
     */
    @Column(name = "max_retry_count")
    private Integer maxRetryCount = 3;
    
    /**
     * 创建时间
     */
    @Column(name = "create_time", nullable = false)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;
    
    /**
     * 更新时间
     */
    @Column(name = "update_time")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime;
    
    // 构造函数
    public UserBehaviorEvent() {
        this.createTime = LocalDateTime.now();
        this.eventTimestamp = LocalDateTime.now();
    }
    
    public UserBehaviorEvent(String eventId, String userId, BehaviorEventType eventType, String eventName) {
        this();
        this.eventId = eventId;
        this.userId = userId;
        this.eventType = eventType;
        this.eventName = eventName;
    }
    
    // 业务方法
    
    /**
     * 设置事件属性
     */
    public void setPropertiesMap(Map<String, Object> propertiesMap) {
        if (propertiesMap != null && !propertiesMap.isEmpty()) {
            try {
                ObjectMapper mapper = new ObjectMapper();
                this.properties = mapper.writeValueAsString(propertiesMap);
            } catch (JsonProcessingException e) {
                throw new RuntimeException("Failed to serialize properties", e);
            }
        }
    }
    
    /**
     * 获取事件属性
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> getPropertiesMap() {
        if (properties == null || properties.trim().isEmpty()) {
            return new HashMap<>();
        }
        try {
            ObjectMapper mapper = new ObjectMapper();
            return mapper.readValue(properties, Map.class);
        } catch (JsonProcessingException e) {
            return new HashMap<>();
        }
    }
    
    /**
     * 添加单个属性
     */
    public void addProperty(String key, Object value) {
        Map<String, Object> propertiesMap = getPropertiesMap();
        propertiesMap.put(key, value);
        setPropertiesMap(propertiesMap);
    }
    
    /**
     * 设置延迟处理时间
     */
    public void setDelayProcessing(int delayMinutes) {
        this.delayMinutes = delayMinutes;
        this.scheduledProcessTime = LocalDateTime.now().plusMinutes(delayMinutes);
    }
    
    /**
     * 标记为处理中
     */
    public void markAsProcessing() {
        this.processStatus = ProcessStatus.PROCESSING;
        this.actualProcessTime = LocalDateTime.now();
        this.updateTime = LocalDateTime.now();
    }
    
    /**
     * 标记为处理成功
     */
    public void markAsProcessed(String result) {
        this.processStatus = ProcessStatus.PROCESSED;
        this.processResult = result;
        this.updateTime = LocalDateTime.now();
    }
    
    /**
     * 标记为处理失败
     */
    public void markAsFailed(String errorMessage) {
        this.processStatus = ProcessStatus.FAILED;
        this.errorMessage = errorMessage;
        this.updateTime = LocalDateTime.now();
    }
    
    /**
     * 增加重试次数
     */
    public void incrementRetryCount() {
        this.retryCount++;
        this.updateTime = LocalDateTime.now();
    }
    
    /**
     * 是否可以重试
     */
    public boolean canRetry() {
        return this.retryCount < this.maxRetryCount && 
               (this.processStatus == ProcessStatus.FAILED || this.processStatus == ProcessStatus.PENDING);
    }
    
    /**
     * 是否已达到最大重试次数
     */
    public boolean hasReachedMaxRetries() {
        return this.retryCount >= this.maxRetryCount;
    }
    
    /**
     * 是否需要延迟处理
     */
    public boolean needsDelayedProcessing() {
        return this.delayMinutes != null && this.delayMinutes > 0;
    }
    
    /**
     * 是否到了处理时间
     */
    public boolean isReadyForProcessing() {
        if (scheduledProcessTime == null) {
            return true;
        }
        return LocalDateTime.now().isAfter(scheduledProcessTime);
    }
    
    // Getter 和 Setter 方法
    
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public String getEventId() {
        return eventId;
    }
    
    public void setEventId(String eventId) {
        this.eventId = eventId;
    }
    
    public String getUserId() {
        return userId;
    }
    
    public void setUserId(String userId) {
        this.userId = userId;
    }
    
    public String getSessionId() {
        return sessionId;
    }
    
    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }
    
    public BehaviorEventType getEventType() {
        return eventType;
    }
    
    public void setEventType(BehaviorEventType eventType) {
        this.eventType = eventType;
    }
    
    public String getEventName() {
        return eventName;
    }
    
    public void setEventName(String eventName) {
        this.eventName = eventName;
    }
    
    public String getPagePath() {
        return pagePath;
    }
    
    public void setPagePath(String pagePath) {
        this.pagePath = pagePath;
    }
    
    public String getPageTitle() {
        return pageTitle;
    }
    
    public void setPageTitle(String pageTitle) {
        this.pageTitle = pageTitle;
    }
    
    public String getReferrer() {
        return referrer;
    }
    
    public void setReferrer(String referrer) {
        this.referrer = referrer;
    }
    
    public String getUserAgent() {
        return userAgent;
    }
    
    public void setUserAgent(String userAgent) {
        this.userAgent = userAgent;
    }
    
    public String getIpAddress() {
        return ipAddress;
    }
    
    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }
    
    public String getDeviceType() {
        return deviceType;
    }
    
    public void setDeviceType(String deviceType) {
        this.deviceType = deviceType;
    }
    
    public String getOperatingSystem() {
        return operatingSystem;
    }
    
    public void setOperatingSystem(String operatingSystem) {
        this.operatingSystem = operatingSystem;
    }
    
    public String getBrowser() {
        return browser;
    }
    
    public void setBrowser(String browser) {
        this.browser = browser;
    }
    
    public String getCountry() {
        return country;
    }
    
    public void setCountry(String country) {
        this.country = country;
    }
    
    public String getCity() {
        return city;
    }
    
    public void setCity(String city) {
        this.city = city;
    }
    
    public String getProperties() {
        return properties;
    }
    
    public void setProperties(String properties) {
        this.properties = properties;
    }
    
    public Double getEventValue() {
        return eventValue;
    }
    
    public void setEventValue(Double eventValue) {
        this.eventValue = eventValue;
    }
    
    public String getCurrency() {
        return currency;
    }
    
    public void setCurrency(String currency) {
        this.currency = currency;
    }
    
    public LocalDateTime getEventTimestamp() {
        return eventTimestamp;
    }
    
    public void setEventTimestamp(LocalDateTime eventTimestamp) {
        this.eventTimestamp = eventTimestamp;
    }
    
    public ProcessStatus getProcessStatus() {
        return processStatus;
    }
    
    public void setProcessStatus(ProcessStatus processStatus) {
        this.processStatus = processStatus;
    }
    
    public Integer getDelayMinutes() {
        return delayMinutes;
    }
    
    public void setDelayMinutes(Integer delayMinutes) {
        this.delayMinutes = delayMinutes;
    }
    
    public LocalDateTime getScheduledProcessTime() {
        return scheduledProcessTime;
    }
    
    public void setScheduledProcessTime(LocalDateTime scheduledProcessTime) {
        this.scheduledProcessTime = scheduledProcessTime;
    }
    
    public LocalDateTime getActualProcessTime() {
        return actualProcessTime;
    }
    
    public void setActualProcessTime(LocalDateTime actualProcessTime) {
        this.actualProcessTime = actualProcessTime;
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
    
    @Override
    public String toString() {
        return "UserBehaviorEvent{" +
                "id=" + id +
                ", eventId='" + eventId + '\'' +
                ", userId='" + userId + '\'' +
                ", eventType=" + eventType +
                ", eventName='" + eventName + '\'' +
                ", processStatus=" + processStatus +
                ", eventTimestamp=" + eventTimestamp +
                '}';
    }
}