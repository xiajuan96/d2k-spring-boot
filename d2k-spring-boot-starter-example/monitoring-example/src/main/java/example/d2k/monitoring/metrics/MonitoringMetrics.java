package example.d2k.monitoring.metrics;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;

import javax.persistence.*;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 监控指标实体类
 * 用于收集和存储各种监控数据
 * 
 * @author xiajuan96
 */
@Entity
@Table(name = "monitoring_metrics")
@JsonInclude(JsonInclude.Include.NON_NULL)
public class MonitoringMetrics {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 基本信息
    @Column(name = "instance_id", nullable = false)
    private String instanceId;

    @Column(name = "node_id")
    private String nodeId;

    @Column(name = "application_name")
    private String applicationName;

    @Column(name = "environment")
    private String environment;

    @Column(name = "timestamp", nullable = false)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime timestamp;

    // Kafka监控指标
    @Column(name = "kafka_producer_count")
    private Long kafkaProducerCount = 0L;

    @Column(name = "kafka_consumer_count")
    private Long kafkaConsumerCount = 0L;

    @Column(name = "kafka_messages_sent")
    private Long kafkaMessagesSent = 0L;

    @Column(name = "kafka_messages_received")
    private Long kafkaMessagesReceived = 0L;

    @Column(name = "kafka_send_errors")
    private Long kafkaSendErrors = 0L;

    @Column(name = "kafka_consume_errors")
    private Long kafkaConsumeErrors = 0L;

    @Column(name = "kafka_avg_send_latency")
    private Double kafkaAvgSendLatency = 0.0;

    @Column(name = "kafka_avg_consume_latency")
    private Double kafkaAvgConsumeLatency = 0.0;

    @Column(name = "kafka_max_send_latency")
    private Double kafkaMaxSendLatency = 0.0;

    @Column(name = "kafka_max_consume_latency")
    private Double kafkaMaxConsumeLatency = 0.0;

    // 系统监控指标
    @Column(name = "cpu_usage")
    private Double cpuUsage = 0.0;

    @Column(name = "memory_used")
    private Long memoryUsed = 0L;

    @Column(name = "memory_total")
    private Long memoryTotal = 0L;

    @Column(name = "memory_usage_percent")
    private Double memoryUsagePercent = 0.0;

    @Column(name = "heap_used")
    private Long heapUsed = 0L;

    @Column(name = "heap_total")
    private Long heapTotal = 0L;

    @Column(name = "heap_usage_percent")
    private Double heapUsagePercent = 0.0;

    @Column(name = "non_heap_used")
    private Long nonHeapUsed = 0L;

    @Column(name = "non_heap_total")
    private Long nonHeapTotal = 0L;

    // 线程监控指标
    @Column(name = "thread_count")
    private Integer threadCount = 0;

    @Column(name = "daemon_thread_count")
    private Integer daemonThreadCount = 0;

    @Column(name = "peak_thread_count")
    private Integer peakThreadCount = 0;

    @Column(name = "total_started_thread_count")
    private Long totalStartedThreadCount = 0L;

    // 连接池监控指标
    @Column(name = "db_active_connections")
    private Integer dbActiveConnections = 0;

    @Column(name = "db_idle_connections")
    private Integer dbIdleConnections = 0;

    @Column(name = "db_max_connections")
    private Integer dbMaxConnections = 0;

    @Column(name = "redis_active_connections")
    private Integer redisActiveConnections = 0;

    @Column(name = "redis_idle_connections")
    private Integer redisIdleConnections = 0;

    @Column(name = "redis_max_connections")
    private Integer redisMaxConnections = 0;

    // 应用监控指标
    @Column(name = "http_requests_total")
    private Long httpRequestsTotal = 0L;

    @Column(name = "http_requests_success")
    private Long httpRequestsSuccess = 0L;

    @Column(name = "http_requests_error")
    private Long httpRequestsError = 0L;

    @Column(name = "http_avg_response_time")
    private Double httpAvgResponseTime = 0.0;

    @Column(name = "http_max_response_time")
    private Double httpMaxResponseTime = 0.0;

    // 缓存监控指标
    @Column(name = "cache_hits")
    private Long cacheHits = 0L;

    @Column(name = "cache_misses")
    private Long cacheMisses = 0L;

    @Column(name = "cache_hit_rate")
    private Double cacheHitRate = 0.0;

    @Column(name = "cache_evictions")
    private Long cacheEvictions = 0L;

    @Column(name = "cache_size")
    private Long cacheSize = 0L;

    // 健康状态
    @Enumerated(EnumType.STRING)
    @Column(name = "health_status")
    private HealthStatus healthStatus = HealthStatus.UP;

    @Column(name = "health_details", columnDefinition = "TEXT")
    private String healthDetails;

    // 告警状态
    @Enumerated(EnumType.STRING)
    @Column(name = "alert_level")
    private AlertLevel alertLevel = AlertLevel.NORMAL;

    @Column(name = "active_alerts")
    private Integer activeAlerts = 0;

    @Column(name = "alert_details", columnDefinition = "TEXT")
    private String alertDetails;

    // 构造函数
    public MonitoringMetrics() {
        this.timestamp = LocalDateTime.now();
    }

    public MonitoringMetrics(String instanceId) {
        this();
        this.instanceId = instanceId;
    }

    // 健康状态枚举
    public enum HealthStatus {
        UP("正常"),
        DOWN("故障"),
        WARNING("警告"),
        UNKNOWN("未知");

        private final String description;

        HealthStatus(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }
    }

    // 告警级别枚举
    public enum AlertLevel {
        NORMAL("正常"),
        INFO("信息"),
        WARNING("警告"),
        ERROR("错误"),
        CRITICAL("严重");

        private final String description;

        AlertLevel(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }
    }

    // Topic监控指标内嵌类
    @Embeddable
    public static class TopicMetrics {
        private String topicName;
        private Integer partitionCount;
        private Long messagesSent;
        private Long messagesReceived;
        private Double avgLatency;
        private Double maxLatency;
        private Long errorCount;

        // 构造函数、getter和setter
        public TopicMetrics() {}

        public TopicMetrics(String topicName) {
            this.topicName = topicName;
        }

        // Getters and Setters
        public String getTopicName() { return topicName; }
        public void setTopicName(String topicName) { this.topicName = topicName; }
        public Integer getPartitionCount() { return partitionCount; }
        public void setPartitionCount(Integer partitionCount) { this.partitionCount = partitionCount; }
        public Long getMessagesSent() { return messagesSent; }
        public void setMessagesSent(Long messagesSent) { this.messagesSent = messagesSent; }
        public Long getMessagesReceived() { return messagesReceived; }
        public void setMessagesReceived(Long messagesReceived) { this.messagesReceived = messagesReceived; }
        public Double getAvgLatency() { return avgLatency; }
        public void setAvgLatency(Double avgLatency) { this.avgLatency = avgLatency; }
        public Double getMaxLatency() { return maxLatency; }
        public void setMaxLatency(Double maxLatency) { this.maxLatency = maxLatency; }
        public Long getErrorCount() { return errorCount; }
        public void setErrorCount(Long errorCount) { this.errorCount = errorCount; }
    }

    // 计算方法
    public Double getKafkaErrorRate() {
        long totalMessages = kafkaMessagesSent + kafkaMessagesReceived;
        long totalErrors = kafkaSendErrors + kafkaConsumeErrors;
        return totalMessages > 0 ? (double) totalErrors / totalMessages : 0.0;
    }

    public Double getHttpSuccessRate() {
        return httpRequestsTotal > 0 ? (double) httpRequestsSuccess / httpRequestsTotal : 0.0;
    }

    public Double getHttpErrorRate() {
        return httpRequestsTotal > 0 ? (double) httpRequestsError / httpRequestsTotal : 0.0;
    }

    public boolean isHealthy() {
        return healthStatus == HealthStatus.UP;
    }

    public boolean hasAlerts() {
        return activeAlerts > 0 || alertLevel != AlertLevel.NORMAL;
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getInstanceId() { return instanceId; }
    public void setInstanceId(String instanceId) { this.instanceId = instanceId; }

    public String getNodeId() { return nodeId; }
    public void setNodeId(String nodeId) { this.nodeId = nodeId; }

    public String getApplicationName() { return applicationName; }
    public void setApplicationName(String applicationName) { this.applicationName = applicationName; }

    public String getEnvironment() { return environment; }
    public void setEnvironment(String environment) { this.environment = environment; }

    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }

    public Long getKafkaProducerCount() { return kafkaProducerCount; }
    public void setKafkaProducerCount(Long kafkaProducerCount) { this.kafkaProducerCount = kafkaProducerCount; }

    public Long getKafkaConsumerCount() { return kafkaConsumerCount; }
    public void setKafkaConsumerCount(Long kafkaConsumerCount) { this.kafkaConsumerCount = kafkaConsumerCount; }

    public Long getKafkaMessagesSent() { return kafkaMessagesSent; }
    public void setKafkaMessagesSent(Long kafkaMessagesSent) { this.kafkaMessagesSent = kafkaMessagesSent; }

    public Long getKafkaMessagesReceived() { return kafkaMessagesReceived; }
    public void setKafkaMessagesReceived(Long kafkaMessagesReceived) { this.kafkaMessagesReceived = kafkaMessagesReceived; }

    public Long getKafkaSendErrors() { return kafkaSendErrors; }
    public void setKafkaSendErrors(Long kafkaSendErrors) { this.kafkaSendErrors = kafkaSendErrors; }

    public Long getKafkaConsumeErrors() { return kafkaConsumeErrors; }
    public void setKafkaConsumeErrors(Long kafkaConsumeErrors) { this.kafkaConsumeErrors = kafkaConsumeErrors; }

    public Double getKafkaAvgSendLatency() { return kafkaAvgSendLatency; }
    public void setKafkaAvgSendLatency(Double kafkaAvgSendLatency) { this.kafkaAvgSendLatency = kafkaAvgSendLatency; }

    public Double getKafkaAvgConsumeLatency() { return kafkaAvgConsumeLatency; }
    public void setKafkaAvgConsumeLatency(Double kafkaAvgConsumeLatency) { this.kafkaAvgConsumeLatency = kafkaAvgConsumeLatency; }

    public Double getKafkaMaxSendLatency() { return kafkaMaxSendLatency; }
    public void setKafkaMaxSendLatency(Double kafkaMaxSendLatency) { this.kafkaMaxSendLatency = kafkaMaxSendLatency; }

    public Double getKafkaMaxConsumeLatency() { return kafkaMaxConsumeLatency; }
    public void setKafkaMaxConsumeLatency(Double kafkaMaxConsumeLatency) { this.kafkaMaxConsumeLatency = kafkaMaxConsumeLatency; }

    public Double getCpuUsage() { return cpuUsage; }
    public void setCpuUsage(Double cpuUsage) { this.cpuUsage = cpuUsage; }

    public Long getMemoryUsed() { return memoryUsed; }
    public void setMemoryUsed(Long memoryUsed) { this.memoryUsed = memoryUsed; }

    public Long getMemoryTotal() { return memoryTotal; }
    public void setMemoryTotal(Long memoryTotal) { this.memoryTotal = memoryTotal; }

    public Double getMemoryUsagePercent() { return memoryUsagePercent; }
    public void setMemoryUsagePercent(Double memoryUsagePercent) { this.memoryUsagePercent = memoryUsagePercent; }

    public Long getHeapUsed() { return heapUsed; }
    public void setHeapUsed(Long heapUsed) { this.heapUsed = heapUsed; }

    public Long getHeapTotal() { return heapTotal; }
    public void setHeapTotal(Long heapTotal) { this.heapTotal = heapTotal; }

    public Double getHeapUsagePercent() { return heapUsagePercent; }
    public void setHeapUsagePercent(Double heapUsagePercent) { this.heapUsagePercent = heapUsagePercent; }

    public Long getNonHeapUsed() { return nonHeapUsed; }
    public void setNonHeapUsed(Long nonHeapUsed) { this.nonHeapUsed = nonHeapUsed; }

    public Long getNonHeapTotal() { return nonHeapTotal; }
    public void setNonHeapTotal(Long nonHeapTotal) { this.nonHeapTotal = nonHeapTotal; }

    public Integer getThreadCount() { return threadCount; }
    public void setThreadCount(Integer threadCount) { this.threadCount = threadCount; }

    public Integer getDaemonThreadCount() { return daemonThreadCount; }
    public void setDaemonThreadCount(Integer daemonThreadCount) { this.daemonThreadCount = daemonThreadCount; }

    public Integer getPeakThreadCount() { return peakThreadCount; }
    public void setPeakThreadCount(Integer peakThreadCount) { this.peakThreadCount = peakThreadCount; }

    public Long getTotalStartedThreadCount() { return totalStartedThreadCount; }
    public void setTotalStartedThreadCount(Long totalStartedThreadCount) { this.totalStartedThreadCount = totalStartedThreadCount; }

    public Integer getDbActiveConnections() { return dbActiveConnections; }
    public void setDbActiveConnections(Integer dbActiveConnections) { this.dbActiveConnections = dbActiveConnections; }

    public Integer getDbIdleConnections() { return dbIdleConnections; }
    public void setDbIdleConnections(Integer dbIdleConnections) { this.dbIdleConnections = dbIdleConnections; }

    public Integer getDbMaxConnections() { return dbMaxConnections; }
    public void setDbMaxConnections(Integer dbMaxConnections) { this.dbMaxConnections = dbMaxConnections; }

    public Integer getRedisActiveConnections() { return redisActiveConnections; }
    public void setRedisActiveConnections(Integer redisActiveConnections) { this.redisActiveConnections = redisActiveConnections; }

    public Integer getRedisIdleConnections() { return redisIdleConnections; }
    public void setRedisIdleConnections(Integer redisIdleConnections) { this.redisIdleConnections = redisIdleConnections; }

    public Integer getRedisMaxConnections() { return redisMaxConnections; }
    public void setRedisMaxConnections(Integer redisMaxConnections) { this.redisMaxConnections = redisMaxConnections; }

    public Long getHttpRequestsTotal() { return httpRequestsTotal; }
    public void setHttpRequestsTotal(Long httpRequestsTotal) { this.httpRequestsTotal = httpRequestsTotal; }

    public Long getHttpRequestsSuccess() { return httpRequestsSuccess; }
    public void setHttpRequestsSuccess(Long httpRequestsSuccess) { this.httpRequestsSuccess = httpRequestsSuccess; }

    public Long getHttpRequestsError() { return httpRequestsError; }
    public void setHttpRequestsError(Long httpRequestsError) { this.httpRequestsError = httpRequestsError; }

    public Double getHttpAvgResponseTime() { return httpAvgResponseTime; }
    public void setHttpAvgResponseTime(Double httpAvgResponseTime) { this.httpAvgResponseTime = httpAvgResponseTime; }

    public Double getHttpMaxResponseTime() { return httpMaxResponseTime; }
    public void setHttpMaxResponseTime(Double httpMaxResponseTime) { this.httpMaxResponseTime = httpMaxResponseTime; }

    public Long getCacheHits() { return cacheHits; }
    public void setCacheHits(Long cacheHits) { this.cacheHits = cacheHits; }

    public Long getCacheMisses() { return cacheMisses; }
    public void setCacheMisses(Long cacheMisses) { this.cacheMisses = cacheMisses; }

    public Double getCacheHitRate() { return cacheHitRate; }
    public void setCacheHitRate(Double cacheHitRate) { this.cacheHitRate = cacheHitRate; }

    public Long getCacheEvictions() { return cacheEvictions; }
    public void setCacheEvictions(Long cacheEvictions) { this.cacheEvictions = cacheEvictions; }

    public Long getCacheSize() { return cacheSize; }
    public void setCacheSize(Long cacheSize) { this.cacheSize = cacheSize; }

    public HealthStatus getHealthStatus() { return healthStatus; }
    public void setHealthStatus(HealthStatus healthStatus) { this.healthStatus = healthStatus; }

    public String getHealthDetails() { return healthDetails; }
    public void setHealthDetails(String healthDetails) { this.healthDetails = healthDetails; }

    public AlertLevel getAlertLevel() { return alertLevel; }
    public void setAlertLevel(AlertLevel alertLevel) { this.alertLevel = alertLevel; }

    public Integer getActiveAlerts() { return activeAlerts; }
    public void setActiveAlerts(Integer activeAlerts) { this.activeAlerts = activeAlerts; }

    public String getAlertDetails() { return alertDetails; }
    public void setAlertDetails(String alertDetails) { this.alertDetails = alertDetails; }

    @Override
    public String toString() {
        return "MonitoringMetrics{" +
                "instanceId='" + instanceId + '\'' +
                ", timestamp=" + timestamp +
                ", healthStatus=" + healthStatus +
                ", alertLevel=" + alertLevel +
                ", kafkaMessagesSent=" + kafkaMessagesSent +
                ", kafkaMessagesReceived=" + kafkaMessagesReceived +
                ", memoryUsagePercent=" + memoryUsagePercent +
                ", cpuUsage=" + cpuUsage +
                '}';
    }
}