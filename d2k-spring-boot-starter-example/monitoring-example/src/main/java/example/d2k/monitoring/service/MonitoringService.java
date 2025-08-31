package example.d2k.monitoring.service;

import example.d2k.monitoring.alert.AlertService;
import example.d2k.monitoring.health.HealthCheckService;
import example.d2k.monitoring.metrics.MonitoringMetrics;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
// Redis相关导入已移除，使用虚假实现
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.sql.DataSource;
import java.lang.management.*;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 监控服务
 * 负责收集、存储和分析各种监控指标
 * 
 * @author xiajuan96
 */
@Service
public class MonitoringService {

    private static final Logger logger = LoggerFactory.getLogger(MonitoringService.class);

    @Autowired
    private MeterRegistry meterRegistry;

    // Redis模板依赖已移除，使用虚假实现

    @Autowired
    private HealthCheckService healthCheckService;

    @Autowired
    private AlertService alertService;

    @Autowired
    private DataSource dataSource;

    @PersistenceContext
    private EntityManager entityManager;

    // 配置参数
    @Value("${spring.application.name:monitoring-example}")
    private String applicationName;

    @Value("${app.monitoring.instance-id:}")
    private String instanceId;

    @Value("${app.monitoring.node-id:}")
    private String nodeId;

    @Value("${app.monitoring.environment:dev}")
    private String environment;

    @Value("${app.monitoring.metrics.collection-interval:60}")
    private long metricsCollectionInterval;

    @Value("${app.monitoring.metrics.retention-hours:24}")
    private int metricsRetentionHours;

    // JMX Bean
    private final MemoryMXBean memoryMXBean = ManagementFactory.getMemoryMXBean();
    private final ThreadMXBean threadMXBean = ManagementFactory.getThreadMXBean();
    private final RuntimeMXBean runtimeMXBean = ManagementFactory.getRuntimeMXBean();
    private final OperatingSystemMXBean osMXBean = ManagementFactory.getOperatingSystemMXBean();

    // Micrometer指标
    private Counter kafkaMessagesSentCounter;
    private Counter kafkaMessagesReceivedCounter;
    private Counter kafkaSendErrorsCounter;
    private Counter kafkaConsumeErrorsCounter;
    private Timer kafkaSendLatencyTimer;
    private Timer kafkaConsumeLatencyTimer;
    private Counter httpRequestsCounter;
    private Timer httpResponseTimeTimer;
    private Counter cacheHitsCounter;
    private Counter cacheMissesCounter;

    // 内存中的指标缓存
    private final Map<String, AtomicLong> metricsCache = new ConcurrentHashMap<>();
    private final Map<String, Double> gaugeCache = new ConcurrentHashMap<>();

    @PostConstruct
    public void init() {
        // 生成实例ID
        if (instanceId == null || instanceId.isEmpty()) {
            instanceId = applicationName + "-" + UUID.randomUUID().toString().substring(0, 8);
        }
        
        if (nodeId == null || nodeId.isEmpty()) {
            nodeId = getHostname();
        }

        // 初始化Micrometer指标
        initializeMetrics();

        // 注册JVM指标
        registerJvmMetrics();

        logger.info("监控服务初始化完成 - 实例ID: {}, 节点ID: {}, 环境: {}", instanceId, nodeId, environment);
    }

    /**
     * 初始化Micrometer指标
     */
    private void initializeMetrics() {
        // Kafka指标
        kafkaMessagesSentCounter = Counter.builder("kafka.messages.sent")
                .description("Kafka消息发送总数")
                .register(meterRegistry);

        kafkaMessagesReceivedCounter = Counter.builder("kafka.messages.received")
                .description("Kafka消息接收总数")
                .register(meterRegistry);

        kafkaSendErrorsCounter = Counter.builder("kafka.send.errors")
                .description("Kafka发送错误总数")
                .register(meterRegistry);

        kafkaConsumeErrorsCounter = Counter.builder("kafka.consume.errors")
                .description("Kafka消费错误总数")
                .register(meterRegistry);

        kafkaSendLatencyTimer = Timer.builder("kafka.send.latency")
                .description("Kafka发送延迟")
                .register(meterRegistry);

        kafkaConsumeLatencyTimer = Timer.builder("kafka.consume.latency")
                .description("Kafka消费延迟")
                .register(meterRegistry);

        // HTTP指标
        httpRequestsCounter = Counter.builder("http.requests")
                .description("HTTP请求总数")
                .register(meterRegistry);

        httpResponseTimeTimer = Timer.builder("http.response.time")
                .description("HTTP响应时间")
                .register(meterRegistry);

        // 缓存指标
        cacheHitsCounter = Counter.builder("cache.hits")
                .description("缓存命中总数")
                .register(meterRegistry);

        cacheMissesCounter = Counter.builder("cache.misses")
                .description("缓存未命中总数")
                .register(meterRegistry);
    }

    /**
     * 注册JVM指标
     */
    private void registerJvmMetrics() {
        // 内存使用率
        Gauge.builder("jvm.memory.heap.usage", this, MonitoringService::getHeapMemoryUsage)
                .description("JVM堆内存使用率")
                .register(meterRegistry);

        Gauge.builder("jvm.memory.nonheap.usage", this, MonitoringService::getNonHeapMemoryUsage)
                .description("JVM非堆内存使用率")
                .register(meterRegistry);

        // 线程数
        Gauge.builder("jvm.threads.count", this, MonitoringService::getThreadCount)
                .description("JVM线程数")
                .register(meterRegistry);

        // CPU使用率
        Gauge.builder("system.cpu.usage", this, MonitoringService::getCpuUsage)
                .description("系统CPU使用率")
                .register(meterRegistry);
    }

    /**
     * 定期收集监控指标
     */
    @Scheduled(fixedDelayString = "${app.monitoring.metrics.collection-interval:60}000")
    public void collectMetrics() {
        try {
            logger.debug("开始收集监控指标");

            MonitoringMetrics metrics = createMetricsSnapshot();
            
            // 存储指标到数据库
            saveMetricsToDatabase(metrics);
            
            // 存储指标到Redis
            saveMetricsToRedis(metrics);
            
            // 检查告警
            alertService.checkMetricsAndAlert(metrics);
            
            // 清理过期指标
            cleanupExpiredMetrics();
            
            logger.debug("监控指标收集完成");

        } catch (Exception e) {
            logger.error("监控指标收集失败", e);
        }
    }

    /**
     * 创建指标快照
     */
    public MonitoringMetrics createMetricsSnapshot() {
        MonitoringMetrics metrics = new MonitoringMetrics(instanceId);
        
        // 基本信息
        metrics.setNodeId(nodeId);
        metrics.setApplicationName(applicationName);
        metrics.setEnvironment(environment);
        metrics.setTimestamp(LocalDateTime.now());

        // Kafka指标
        metrics.setKafkaMessagesSent(getCounterValue(kafkaMessagesSentCounter));
        metrics.setKafkaMessagesReceived(getCounterValue(kafkaMessagesReceivedCounter));
        metrics.setKafkaSendErrors(getCounterValue(kafkaSendErrorsCounter));
        metrics.setKafkaConsumeErrors(getCounterValue(kafkaConsumeErrorsCounter));
        metrics.setKafkaAvgSendLatency(getTimerMean(kafkaSendLatencyTimer));
        metrics.setKafkaAvgConsumeLatency(getTimerMean(kafkaConsumeLatencyTimer));
        metrics.setKafkaMaxSendLatency(getTimerMax(kafkaSendLatencyTimer));
        metrics.setKafkaMaxConsumeLatency(getTimerMax(kafkaConsumeLatencyTimer));

        // 系统指标
        metrics.setCpuUsage(getCpuUsage());
        
        // 内存指标
        MemoryUsage heapMemory = memoryMXBean.getHeapMemoryUsage();
        metrics.setHeapUsed(heapMemory.getUsed());
        metrics.setHeapTotal(heapMemory.getMax());
        metrics.setHeapUsagePercent((double) heapMemory.getUsed() / heapMemory.getMax());
        
        MemoryUsage nonHeapMemory = memoryMXBean.getNonHeapMemoryUsage();
        metrics.setNonHeapUsed(nonHeapMemory.getUsed());
        metrics.setNonHeapTotal(nonHeapMemory.getMax() > 0 ? nonHeapMemory.getMax() : nonHeapMemory.getCommitted());
        
        // 总内存
        long totalMemory = heapMemory.getUsed() + nonHeapMemory.getUsed();
        long maxMemory = heapMemory.getMax() + (nonHeapMemory.getMax() > 0 ? nonHeapMemory.getMax() : nonHeapMemory.getCommitted());
        metrics.setMemoryUsed(totalMemory);
        metrics.setMemoryTotal(maxMemory);
        metrics.setMemoryUsagePercent((double) totalMemory / maxMemory);

        // 线程指标
        metrics.setThreadCount(threadMXBean.getThreadCount());
        metrics.setDaemonThreadCount(threadMXBean.getDaemonThreadCount());
        metrics.setPeakThreadCount(threadMXBean.getPeakThreadCount());
        metrics.setTotalStartedThreadCount(threadMXBean.getTotalStartedThreadCount());

        // HTTP指标
        metrics.setHttpRequestsTotal(getCounterValue(httpRequestsCounter));
        metrics.setHttpAvgResponseTime(getTimerMean(httpResponseTimeTimer));
        metrics.setHttpMaxResponseTime(getTimerMax(httpResponseTimeTimer));

        // 缓存指标
        long cacheHits = getCounterValue(cacheHitsCounter);
        long cacheMisses = getCounterValue(cacheMissesCounter);
        metrics.setCacheHits(cacheHits);
        metrics.setCacheMisses(cacheMisses);
        metrics.setCacheHitRate(cacheHits + cacheMisses > 0 ? (double) cacheHits / (cacheHits + cacheMisses) : 0.0);

        // 连接池指标
        metrics.setDbActiveConnections(getDatabaseActiveConnections());
        metrics.setRedisActiveConnections(getRedisActiveConnections());

        // 健康状态
        metrics.setHealthStatus(healthCheckService.getOverallHealthStatus());
        
        // 告警信息
        List<AlertService.Alert> activeAlerts = alertService.getActiveAlerts();
        metrics.setActiveAlerts(activeAlerts.size());
        if (!activeAlerts.isEmpty()) {
            AlertService.Alert highestAlert = activeAlerts.stream()
                    .max(Comparator.comparing(alert -> alert.getLevel().ordinal()))
                    .orElse(null);
            if (highestAlert != null) {
                switch (highestAlert.getLevel()) {
                    case CRITICAL:
                        metrics.setAlertLevel(MonitoringMetrics.AlertLevel.CRITICAL);
                        break;
                    case ERROR:
                        metrics.setAlertLevel(MonitoringMetrics.AlertLevel.ERROR);
                        break;
                    case WARNING:
                        metrics.setAlertLevel(MonitoringMetrics.AlertLevel.WARNING);
                        break;
                    default:
                        metrics.setAlertLevel(MonitoringMetrics.AlertLevel.INFO);
                        break;
                }
            }
        }

        return metrics;
    }

    /**
     * 记录Kafka消息发送
     */
    public void recordKafkaMessageSent(String topic, long latency) {
        kafkaMessagesSentCounter.increment();
        kafkaSendLatencyTimer.record(latency, TimeUnit.MILLISECONDS);
        incrementMetric("kafka.messages.sent." + topic);
    }

    /**
     * 记录Kafka消息接收
     */
    public void recordKafkaMessageReceived(String topic, long latency) {
        kafkaMessagesReceivedCounter.increment();
        kafkaConsumeLatencyTimer.record(latency, TimeUnit.MILLISECONDS);
        incrementMetric("kafka.messages.received." + topic);
    }

    /**
     * 记录Kafka发送错误
     */
    public void recordKafkaSendError(String topic, String errorType) {
        kafkaSendErrorsCounter.increment();
        incrementMetric("kafka.send.errors." + topic);
        incrementMetric("kafka.send.errors.type." + errorType);
    }

    /**
     * 记录Kafka消费错误
     */
    public void recordKafkaConsumeError(String topic, String errorType) {
        kafkaConsumeErrorsCounter.increment();
        incrementMetric("kafka.consume.errors." + topic);
        incrementMetric("kafka.consume.errors.type." + errorType);
    }

    /**
     * 记录HTTP请求
     */
    public void recordHttpRequest(String method, String uri, int statusCode, long responseTime) {
        httpRequestsCounter.increment();
        httpResponseTimeTimer.record(responseTime, TimeUnit.MILLISECONDS);
        
        incrementMetric("http.requests." + method.toLowerCase());
        incrementMetric("http.requests.status." + (statusCode / 100) + "xx");
        
        if (statusCode >= 200 && statusCode < 300) {
            incrementMetric("http.requests.success");
        } else {
            incrementMetric("http.requests.error");
        }
    }

    /**
     * 记录缓存命中
     */
    public void recordCacheHit(String cacheName) {
        cacheHitsCounter.increment();
        incrementMetric("cache.hits." + cacheName);
    }

    /**
     * 记录缓存未命中
     */
    public void recordCacheMiss(String cacheName) {
        cacheMissesCounter.increment();
        incrementMetric("cache.misses." + cacheName);
    }

    /**
     * 获取指定实例的监控指标
     */
    public MonitoringMetrics getMetrics(String instanceId) {
        if (instanceId == null || instanceId.equals(this.instanceId)) {
            return createMetricsSnapshot();
        }
        
        // 虚假实现：模拟从Redis获取其他实例的指标
        try {
            logger.info("Mock: 从Redis获取监控指标 - instanceId: {}", instanceId);
            // 返回null表示未找到其他实例的指标
        } catch (Exception e) {
            logger.error("虚假从Redis获取监控指标失败: {}", instanceId, e);
        }
        
        return null;
    }

    /**
     * 获取历史监控指标
     */
    public List<MonitoringMetrics> getHistoricalMetrics(String instanceId, int hours) {
        try {
            String jpql = "SELECT m FROM MonitoringMetrics m WHERE m.instanceId = :instanceId " +
                         "AND m.timestamp >= :startTime ORDER BY m.timestamp DESC";
            
            LocalDateTime startTime = LocalDateTime.now().minusHours(hours);
            
            return entityManager.createQuery(jpql, MonitoringMetrics.class)
                    .setParameter("instanceId", instanceId)
                    .setParameter("startTime", startTime)
                    .setMaxResults(1000) // 限制结果数量
                    .getResultList();
                    
        } catch (Exception e) {
            logger.error("获取历史监控指标失败", e);
            return new ArrayList<>();
        }
    }

    /**
     * 重置指标
     */
    public void resetMetrics(String instanceId) {
        if (instanceId == null || instanceId.equals(this.instanceId)) {
            // 重置本实例的指标
            metricsCache.clear();
            gaugeCache.clear();
            
            // 重置Micrometer计数器（注意：Micrometer的Counter不支持重置，这里只是清理缓存）
            logger.info("本实例监控指标已重置");
        }
        
        // 虚假实现：模拟从Redis删除指定实例的指标
        try {
            logger.info("Mock: 清除Redis监控指标 - instanceId: {}", instanceId);
        } catch (Exception e) {
            logger.error("虚假清除Redis监控指标失败: {}", instanceId, e);
        }
    }

    /**
     * 保存指标到数据库
     */
    private void saveMetricsToDatabase(MonitoringMetrics metrics) {
        try {
            entityManager.persist(metrics);
            entityManager.flush();
        } catch (Exception e) {
            logger.error("保存监控指标到数据库失败", e);
        }
    }

    /**
     * 虚假的保存指标到Redis（原Redis保存方法）
     */
    private void saveMetricsToRedis(MonitoringMetrics metrics) {
        try {
            // 虚假实现：模拟保存指标到Redis
            logger.info("Mock: 保存监控指标到Redis - instanceId: {}, timestamp: {}", instanceId, metrics.getTimestamp());
            
        } catch (Exception e) {
            logger.error("虚假保存监控指标到Redis失败", e);
        }
    }

    /**
     * 清理过期指标
     */
    @Scheduled(fixedDelay = 3600000) // 每小时执行一次
    public void cleanupExpiredMetrics() {
        try {
            LocalDateTime cutoffTime = LocalDateTime.now().minusHours(metricsRetentionHours);
            
            String jpql = "DELETE FROM MonitoringMetrics m WHERE m.timestamp < :cutoffTime";
            int deletedCount = entityManager.createQuery(jpql)
                    .setParameter("cutoffTime", cutoffTime)
                    .executeUpdate();
                    
            if (deletedCount > 0) {
                logger.info("清理了 {} 条过期监控指标", deletedCount);
            }
            
        } catch (Exception e) {
            logger.error("清理过期监控指标失败", e);
        }
    }

    // 辅助方法
    private void incrementMetric(String key) {
        metricsCache.computeIfAbsent(key, k -> new AtomicLong(0)).incrementAndGet();
    }

    private long getCounterValue(Counter counter) {
        return counter != null ? (long) counter.count() : 0L;
    }

    private double getTimerMean(Timer timer) {
        return timer != null ? timer.mean(TimeUnit.MILLISECONDS) : 0.0;
    }

    private double getTimerMax(Timer timer) {
        return timer != null ? timer.max(TimeUnit.MILLISECONDS) : 0.0;
    }

    private double getHeapMemoryUsage() {
        MemoryUsage heapMemory = memoryMXBean.getHeapMemoryUsage();
        return (double) heapMemory.getUsed() / heapMemory.getMax();
    }

    private double getNonHeapMemoryUsage() {
        MemoryUsage nonHeapMemory = memoryMXBean.getNonHeapMemoryUsage();
        long max = nonHeapMemory.getMax() > 0 ? nonHeapMemory.getMax() : nonHeapMemory.getCommitted();
        return (double) nonHeapMemory.getUsed() / max;
    }

    private double getThreadCount() {
        return threadMXBean.getThreadCount();
    }

    private double getCpuUsage() {
        try {
            if (osMXBean instanceof com.sun.management.OperatingSystemMXBean) {
                return ((com.sun.management.OperatingSystemMXBean) osMXBean).getProcessCpuLoad();
            }
        } catch (Exception e) {
            logger.debug("无法获取CPU使用率", e);
        }
        return 0.0;
    }

    private int getDatabaseActiveConnections() {
        try {
            // 这里需要根据实际使用的连接池实现来获取活跃连接数
            // 例如HikariCP、Tomcat JDBC等
            return 0; // 占位符
        } catch (Exception e) {
            logger.debug("无法获取数据库连接数", e);
            return 0;
        }
    }

    private int getRedisActiveConnections() {
        try {
            // 虚假实现：模拟Redis活跃连接数
            return 5;
        } catch (Exception e) {
            logger.debug("获取虚假Redis活跃连接数失败", e);
            return 0;
        }
    }

    private String getHostname() {
        try {
            return java.net.InetAddress.getLocalHost().getHostName();
        } catch (Exception e) {
            return "unknown";
        }
    }

    // Getters
    public String getInstanceId() {
        return instanceId;
    }

    public String getNodeId() {
        return nodeId;
    }

    public String getApplicationName() {
        return applicationName;
    }

    public String getEnvironment() {
        return environment;
    }
}