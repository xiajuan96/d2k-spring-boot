package example.d2k.performance.optimization.controller;

import com.github.benmanes.caffeine.cache.Cache;
import example.d2k.performance.optimization.aspect.PerformanceMonitoringAspect;
import example.d2k.performance.optimization.metrics.PerformanceMetrics;
import example.d2k.performance.optimization.service.PerformanceMonitoringService;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
// Redis和Kafka相关导入已移除，使用虚假实现
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * 性能优化控制器
 * 提供性能监控、测试和优化相关的API接口
 * @author xiajuan96
 */
@RestController
@RequestMapping("/api/performance")
@PerformanceMonitoringAspect.MonitorPerformance(type = "CONTROLLER")
public class PerformanceOptimizationController {
    
    private static final Logger logger = LoggerFactory.getLogger(PerformanceOptimizationController.class);
    
    @Autowired
    private PerformanceMonitoringService performanceMonitoringService;
    
    // KafkaTemplate和RedisTemplate已移除，使用虚假实现
    
    @Autowired
    private MeterRegistry meterRegistry;
    
    @Autowired
    @Qualifier("localCache")
    private Cache<String, Object> localCache;
    
    @Autowired
    @Qualifier("metricsCache")
    private Cache<String, Object> metricsCache;
    
    @Autowired
    @Qualifier("deduplicationCache")
    private Cache<String, Boolean> deduplicationCache;
    
    @Value("${app.performance.test.default-topic:performance-test}")
    private String defaultTestTopic;
    
    @Value("${app.performance.test.batch-size:100}")
    private int defaultBatchSize;
    
    /**
     * 获取性能指标概览
     */
    @GetMapping("/metrics/overview")
    public ResponseEntity<Map<String, Object>> getMetricsOverview() {
        try {
            Map<String, Object> overview = new HashMap<>();
            
            // 聚合指标
            Map<String, Object> aggregatedMetrics = performanceMonitoringService.getAggregatedMetrics();
            overview.put("aggregated", aggregatedMetrics);
            
            // 实例指标
            Map<String, PerformanceMetrics> allMetrics = performanceMonitoringService.getAllMetrics();
            overview.put("instances", allMetrics);
            
            // 告警统计
            Map<String, Object> alertStats = performanceMonitoringService.getAlertStatistics();
            overview.put("alerts", alertStats);
            
            // 系统指标
            Map<String, Object> systemMetrics = getSystemMetrics();
            overview.put("system", systemMetrics);
            
            return ResponseEntity.ok(overview);
            
        } catch (Exception e) {
            logger.error("获取性能指标概览失败", e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * 获取指定实例的性能指标
     */
    @GetMapping("/metrics/instance/{instanceId}")
    public ResponseEntity<PerformanceMetrics> getInstanceMetrics(@PathVariable String instanceId) {
        try {
            PerformanceMetrics metrics = performanceMonitoringService.getMetrics(instanceId);
            if (metrics != null) {
                return ResponseEntity.ok(metrics);
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            logger.error("获取实例性能指标失败: instanceId={}", instanceId, e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * 获取历史性能指标
     */
    @GetMapping("/metrics/history/{instanceId}")
    public ResponseEntity<List<PerformanceMetrics>> getHistoricalMetrics(
            @PathVariable String instanceId,
            @RequestParam(defaultValue = "1") int hours) {
        try {
            List<PerformanceMetrics> historicalMetrics = 
                performanceMonitoringService.getHistoricalMetrics(instanceId, hours);
            return ResponseEntity.ok(historicalMetrics);
        } catch (Exception e) {
            logger.error("获取历史性能指标失败: instanceId={}, hours={}", instanceId, hours, e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * 重置性能指标
     */
    @PostMapping("/metrics/reset")
    public ResponseEntity<Map<String, String>> resetMetrics(
            @RequestParam(required = false) String instanceId) {
        try {
            if (instanceId != null && !instanceId.isEmpty()) {
                performanceMonitoringService.resetMetrics(instanceId);
            } else {
                performanceMonitoringService.resetAllMetrics();
            }
            
            Map<String, String> result = new HashMap<>();
            result.put("status", "success");
            result.put("message", instanceId != null ? 
                "实例指标重置成功: " + instanceId : "所有指标重置成功");
            result.put("timestamp", LocalDateTime.now().toString());
            
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            logger.error("重置性能指标失败: instanceId={}", instanceId, e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * 性能压测 - 发送消息
     */
    @PostMapping("/test/send")
    public ResponseEntity<Map<String, Object>> performanceSendTest(
            @RequestParam(required = false) String topic,
            @RequestParam(defaultValue = "100") int messageCount,
            @RequestParam(defaultValue = "1024") int messageSize,
            @RequestParam(defaultValue = "false") boolean async) {
        
        String testTopic = topic != null ? topic : defaultTestTopic;
        
        try {
            logger.info("开始性能发送测试: topic={}, messageCount={}, messageSize={}, async={}", 
                testTopic, messageCount, messageSize, async);
            
            long startTime = System.currentTimeMillis();
            
            if (async) {
                return performAsyncSendTest(testTopic, messageCount, messageSize, startTime);
            } else {
                return performSyncSendTest(testTopic, messageCount, messageSize, startTime);
            }
            
        } catch (Exception e) {
            logger.error("性能发送测试失败", e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * 性能压测 - 批量发送消息
     */
    @PostMapping("/test/batch-send")
    public ResponseEntity<Map<String, Object>> performanceBatchSendTest(
            @RequestParam(required = false) String topic,
            @RequestParam(defaultValue = "1000") int totalMessages,
            @RequestParam(defaultValue = "100") int batchSize,
            @RequestParam(defaultValue = "1024") int messageSize) {
        
        String testTopic = topic != null ? topic : defaultTestTopic;
        
        try {
            logger.info("开始批量发送测试: topic={}, totalMessages={}, batchSize={}, messageSize={}", 
                testTopic, totalMessages, batchSize, messageSize);
            
            long startTime = System.currentTimeMillis();
            List<CompletableFuture<Void>> futures = new ArrayList<>();
            
            int batches = (totalMessages + batchSize - 1) / batchSize;
            
            for (int batch = 0; batch < batches; batch++) {
                int batchStart = batch * batchSize;
                int batchEnd = Math.min(batchStart + batchSize, totalMessages);
                
                CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
                    for (int i = batchStart; i < batchEnd; i++) {
                        try {
                            Map<String, Object> message = createTestMessage(i, messageSize);
                            // 虚假实现：模拟Kafka消息发送
                            Thread.sleep(1); // 模拟发送延迟
                            logger.debug("模拟发送消息到主题 {} 键 {}", testTopic, "key-" + i);
                        } catch (Exception e) {
                            logger.error("批量发送消息失败: index={}", i, e);
                        }
                    }
                });
                
                futures.add(future);
            }
            
            // 等待所有批次完成
            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
            
            long endTime = System.currentTimeMillis();
            long duration = endTime - startTime;
            
            Map<String, Object> result = new HashMap<>();
            result.put("status", "success");
            result.put("topic", testTopic);
            result.put("totalMessages", totalMessages);
            result.put("batchSize", batchSize);
            result.put("batches", batches);
            result.put("duration", duration);
            result.put("throughput", totalMessages * 1000.0 / duration);
            result.put("timestamp", LocalDateTime.now());
            
            logger.info("批量发送测试完成: 发送{}条消息，耗时{}ms，吞吐量{:.2f} msg/s", 
                totalMessages, duration, totalMessages * 1000.0 / duration);
            
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            logger.error("批量发送测试失败", e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * 缓存性能测试
     */
    @PostMapping("/test/cache")
    public ResponseEntity<Map<String, Object>> cachePerformanceTest(
            @RequestParam(defaultValue = "1000") int operationCount,
            @RequestParam(defaultValue = "local") String cacheType) {
        
        try {
            logger.info("开始缓存性能测试: operationCount={}, cacheType={}", operationCount, cacheType);
            
            Map<String, Object> result = new HashMap<>();
            
            if ("local".equals(cacheType)) {
                result = testLocalCache(operationCount);
            } else if ("redis".equals(cacheType)) {
                result = testRedisCache(operationCount);
            } else {
                result.put("error", "不支持的缓存类型: " + cacheType);
                return ResponseEntity.badRequest().body(result);
            }
            
            result.put("cacheType", cacheType);
            result.put("operationCount", operationCount);
            result.put("timestamp", LocalDateTime.now());
            
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            logger.error("缓存性能测试失败", e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * 获取缓存统计信息
     */
    @GetMapping("/cache/stats")
    public ResponseEntity<Map<String, Object>> getCacheStats() {
        try {
            Map<String, Object> stats = new HashMap<>();
            
            // 本地缓存统计
            stats.put("localCache", localCache.stats());
            stats.put("metricsCache", metricsCache.stats());
            stats.put("deduplicationCache", deduplicationCache.stats());
            
            // Redis连接信息（虚假实现）
            try {
                // 虚假实现：模拟Redis连接状态
                stats.put("redisConnected", true);
                stats.put("redisNote", "虚假实现，未实际连接Redis");
            } catch (Exception e) {
                stats.put("redisConnected", false);
                stats.put("redisError", e.getMessage());
            }
            
            return ResponseEntity.ok(stats);
            
        } catch (Exception e) {
            logger.error("获取缓存统计信息失败", e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * 清理缓存
     */
    @PostMapping("/cache/clear")
    public ResponseEntity<Map<String, String>> clearCache(
            @RequestParam(defaultValue = "all") String cacheType) {
        
        try {
            Map<String, String> result = new HashMap<>();
            
            switch (cacheType.toLowerCase()) {
                case "local":
                    localCache.invalidateAll();
                    result.put("message", "本地缓存已清理");
                    break;
                case "metrics":
                    metricsCache.invalidateAll();
                    result.put("message", "指标缓存已清理");
                    break;
                case "deduplication":
                    deduplicationCache.invalidateAll();
                    result.put("message", "去重缓存已清理");
                    break;
                case "all":
                    localCache.invalidateAll();
                    metricsCache.invalidateAll();
                    deduplicationCache.invalidateAll();
                    result.put("message", "所有缓存已清理");
                    break;
                default:
                    result.put("error", "不支持的缓存类型: " + cacheType);
                    return ResponseEntity.badRequest().body(result);
            }
            
            result.put("status", "success");
            result.put("timestamp", LocalDateTime.now().toString());
            
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            logger.error("清理缓存失败: cacheType={}", cacheType, e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * 获取Micrometer指标
     */
    @GetMapping("/metrics/micrometer")
    public ResponseEntity<Map<String, Object>> getMicrometerMetrics() {
        try {
            Map<String, Object> metrics = new HashMap<>();
            
            meterRegistry.getMeters().forEach(meter -> {
                String name = meter.getId().getName();
                if (name.startsWith("kafka.") || name.startsWith("jvm.") || name.startsWith("system.")) {
                    metrics.put(name, meter.measure());
                }
            });
            
            return ResponseEntity.ok(metrics);
            
        } catch (Exception e) {
            logger.error("获取Micrometer指标失败", e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * 健康检查
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> healthCheck() {
        Map<String, Object> health = new HashMap<>();
        
        try {
            // 检查Kafka连接
            boolean kafkaHealthy = checkKafkaHealth();
            health.put("kafka", kafkaHealthy ? "UP" : "DOWN");
            
            // 检查Redis连接
            boolean redisHealthy = checkRedisHealth();
            health.put("redis", redisHealthy ? "UP" : "DOWN");
            
            // 检查监控服务
            boolean monitoringHealthy = performanceMonitoringService != null;
            health.put("monitoring", monitoringHealthy ? "UP" : "DOWN");
            
            // 整体状态
            boolean overallHealthy = kafkaHealthy && redisHealthy && monitoringHealthy;
            health.put("status", overallHealthy ? "UP" : "DOWN");
            health.put("timestamp", LocalDateTime.now());
            
            return ResponseEntity.ok(health);
            
        } catch (Exception e) {
            logger.error("健康检查失败", e);
            health.put("status", "DOWN");
            health.put("error", e.getMessage());
            return ResponseEntity.internalServerError().body(health);
        }
    }
    
    // 私有辅助方法
    
    private ResponseEntity<Map<String, Object>> performSyncSendTest(String topic, int messageCount, int messageSize, long startTime) {
        // 虚假实现：模拟Kafka发送
        for (int i = 0; i < messageCount; i++) {
            Map<String, Object> message = createTestMessage(i, messageSize);
            // 模拟发送延迟
            try {
                Thread.sleep(1);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        
        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;
        
        Map<String, Object> result = createTestResult(topic, messageCount, duration, "sync");
        result.put("note", "虚假实现，未实际发送到Kafka");
        
        logger.info("同步发送测试完成(虚假实现): 模拟发送{}条消息，耗时{}ms，吞吐量{:.2f} msg/s", 
            messageCount, duration, messageCount * 1000.0 / duration);
        
        return ResponseEntity.ok(result);
    }
    
    private ResponseEntity<Map<String, Object>> performAsyncSendTest(String topic, int messageCount, int messageSize, long startTime) {
        // 虚假实现：模拟异步Kafka发送
        List<CompletableFuture<Void>> futures = IntStream.range(0, messageCount)
            .mapToObj(i -> CompletableFuture.runAsync(() -> {
                Map<String, Object> message = createTestMessage(i, messageSize);
                // 模拟异步发送延迟
                try {
                    Thread.sleep(1);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }))
            .collect(Collectors.toList());
        
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
        
        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;
        
        Map<String, Object> result = createTestResult(topic, messageCount, duration, "async");
        result.put("note", "虚假实现，未实际发送到Kafka");
        
        logger.info("异步发送测试完成(虚假实现): 模拟发送{}条消息，耗时{}ms，吞吐量{:.2f} msg/s", 
            messageCount, duration, messageCount * 1000.0 / duration);
        
        return ResponseEntity.ok(result);
    }
    
    private Map<String, Object> createTestMessage(int index, int size) {
        Map<String, Object> message = new HashMap<>();
        message.put("id", "test-" + index);
        message.put("timestamp", System.currentTimeMillis());
        message.put("index", index);
        
        // 填充到指定大小
        if (size > 100) {
            StringBuilder padding = new StringBuilder();
            for (int i = 0; i < size - 100; i++) {
                padding.append("x");
            }
            message.put("padding", padding.toString());
        }
        
        return message;
    }
    
    private Map<String, Object> createTestResult(String topic, int messageCount, long duration, String mode) {
        Map<String, Object> result = new HashMap<>();
        result.put("status", "success");
        result.put("topic", topic);
        result.put("messageCount", messageCount);
        result.put("duration", duration);
        result.put("throughput", messageCount * 1000.0 / duration);
        result.put("mode", mode);
        result.put("timestamp", LocalDateTime.now());
        return result;
    }
    
    private Map<String, Object> testLocalCache(int operationCount) {
        long startTime = System.currentTimeMillis();
        
        // 写入测试
        for (int i = 0; i < operationCount; i++) {
            localCache.put("test-key-" + i, "test-value-" + i);
        }
        long writeTime = System.currentTimeMillis() - startTime;
        
        // 读取测试
        startTime = System.currentTimeMillis();
        for (int i = 0; i < operationCount; i++) {
            localCache.getIfPresent("test-key-" + i);
        }
        long readTime = System.currentTimeMillis() - startTime;
        
        Map<String, Object> result = new HashMap<>();
        result.put("writeTime", writeTime);
        result.put("readTime", readTime);
        result.put("writeOps", operationCount * 1000.0 / writeTime);
        result.put("readOps", operationCount * 1000.0 / readTime);
        result.put("cacheStats", localCache.stats());
        
        return result;
    }
    
    private Map<String, Object> testRedisCache(int operationCount) {
        // 虚假实现：模拟Redis缓存操作
        long startTime = System.currentTimeMillis();
        
        // 模拟写入测试
        for (int i = 0; i < operationCount; i++) {
            // 模拟Redis写入延迟
            try {
                Thread.sleep(1);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        long writeTime = System.currentTimeMillis() - startTime;
        
        // 模拟读取测试
        startTime = System.currentTimeMillis();
        for (int i = 0; i < operationCount; i++) {
            // 模拟Redis读取延迟
            try {
                Thread.sleep(1);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        long readTime = System.currentTimeMillis() - startTime;
        
        Map<String, Object> result = new HashMap<>();
        result.put("writeTime", writeTime);
        result.put("readTime", readTime);
        result.put("writeOps", operationCount * 1000.0 / writeTime);
        result.put("readOps", operationCount * 1000.0 / readTime);
        result.put("note", "虚假实现，未实际操作Redis");
        
        return result;
    }
    
    private Map<String, Object> getSystemMetrics() {
        Map<String, Object> systemMetrics = new HashMap<>();
        
        Runtime runtime = Runtime.getRuntime();
        systemMetrics.put("totalMemory", runtime.totalMemory());
        systemMetrics.put("freeMemory", runtime.freeMemory());
        systemMetrics.put("usedMemory", runtime.totalMemory() - runtime.freeMemory());
        systemMetrics.put("maxMemory", runtime.maxMemory());
        systemMetrics.put("availableProcessors", runtime.availableProcessors());
        
        return systemMetrics;
    }
    
    private boolean checkKafkaHealth() {
        // 虚假实现：模拟Kafka健康检查
        try {
            // 模拟健康检查延迟
            Thread.sleep(10);
            return true;
        } catch (Exception e) {
            logger.debug("Kafka健康检查失败", e);
            return false;
        }
    }
    
    private boolean checkRedisHealth() {
        // 虚假实现：模拟Redis健康检查
        try {
            // 模拟健康检查延迟
            Thread.sleep(10);
            return true;
        } catch (Exception e) {
            logger.debug("Redis健康检查失败", e);
            return false;
        }
    }
}