package example.d2k.monitoring.controller;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.stats.CacheStats;
import example.d2k.monitoring.alert.AlertService;
import example.d2k.monitoring.aspect.MonitoringAspect;
import example.d2k.monitoring.health.HealthCheckService;
import example.d2k.monitoring.metrics.MonitoringMetrics;
import example.d2k.monitoring.service.MonitoringService;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.actuate.health.Health;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 监控控制器
 * 提供监控相关的API接口
 * 
 * @author xiajuan96
 */
@RestController
@RequestMapping("/api/monitoring")
public class MonitoringController {

    private static final Logger logger = LoggerFactory.getLogger(MonitoringController.class);

    @Autowired
    private MonitoringService monitoringService;

    @Autowired
    private HealthCheckService healthCheckService;

    @Autowired
    private AlertService alertService;

    @Autowired
    private MeterRegistry meterRegistry;

    // Redis和Kafka依赖已移除，使用虚假方法替代

    @Autowired
    @Qualifier("monitoringCache")
    private Cache<String, Object> monitoringCache;

    @Autowired
    @Qualifier("metricsCache")
    private Cache<String, Object> metricsCache;

    @Autowired
    @Qualifier("alertCache")
    private Cache<String, Object> alertCache;

    @Autowired
    @Qualifier("healthCheckCache")
    private Cache<String, Object> healthCheckCache;

    /**
     * 获取监控概览
     */
    @GetMapping("/overview")
    public ResponseEntity<Map<String, Object>> getMonitoringOverview() {
        try {
            Map<String, Object> overview = new HashMap<>();
            
            // 基本信息
            overview.put("instanceId", monitoringService.getInstanceId());
            overview.put("nodeId", monitoringService.getNodeId());
            overview.put("applicationName", monitoringService.getApplicationName());
            overview.put("environment", monitoringService.getEnvironment());
            overview.put("timestamp", LocalDateTime.now());
            
            // 当前指标
            MonitoringMetrics currentMetrics = monitoringService.createMetricsSnapshot();
            overview.put("metrics", currentMetrics);
            
            // 健康状态
            Health health = healthCheckService.health();
            overview.put("health", health);
            
            // 活跃告警
            List<AlertService.Alert> activeAlerts = alertService.getActiveAlerts();
            overview.put("activeAlerts", activeAlerts);
            overview.put("alertCount", activeAlerts.size());
            
            // 缓存统计
            Map<String, Object> cacheStats = new HashMap<>();
            cacheStats.put("monitoring", getCacheStatsMap(monitoringCache.stats()));
            cacheStats.put("metrics", getCacheStatsMap(metricsCache.stats()));
            cacheStats.put("alert", getCacheStatsMap(alertCache.stats()));
            cacheStats.put("healthCheck", getCacheStatsMap(healthCheckCache.stats()));
            overview.put("cacheStats", cacheStats);
            
            return ResponseEntity.ok(overview);
            
        } catch (Exception e) {
            logger.error("获取监控概览失败", e);
            Map<String, Object> errorMap = new HashMap<>();
            errorMap.put("error", e.getMessage());
            return ResponseEntity.internalServerError().body(errorMap);
        }
    }

    /**
     * 获取指定实例的监控指标
     */
    @GetMapping("/metrics")
    public ResponseEntity<MonitoringMetrics> getMetrics(
            @RequestParam(required = false) String instanceId) {
        try {
            MonitoringMetrics metrics = monitoringService.getMetrics(instanceId);
            if (metrics != null) {
                return ResponseEntity.ok(metrics);
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            logger.error("获取监控指标失败", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * 获取历史监控指标
     */
    @GetMapping("/metrics/history")
    public ResponseEntity<List<MonitoringMetrics>> getHistoricalMetrics(
            @RequestParam(required = false) String instanceId,
            @RequestParam(defaultValue = "24") int hours) {
        try {
            String targetInstanceId = instanceId != null ? instanceId : monitoringService.getInstanceId();
            List<MonitoringMetrics> metrics = monitoringService.getHistoricalMetrics(targetInstanceId, hours);
            return ResponseEntity.ok(metrics);
        } catch (Exception e) {
            logger.error("获取历史监控指标失败", e);
            return ResponseEntity.internalServerError().body(new ArrayList<>());
        }
    }

    /**
     * 重置监控指标
     */
    @PostMapping("/metrics/reset")
    public ResponseEntity<Map<String, String>> resetMetrics(
            @RequestParam(required = false) String instanceId) {
        try {
            String targetInstanceId = instanceId != null ? instanceId : monitoringService.getInstanceId();
            monitoringService.resetMetrics(targetInstanceId);
            
            Map<String, String> result = new HashMap<>();
            result.put("message", "监控指标已重置");
            result.put("instanceId", targetInstanceId);
            result.put("timestamp", LocalDateTime.now().toString());
            
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            logger.error("重置监控指标失败", e);
            Map<String, String> errorMap = new HashMap<>();
            errorMap.put("error", e.getMessage());
            return ResponseEntity.internalServerError().body(errorMap);
        }
    }

    /**
     * 健康检查
     */
    @GetMapping("/health")
    public ResponseEntity<Health> getHealth() {
        try {
            Health health = healthCheckService.health();
            return ResponseEntity.ok(health);
        } catch (Exception e) {
            logger.error("健康检查失败", e);
            return ResponseEntity.internalServerError().body(
                Health.down().withDetail("error", e.getMessage()).build()
            );
        }
    }

    /**
     * 获取活跃告警
     */
    @GetMapping("/alerts")
    public ResponseEntity<List<AlertService.Alert>> getActiveAlerts() {
        try {
            List<AlertService.Alert> alerts = alertService.getActiveAlerts();
            return ResponseEntity.ok(alerts);
        } catch (Exception e) {
            logger.error("获取活跃告警失败", e);
            return ResponseEntity.internalServerError().body(new ArrayList<>());
        }
    }

    /**
     * 获取告警统计
     */
    @GetMapping("/alerts/stats")
    public ResponseEntity<Map<String, Object>> getAlertStats() {
        try {
            Map<String, Object> stats = alertService.getAlertStatistics();
            return ResponseEntity.ok(stats);
        } catch (Exception e) {
            logger.error("获取告警统计失败", e);
            return ResponseEntity.internalServerError().body(new HashMap<>());
        }
    }

    /**
     * 手动触发测试告警
     */
    @PostMapping("/alerts/test")
    public ResponseEntity<Map<String, String>> triggerTestAlert(
            @RequestParam(required = false, defaultValue = "test-instance") String instanceId) {
        try {
            alertService.triggerTestAlert(instanceId);
            
            Map<String, String> result = new HashMap<>();
            result.put("message", "测试告警已触发");
            result.put("instanceId", instanceId);
            result.put("timestamp", LocalDateTime.now().toString());
            
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            logger.error("触发测试告警失败", e);
            Map<String, String> errorMap = new HashMap<>();
            errorMap.put("error", e.getMessage());
            return ResponseEntity.internalServerError().body(errorMap);
        }
    }

    /**
     * 获取缓存统计信息
     */
    @GetMapping("/cache/stats")
    public ResponseEntity<Map<String, Object>> getCacheStats() {
        try {
            Map<String, Object> stats = new HashMap<>();
            stats.put("monitoring", getCacheStatsMap(monitoringCache.stats()));
            stats.put("metrics", getCacheStatsMap(metricsCache.stats()));
            stats.put("alert", getCacheStatsMap(alertCache.stats()));
            stats.put("healthCheck", getCacheStatsMap(healthCheckCache.stats()));
            
            return ResponseEntity.ok(stats);
        } catch (Exception e) {
            logger.error("获取缓存统计失败", e);
            return ResponseEntity.internalServerError().body(new HashMap<>());
        }
    }

    /**
     * 清理缓存
     */
    @PostMapping("/cache/clear")
    public ResponseEntity<Map<String, String>> clearCache(
            @RequestParam(required = false) String cacheName) {
        try {
            if (cacheName == null || cacheName.isEmpty()) {
                // 清理所有缓存
                monitoringCache.invalidateAll();
                metricsCache.invalidateAll();
                alertCache.invalidateAll();
                healthCheckCache.invalidateAll();
                
                Map<String, String> response = new HashMap<>();
                response.put("message", "所有缓存已清理");
                response.put("timestamp", LocalDateTime.now().toString());
                return ResponseEntity.ok(response);
            } else {
                // 清理指定缓存
                switch (cacheName.toLowerCase()) {
                    case "monitoring":
                        monitoringCache.invalidateAll();
                        break;
                    case "metrics":
                        metricsCache.invalidateAll();
                        break;
                    case "alert":
                        alertCache.invalidateAll();
                        break;
                    case "healthcheck":
                        healthCheckCache.invalidateAll();
                        break;
                    default:
                        Map<String, String> errorResponse = new HashMap<>();
                errorResponse.put("error", "未知的缓存名称: " + cacheName);
                return ResponseEntity.badRequest().body(errorResponse);
                }
                
                Map<String, String> response = new HashMap<>();
                response.put("message", "缓存 " + cacheName + " 已清理");
                response.put("timestamp", LocalDateTime.now().toString());
                return ResponseEntity.ok(response);
            }
        } catch (Exception e) {
            logger.error("清理缓存失败", e);
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("error", e.getMessage());
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }

    /**
     * 虚假的Kafka消息发送测试（原Kafka发送）
     */
    @PostMapping("/test/kafka/send")
    @MonitoringAspect.Monitor(value = "kafka-send-test", slowThreshold = 2000)
    public ResponseEntity<Map<String, Object>> testKafkaSend(
            @RequestParam(defaultValue = "test-topic") String topic,
            @RequestParam(defaultValue = "10") int count,
            @RequestParam(defaultValue = "false") boolean async) {
        
        try {
            long startTime = System.currentTimeMillis();
            
            // 虚假实现：模拟Kafka消息发送
            for (int i = 0; i < count; i++) {
                String message = "测试消息-" + i + "-" + System.currentTimeMillis();
                
                // 模拟发送延迟
                Thread.sleep(ThreadLocalRandom.current().nextInt(10, 50));
                
                // 记录虚假的发送指标
                long sendTime = ThreadLocalRandom.current().nextInt(5, 100);
                monitoringService.recordKafkaMessageSent(topic, sendTime);
                
                logger.info("Mock: Kafka message sent - Topic: {}, Key: key-{}, Message: {}", 
                           topic, i, message);
            }
            
            long totalTime = System.currentTimeMillis() - startTime;
            
            Map<String, Object> result = new HashMap<>();
            result.put("message", "虚假Kafka消息发送测试完成");
            result.put("topic", topic);
            result.put("count", count);
            result.put("async", async);
            result.put("totalTime", totalTime + "ms");
            result.put("avgTime", (totalTime / count) + "ms");
            result.put("timestamp", LocalDateTime.now());
            result.put("note", "这是虚假实现，未实际发送到Kafka");
            
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            logger.error("虚假Kafka发送测试失败", e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", e.getMessage());
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }

    /**
     * 虚假的Redis操作测试（原Redis操作）
     */
    @PostMapping("/test/redis")
    @MonitoringAspect.Monitor(value = "redis-test", slowThreshold = 1000)
    public ResponseEntity<Map<String, Object>> testRedis(
            @RequestParam(defaultValue = "100") int count) {
        try {
            long startTime = System.currentTimeMillis();
            
            // 虚假写入测试
            long writeStart = System.currentTimeMillis();
            for (int i = 0; i < count; i++) {
                String key = "test:key:" + i;
                String value = "test-value-" + i + "-" + System.currentTimeMillis();
                
                // 模拟Redis写入延迟
                Thread.sleep(ThreadLocalRandom.current().nextInt(1, 10));
                
                logger.debug("Mock: Redis SET - Key: {}, Value: {}", key, value);
            }
            long writeTime = System.currentTimeMillis() - writeStart;
            
            // 虚假读取测试
            long readStart = System.currentTimeMillis();
            int successCount = count; // 虚假实现：假设全部成功
            for (int i = 0; i < count; i++) {
                String key = "test:key:" + i;
                
                // 模拟Redis读取延迟
                Thread.sleep(ThreadLocalRandom.current().nextInt(1, 5));
                
                logger.debug("Mock: Redis GET - Key: {}", key);
            }
            long readTime = System.currentTimeMillis() - readStart;
            
            long totalTime = System.currentTimeMillis() - startTime;
            
            Map<String, Object> result = new HashMap<>();
            result.put("message", "虚假Redis性能测试完成");
            result.put("count", count);
            result.put("writeTime", writeTime + "ms");
            result.put("readTime", readTime + "ms");
            result.put("totalTime", totalTime + "ms");
            result.put("successCount", successCount);
            result.put("avgWriteTime", (writeTime / count) + "ms");
            result.put("avgReadTime", (readTime / count) + "ms");
            result.put("timestamp", LocalDateTime.now());
            result.put("note", "这是虚假实现，未实际操作Redis");
            
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            logger.error("虚假Redis测试失败", e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", e.getMessage());
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }

    /**
     * 性能测试 - 缓存操作
     */
    @PostMapping("/test/cache")
    @MonitoringAspect.Monitor(value = "cache-test", slowThreshold = 500)
    public ResponseEntity<Map<String, Object>> testCache(
            @RequestParam(defaultValue = "1000") int count) {
        try {
            long startTime = System.currentTimeMillis();
            
            // 写入测试
            long writeStart = System.currentTimeMillis();
            for (int i = 0; i < count; i++) {
                String key = "cache-test-" + i;
                String value = "cache-value-" + i;
                monitoringCache.put(key, value);
            }
            long writeTime = System.currentTimeMillis() - writeStart;
            
            // 读取测试
            long readStart = System.currentTimeMillis();
            int hitCount = 0;
            int missCount = 0;
            for (int i = 0; i < count; i++) {
                String key = "cache-test-" + i;
                Object value = monitoringCache.getIfPresent(key);
                if (value != null) {
                    hitCount++;
                    monitoringService.recordCacheHit("monitoring");
                } else {
                    missCount++;
                    monitoringService.recordCacheMiss("monitoring");
                }
            }
            long readTime = System.currentTimeMillis() - readStart;
            
            // 清理测试数据
            for (int i = 0; i < count; i++) {
                String key = "cache-test-" + i;
                monitoringCache.invalidate(key);
            }
            
            long totalTime = System.currentTimeMillis() - startTime;
            
            Map<String, Object> result = new HashMap<>();
            result.put("message", "缓存性能测试完成");
            result.put("count", count);
            result.put("writeTime", writeTime + "ms");
            result.put("readTime", readTime + "ms");
            result.put("totalTime", totalTime + "ms");
            result.put("hitCount", hitCount);
            result.put("missCount", missCount);
            result.put("hitRate", hitCount > 0 ? (double) hitCount / (hitCount + missCount) : 0.0);
            result.put("avgWriteTime", (writeTime / count) + "ms");
            result.put("avgReadTime", (readTime / count) + "ms");
            result.put("timestamp", LocalDateTime.now());
            
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            logger.error("缓存测试失败", e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", e.getMessage());
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }

    /**
     * 获取Micrometer指标
     */
    @GetMapping("/micrometer")
    public ResponseEntity<Map<String, Object>> getMicrometerMetrics() {
        try {
            Map<String, Object> metrics = new HashMap<>();
            
            meterRegistry.getMeters().forEach(meter -> {
                String name = meter.getId().getName();
                Object value = null;
                
                switch (meter.getId().getType()) {
                    case COUNTER:
                        value = ((io.micrometer.core.instrument.Counter) meter).count();
                        break;
                    case GAUGE:
                        value = ((io.micrometer.core.instrument.Gauge) meter).value();
                        break;
                    case TIMER:
                        io.micrometer.core.instrument.Timer timer = (io.micrometer.core.instrument.Timer) meter;
                        Map<String, Object> timerStats = new HashMap<>();
                        timerStats.put("count", timer.count());
                        timerStats.put("totalTime", timer.totalTime(java.util.concurrent.TimeUnit.MILLISECONDS));
                        timerStats.put("mean", timer.mean(java.util.concurrent.TimeUnit.MILLISECONDS));
                        timerStats.put("max", timer.max(java.util.concurrent.TimeUnit.MILLISECONDS));
                        value = timerStats;
                        break;
                    default:
                        value = "N/A";
                        break;
                }
                
                metrics.put(name, value);
            });
            
            return ResponseEntity.ok(metrics);
            
        } catch (Exception e) {
            logger.error("获取Micrometer指标失败", e);
            return ResponseEntity.internalServerError().body(new HashMap<>());
        }
    }

    /**
     * 转换缓存统计信息为Map
     */
    private Map<String, Object> getCacheStatsMap(CacheStats stats) {
        Map<String, Object> statsMap = new HashMap<>();
        statsMap.put("requestCount", stats.requestCount());
        statsMap.put("hitCount", stats.hitCount());
        statsMap.put("hitRate", stats.hitRate());
        statsMap.put("missCount", stats.missCount());
        statsMap.put("missRate", stats.missRate());
        statsMap.put("loadCount", stats.loadCount());
        // loadExceptionCount和loadExceptionRate在某些版本的Caffeine中不可用
        // statsMap.put("loadExceptionCount", stats.loadExceptionCount());
        // statsMap.put("loadExceptionRate", stats.loadExceptionRate());
        statsMap.put("totalLoadTime", stats.totalLoadTime());
        statsMap.put("averageLoadPenalty", stats.averageLoadPenalty());
        statsMap.put("evictionCount", stats.evictionCount());
        return statsMap;
    }
}