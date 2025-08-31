package example.d2k.performance.optimization.service;

import example.d2k.performance.optimization.metrics.PerformanceMetrics;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
// Redis相关导入已移除，使用虚假实现
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.ThreadMXBean;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 性能监控服务
 * 负责收集、分析和报告Kafka客户端的性能指标
 * @author xiajuan96
 */
@Service
public class PerformanceMonitoringService {
    
    private static final Logger logger = LoggerFactory.getLogger(PerformanceMonitoringService.class);
    
    @Autowired
    private MeterRegistry meterRegistry;
    
    // Redis模板已移除，使用虚假实现
    // private RedisTemplate<String, Object> redisTemplate;
    
    @Value("${app.performance.monitoring.enabled:true}")
    private boolean monitoringEnabled;
    
    @Value("${app.performance.monitoring.metrics-interval:30000}")
    private long metricsInterval;
    
    @Value("${app.performance.monitoring.retention-hours:24}")
    private int retentionHours;
    
    @Value("${app.performance.alert.enabled:true}")
    private boolean alertEnabled;
    
    @Value("${app.performance.alert.throughput-threshold:100}")
    private double throughputThreshold;
    
    @Value("${app.performance.alert.latency-threshold:1000}")
    private long latencyThreshold;
    
    @Value("${app.performance.alert.error-rate-threshold:0.05}")
    private double errorRateThreshold;
    
    // 性能指标存储
    private final Map<String, PerformanceMetrics> metricsMap = new ConcurrentHashMap<>();
    
    // Micrometer指标
    private Counter messagesSentCounter;
    private Counter messagesConsumedCounter;
    private Counter sendErrorsCounter;
    private Counter consumeErrorsCounter;
    private Timer sendLatencyTimer;
    private Timer processingTimeTimer;
    private Gauge throughputGauge;
    private Gauge errorRateGauge;
    private Gauge memoryUsageGauge;
    
    // 系统监控
    private final MemoryMXBean memoryMXBean = ManagementFactory.getMemoryMXBean();
    private final ThreadMXBean threadMXBean = ManagementFactory.getThreadMXBean();
    
    // 告警状态
    private final Map<String, AtomicLong> alertCounts = new ConcurrentHashMap<>();
    private final Map<String, LocalDateTime> lastAlertTimes = new ConcurrentHashMap<>();
    
    @PostConstruct
    public void init() {
        if (!monitoringEnabled) {
            logger.info("性能监控已禁用");
            return;
        }
        
        logger.info("初始化性能监控服务...");
        initializeMetrics();
        logger.info("性能监控服务初始化完成");
    }
    
    /**
     * 初始化Micrometer指标
     */
    private void initializeMetrics() {
        // 计数器
        messagesSentCounter = Counter.builder("kafka.messages.sent")
            .description("发送的消息总数")
            .register(meterRegistry);
        
        messagesConsumedCounter = Counter.builder("kafka.messages.consumed")
            .description("消费的消息总数")
            .register(meterRegistry);
        
        sendErrorsCounter = Counter.builder("kafka.send.errors")
            .description("发送错误总数")
            .register(meterRegistry);
        
        consumeErrorsCounter = Counter.builder("kafka.consume.errors")
            .description("消费错误总数")
            .register(meterRegistry);
        
        // 计时器
        sendLatencyTimer = Timer.builder("kafka.send.latency")
            .description("消息发送延迟")
            .register(meterRegistry);
        
        processingTimeTimer = Timer.builder("kafka.processing.time")
            .description("消息处理时间")
            .register(meterRegistry);
        
        // 仪表盘
        throughputGauge = Gauge.builder("kafka.throughput", this, PerformanceMonitoringService::getCurrentThroughput)
            .description("消息吞吐量")
            .register(meterRegistry);
        
        errorRateGauge = Gauge.builder("kafka.error.rate", this, PerformanceMonitoringService::getCurrentErrorRate)
            .description("错误率")
            .register(meterRegistry);
        
        memoryUsageGauge = Gauge.builder("jvm.memory.usage", this, PerformanceMonitoringService::getMemoryUsage)
            .description("JVM内存使用率")
            .register(meterRegistry);
    }
    
    /**
     * 获取或创建性能指标
     */
    public PerformanceMetrics getOrCreateMetrics(String instanceId, String clientType) {
        return metricsMap.computeIfAbsent(instanceId, k -> {
            PerformanceMetrics metrics = new PerformanceMetrics(instanceId, clientType);
            logger.debug("创建新的性能指标实例: instanceId={}, clientType={}", instanceId, clientType);
            return metrics;
        });
    }
    
    /**
     * 记录消息发送
     */
    public void recordMessageSent(String instanceId, String topic, long bytes, long latency) {
        if (!monitoringEnabled) return;
        
        PerformanceMetrics metrics = getOrCreateMetrics(instanceId, "PRODUCER");
        metrics.recordMessageSent(bytes, latency);
        
        // 记录Topic指标
        PerformanceMetrics.TopicMetrics topicMetrics = metrics.getTopicMetrics(topic);
        topicMetrics.recordMessage(bytes, latency);
        
        // 更新Micrometer指标
        messagesSentCounter.increment();
        sendLatencyTimer.record(latency, TimeUnit.MILLISECONDS);
        
        logger.debug("记录消息发送: instanceId={}, topic={}, bytes={}, latency={}ms", 
            instanceId, topic, bytes, latency);
    }
    
    /**
     * 记录发送错误
     */
    public void recordSendError(String instanceId, String topic, String errorType, Throwable error) {
        if (!monitoringEnabled) return;
        
        PerformanceMetrics metrics = getOrCreateMetrics(instanceId, "PRODUCER");
        metrics.recordSendError(errorType);
        
        // 记录Topic错误
        PerformanceMetrics.TopicMetrics topicMetrics = metrics.getTopicMetrics(topic);
        topicMetrics.recordError();
        
        // 更新Micrometer指标
        sendErrorsCounter.increment();
        
        logger.warn("记录发送错误: instanceId={}, topic={}, errorType={}, error={}", 
            instanceId, topic, errorType, error.getMessage());
    }
    
    /**
     * 记录消息消费
     */
    public void recordMessageConsumed(String instanceId, String topic, String partition, 
                                     long bytes, long processingTime, long offset) {
        if (!monitoringEnabled) return;
        
        PerformanceMetrics metrics = getOrCreateMetrics(instanceId, "CONSUMER");
        metrics.recordMessageConsumed(bytes, processingTime);
        
        // 记录Topic指标
        PerformanceMetrics.TopicMetrics topicMetrics = metrics.getTopicMetrics(topic);
        topicMetrics.recordMessage(bytes, processingTime);
        
        // 记录分区指标
        String topicPartition = topic + "-" + partition;
        PerformanceMetrics.PartitionMetrics partitionMetrics = metrics.getPartitionMetrics(topicPartition);
        partitionMetrics.recordMessage(bytes, offset);
        
        // 更新Micrometer指标
        messagesConsumedCounter.increment();
        processingTimeTimer.record(processingTime, TimeUnit.MILLISECONDS);
        
        logger.debug("记录消息消费: instanceId={}, topic={}, partition={}, bytes={}, processingTime={}ms, offset={}", 
            instanceId, topic, partition, bytes, processingTime, offset);
    }
    
    /**
     * 记录消费错误
     */
    public void recordConsumeError(String instanceId, String topic, String errorType, Throwable error) {
        if (!monitoringEnabled) return;
        
        PerformanceMetrics metrics = getOrCreateMetrics(instanceId, "CONSUMER");
        metrics.recordConsumeError(errorType);
        
        // 记录Topic错误
        PerformanceMetrics.TopicMetrics topicMetrics = metrics.getTopicMetrics(topic);
        topicMetrics.recordError();
        
        // 更新Micrometer指标
        consumeErrorsCounter.increment();
        
        logger.warn("记录消费错误: instanceId={}, topic={}, errorType={}, error={}", 
            instanceId, topic, errorType, error.getMessage());
    }
    
    /**
     * 记录批处理
     */
    public void recordBatch(String instanceId, int batchSize, long batchLatency) {
        if (!monitoringEnabled) return;
        
        PerformanceMetrics metrics = getOrCreateMetrics(instanceId, "PRODUCER");
        metrics.recordBatch(batchSize, batchLatency);
        
        logger.debug("记录批处理: instanceId={}, batchSize={}, batchLatency={}ms", 
            instanceId, batchSize, batchLatency);
    }
    
    /**
     * 更新系统指标
     */
    public void updateSystemMetrics(String instanceId) {
        if (!monitoringEnabled) return;
        
        PerformanceMetrics metrics = metricsMap.get(instanceId);
        if (metrics == null) return;
        
        // 更新内存指标
        long usedMemory = memoryMXBean.getHeapMemoryUsage().getUsed();
        long maxMemory = memoryMXBean.getHeapMemoryUsage().getMax();
        metrics.setUsedMemory(usedMemory);
        metrics.setMaxMemory(maxMemory);
        
        // 更新线程指标
        metrics.setActiveThreads(threadMXBean.getThreadCount());
        
        logger.debug("更新系统指标: instanceId={}, usedMemory={}, maxMemory={}, activeThreads={}", 
            instanceId, usedMemory, maxMemory, threadMXBean.getThreadCount());
    }
    
    /**
     * 更新线程池指标
     */
    public void updateThreadPoolMetrics(String instanceId, ThreadPoolExecutor executor) {
        if (!monitoringEnabled || executor == null) return;
        
        PerformanceMetrics metrics = metricsMap.get(instanceId);
        if (metrics == null) return;
        
        metrics.setActiveThreads(executor.getActiveCount());
        metrics.setMaxThreads(executor.getMaximumPoolSize());
        metrics.setQueueSize(executor.getQueue().size());
        metrics.setCompletedTasks(executor.getCompletedTaskCount());
        
        logger.debug("更新线程池指标: instanceId={}, activeThreads={}, maxThreads={}, queueSize={}, completedTasks={}", 
            instanceId, executor.getActiveCount(), executor.getMaximumPoolSize(), 
            executor.getQueue().size(), executor.getCompletedTaskCount());
    }
    
    /**
     * 获取性能指标
     */
    public PerformanceMetrics getMetrics(String instanceId) {
        return metricsMap.get(instanceId);
    }
    
    /**
     * 获取所有性能指标
     */
    public Map<String, PerformanceMetrics> getAllMetrics() {
        return new HashMap<>(metricsMap);
    }
    
    /**
     * 获取聚合指标
     */
    public Map<String, Object> getAggregatedMetrics() {
        Map<String, Object> aggregated = new HashMap<>();
        
        long totalMessagesSent = 0;
        long totalMessagesConsumed = 0;
        long totalSendErrors = 0;
        long totalConsumeErrors = 0;
        double totalThroughput = 0.0;
        double totalErrorRate = 0.0;
        int instanceCount = 0;
        
        for (PerformanceMetrics metrics : metricsMap.values()) {
            if ("PRODUCER".equals(metrics.getClientType())) {
                totalMessagesSent += metrics.getTotalMessagesSent();
                totalSendErrors += metrics.getTotalSendErrors();
            } else if ("CONSUMER".equals(metrics.getClientType())) {
                totalMessagesConsumed += metrics.getTotalMessagesConsumed();
                totalConsumeErrors += metrics.getTotalConsumeErrors();
            }
            
            totalThroughput += metrics.getMessageThroughput();
            totalErrorRate += metrics.getErrorRate();
            instanceCount++;
        }
        
        aggregated.put("totalMessagesSent", totalMessagesSent);
        aggregated.put("totalMessagesConsumed", totalMessagesConsumed);
        aggregated.put("totalSendErrors", totalSendErrors);
        aggregated.put("totalConsumeErrors", totalConsumeErrors);
        aggregated.put("avgThroughput", instanceCount > 0 ? totalThroughput / instanceCount : 0.0);
        aggregated.put("avgErrorRate", instanceCount > 0 ? totalErrorRate / instanceCount : 0.0);
        aggregated.put("instanceCount", instanceCount);
        aggregated.put("timestamp", LocalDateTime.now());
        
        return aggregated;
    }
    
    /**
     * 重置指标
     */
    public void resetMetrics(String instanceId) {
        PerformanceMetrics metrics = metricsMap.get(instanceId);
        if (metrics != null) {
            metrics.reset();
            logger.info("重置性能指标: instanceId={}", instanceId);
        }
    }
    
    /**
     * 重置所有指标
     */
    public void resetAllMetrics() {
        metricsMap.values().forEach(PerformanceMetrics::reset);
        logger.info("重置所有性能指标");
    }
    
    /**
     * 定期收集指标
     */
    @Scheduled(fixedDelayString = "${app.performance.monitoring.metrics-interval:30000}")
    public void collectMetrics() {
        if (!monitoringEnabled) return;
        
        try {
            logger.debug("开始收集性能指标...");
            
            for (Map.Entry<String, PerformanceMetrics> entry : metricsMap.entrySet()) {
                String instanceId = entry.getKey();
                PerformanceMetrics metrics = entry.getValue();
                
                // 更新时间戳
                metrics.setTimestamp(LocalDateTime.now());
                
                // 更新系统指标
                updateSystemMetrics(instanceId);
                
                // 评估性能等级
                metrics.evaluatePerformanceLevel();
                
                // 存储到Redis
                storeMetricsToRedis(instanceId, metrics);
                
                // 检查告警
                checkAlerts(instanceId, metrics);
            }
            
            logger.debug("性能指标收集完成，共处理{}个实例", metricsMap.size());
        } catch (Exception e) {
            logger.error("收集性能指标时发生错误", e);
        }
    }
    
    /**
     * 存储指标到Redis
     */
    @Async
    public void storeMetricsToRedis(String instanceId, PerformanceMetrics metrics) {
        try {
            // 虚假实现：模拟存储性能指标到Redis
            String key = "performance:metrics:" + instanceId;
            String timeKey = key + ":" + System.currentTimeMillis();
            
            // 模拟存储延迟
            Thread.sleep(10);
            
            logger.debug("虚假实现：性能指标已模拟存储: instanceId={}", instanceId);
        } catch (Exception e) {
            logger.error("模拟存储性能指标失败: instanceId={}", instanceId, e);
        }
    }
    
    /**
     * 从Redis获取历史指标
     */
    public List<PerformanceMetrics> getHistoricalMetrics(String instanceId, int hours) {
        List<PerformanceMetrics> historicalMetrics = new ArrayList<>();
        
        try {
            // 虚假实现：模拟从Redis获取历史指标
            long endTime = System.currentTimeMillis();
            long startTime = endTime - (hours * 60 * 60 * 1000L);
            
            // 模拟生成一些历史数据
            for (int i = 0; i < Math.min(hours, 10); i++) {
                PerformanceMetrics fakeMetrics = new PerformanceMetrics();
                fakeMetrics.setInstanceId(instanceId);
                fakeMetrics.setTimestamp(LocalDateTime.now().minusHours(hours - i));
                // 使用recordMessageSent和recordMessageConsumed方法来设置指标
                for (int j = 0; j < 100 + i * 10; j++) {
                    fakeMetrics.recordMessageSent(1024, 50 + i * 5);
                }
                for (int j = 0; j < 95 + i * 8; j++) {
                    fakeMetrics.recordMessageConsumed(1024, 50 + i * 5);
                }
                historicalMetrics.add(fakeMetrics);
            }
            
            // 按时间排序
            historicalMetrics.sort(Comparator.comparing(PerformanceMetrics::getTimestamp));
            
            logger.debug("虚假实现：已生成{}条历史性能指标", historicalMetrics.size());
            
        } catch (Exception e) {
            logger.error("模拟获取历史性能指标失败: instanceId={}", instanceId, e);
        }
        
        return historicalMetrics;
    }
    
    /**
     * 检查告警
     */
    private void checkAlerts(String instanceId, PerformanceMetrics metrics) {
        if (!alertEnabled) return;
        
        try {
            // 检查吞吐量告警
            double throughput = metrics.getMessageThroughput();
            if (throughput < throughputThreshold) {
                triggerAlert(instanceId, "LOW_THROUGHPUT", 
                    String.format("吞吐量过低: %.2f msg/s (阈值: %.2f msg/s)", throughput, throughputThreshold));
            }
            
            // 检查延迟告警
            long avgLatency = "PRODUCER".equals(metrics.getClientType()) ? 
                metrics.getAvgSendLatency() : metrics.getAvgProcessingTime();
            if (avgLatency > latencyThreshold) {
                triggerAlert(instanceId, "HIGH_LATENCY", 
                    String.format("平均延迟过高: %d ms (阈值: %d ms)", avgLatency, latencyThreshold));
            }
            
            // 检查错误率告警
            double errorRate = metrics.getErrorRate();
            if (errorRate > errorRateThreshold) {
                triggerAlert(instanceId, "HIGH_ERROR_RATE", 
                    String.format("错误率过高: %.2f%% (阈值: %.2f%%)", errorRate * 100, errorRateThreshold * 100));
            }
            
            // 检查内存使用率告警
            double memoryUsage = metrics.getMemoryUsageRate();
            if (memoryUsage > 0.9) {
                triggerAlert(instanceId, "HIGH_MEMORY_USAGE", 
                    String.format("内存使用率过高: %.2f%% (阈值: 90%%)", memoryUsage * 100));
            }
            
        } catch (Exception e) {
            logger.error("检查告警时发生错误: instanceId={}", instanceId, e);
        }
    }
    
    /**
     * 触发告警
     */
    private void triggerAlert(String instanceId, String alertType, String message) {
        String alertKey = instanceId + ":" + alertType;
        
        // 检查告警频率限制
        LocalDateTime lastAlertTime = lastAlertTimes.get(alertKey);
        LocalDateTime now = LocalDateTime.now();
        
        if (lastAlertTime != null && lastAlertTime.plusMinutes(5).isAfter(now)) {
            // 5分钟内不重复告警
            return;
        }
        
        // 记录告警
        alertCounts.computeIfAbsent(alertKey, k -> new AtomicLong(0)).incrementAndGet();
        lastAlertTimes.put(alertKey, now);
        
        logger.warn("性能告警触发: instanceId={}, alertType={}, message={}", instanceId, alertType, message);
        
        // 发送告警通知（可以集成邮件、钉钉、企业微信等）
        sendAlertNotification(instanceId, alertType, message);
    }
    
    /**
     * 发送告警通知
     */
    @Async
    public void sendAlertNotification(String instanceId, String alertType, String message) {
        try {
            // 这里可以集成各种通知方式
            logger.info("发送告警通知: instanceId={}, alertType={}, message={}", instanceId, alertType, message);
            
            // 存储告警记录到Redis
            String alertKey = "performance:alerts:" + instanceId + ":" + System.currentTimeMillis();
            Map<String, Object> alertRecord = new HashMap<>();
            alertRecord.put("instanceId", instanceId);
            alertRecord.put("alertType", alertType);
            alertRecord.put("message", message);
            alertRecord.put("timestamp", LocalDateTime.now());
            
            // 虚假实现：模拟存储告警记录到Redis
            logger.debug("虚假实现：模拟存储告警记录到Redis，键: {}", alertKey);
            
        } catch (Exception e) {
            logger.error("发送告警通知失败: instanceId={}, alertType={}", instanceId, alertType, e);
        }
    }
    
    /**
     * 获取告警统计
     */
    public Map<String, Object> getAlertStatistics() {
        Map<String, Object> stats = new HashMap<>();
        
        long totalAlerts = alertCounts.values().stream().mapToLong(AtomicLong::get).sum();
        stats.put("totalAlerts", totalAlerts);
        stats.put("alertCounts", new HashMap<>(alertCounts));
        stats.put("lastAlertTimes", new HashMap<>(lastAlertTimes));
        
        return stats;
    }
    
    // Micrometer指标获取方法
    private double getCurrentThroughput() {
        return metricsMap.values().stream()
            .mapToDouble(PerformanceMetrics::getMessageThroughput)
            .average()
            .orElse(0.0);
    }
    
    private double getCurrentErrorRate() {
        return metricsMap.values().stream()
            .mapToDouble(PerformanceMetrics::getErrorRate)
            .average()
            .orElse(0.0);
    }
    
    private double getMemoryUsage() {
        long used = memoryMXBean.getHeapMemoryUsage().getUsed();
        long max = memoryMXBean.getHeapMemoryUsage().getMax();
        return max > 0 ? (double) used / max : 0.0;
    }
}