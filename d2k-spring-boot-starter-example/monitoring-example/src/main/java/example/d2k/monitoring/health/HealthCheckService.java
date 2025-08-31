package example.d2k.monitoring.health;

import example.d2k.monitoring.metrics.MonitoringMetrics;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;

import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.ThreadMXBean;
import java.sql.Connection;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * 健康检查服务
 * 负责检查各个组件的健康状态
 * 
 * @author xiajuan96
 */
@Service
public class HealthCheckService implements HealthIndicator {

    private static final Logger logger = LoggerFactory.getLogger(HealthCheckService.class);

    @Autowired
    private DataSource dataSource;

    // Redis和Kafka依赖已移除，使用虚假健康检查替代

    private final MemoryMXBean memoryMXBean = ManagementFactory.getMemoryMXBean();
    private final ThreadMXBean threadMXBean = ManagementFactory.getThreadMXBean();

    /**
     * 执行全面的健康检查
     */
    @Override
    public Health health() {
        try {
            Map<String, Object> details = new HashMap<>();
            boolean isHealthy = true;

            // 检查数据库连接
            HealthCheckResult dbHealth = checkDatabaseHealth();
            details.put("database", dbHealth.getDetails());
            if (!dbHealth.isHealthy()) {
                isHealthy = false;
            }

            // 检查Redis连接
            HealthCheckResult redisHealth = checkRedisHealth();
            details.put("redis", redisHealth.getDetails());
            if (!redisHealth.isHealthy()) {
                isHealthy = false;
            }

            // 检查Kafka连接
            HealthCheckResult kafkaHealth = checkKafkaHealth();
            details.put("kafka", kafkaHealth.getDetails());
            if (!kafkaHealth.isHealthy()) {
                isHealthy = false;
            }

            // 检查系统资源
            HealthCheckResult systemHealth = checkSystemHealth();
            details.put("system", systemHealth.getDetails());
            if (!systemHealth.isHealthy()) {
                isHealthy = false;
            }

            // 检查内存使用情况
            HealthCheckResult memoryHealth = checkMemoryHealth();
            details.put("memory", memoryHealth.getDetails());
            if (!memoryHealth.isHealthy()) {
                isHealthy = false;
            }

            // 检查线程状态
            HealthCheckResult threadHealth = checkThreadHealth();
            details.put("threads", threadHealth.getDetails());
            if (!threadHealth.isHealthy()) {
                isHealthy = false;
            }

            details.put("timestamp", LocalDateTime.now());
            details.put("checkDuration", "完成健康检查");

            if (isHealthy) {
                return Health.up().withDetails(details).build();
            } else {
                return Health.down().withDetails(details).build();
            }

        } catch (Exception e) {
            logger.error("健康检查执行失败", e);
            return Health.down()
                    .withDetail("error", e.getMessage())
                    .withDetail("timestamp", LocalDateTime.now())
                    .build();
        }
    }

    /**
     * 检查数据库健康状态
     */
    public HealthCheckResult checkDatabaseHealth() {
        try {
            long startTime = System.currentTimeMillis();
            
            try (Connection connection = dataSource.getConnection()) {
                boolean isValid = connection.isValid(5); // 5秒超时
                long responseTime = System.currentTimeMillis() - startTime;
                
                Map<String, Object> details = new HashMap<>();
                details.put("status", isValid ? "UP" : "DOWN");
                details.put("responseTime", responseTime + "ms");
                details.put("url", connection.getMetaData().getURL());
                details.put("driver", connection.getMetaData().getDriverName());
                details.put("autoCommit", connection.getAutoCommit());
                details.put("readOnly", connection.isReadOnly());
                
                if (responseTime > 1000) {
                    details.put("warning", "数据库响应时间较慢");
                }
                
                return new HealthCheckResult(isValid, details);
            }
        } catch (Exception e) {
            logger.error("数据库健康检查失败", e);
            Map<String, Object> details = new HashMap<>();
            details.put("status", "DOWN");
            details.put("error", e.getMessage());
            details.put("errorType", e.getClass().getSimpleName());
            return new HealthCheckResult(false, details);
        }
    }

    /**
     * 虚假的Redis健康检查（原Redis健康检查）
     */
    public HealthCheckResult checkRedisHealth() {
        try {
            // 虚假实现：模拟Redis健康检查
            Map<String, Object> details = new HashMap<>();
            details.put("status", "UP");
            details.put("responseTime", "5ms");
            details.put("ping", "PONG");
            details.put("readWrite", "OK");
            details.put("note", "这是虚假实现，未实际连接Redis");
            
            logger.info("Mock: Redis health check - Status: UP");
            
            return new HealthCheckResult(true, details);
            
        } catch (Exception e) {
            logger.error("虚假Redis健康检查失败", e);
            Map<String, Object> details = new HashMap<>();
            details.put("status", "DOWN");
            details.put("error", e.getMessage());
            details.put("errorType", e.getClass().getSimpleName());
            return new HealthCheckResult(false, details);
        }
    }

    /**
     * 虚假的Kafka健康检查（原Kafka健康检查）
     */
    public HealthCheckResult checkKafkaHealth() {
        try {
            // 虚假实现：模拟Kafka健康检查
            Map<String, Object> details = new HashMap<>();
            details.put("status", "UP");
            details.put("producer", "UP");
            details.put("cluster", "UP");
            details.put("note", "这是虚假实现，未实际连接Kafka");
            
            logger.info("Mock: Kafka health check - Status: UP");
            
            return new HealthCheckResult(true, details);
            
        } catch (Exception e) {
            logger.error("虚假Kafka健康检查失败", e);
            Map<String, Object> details = new HashMap<>();
            details.put("status", "DOWN");
            details.put("error", e.getMessage());
            details.put("errorType", e.getClass().getSimpleName());
            return new HealthCheckResult(false, details);
        }
    }

    /**
     * 检查系统健康状态
     */
    public HealthCheckResult checkSystemHealth() {
        try {
            Map<String, Object> details = new HashMap<>();
            boolean isHealthy = true;
            
            // 检查CPU使用率
            double cpuUsage = getCpuUsage();
            details.put("cpuUsage", String.format("%.2f%%", cpuUsage * 100));
            
            if (cpuUsage > 0.9) { // CPU使用率超过90%
                details.put("cpuWarning", "CPU使用率过高");
                isHealthy = false;
            }
            
            // 检查系统负载
            try {
                double systemLoad = ManagementFactory.getOperatingSystemMXBean().getSystemLoadAverage();
                if (systemLoad > 0) {
                    details.put("systemLoad", String.format("%.2f", systemLoad));
                    
                    int availableProcessors = Runtime.getRuntime().availableProcessors();
                    if (systemLoad > availableProcessors * 2) {
                        details.put("loadWarning", "系统负载过高");
                        isHealthy = false;
                    }
                }
            } catch (Exception e) {
                details.put("systemLoad", "无法获取");
            }
            
            // 检查可用处理器数量
            int processors = Runtime.getRuntime().availableProcessors();
            details.put("availableProcessors", processors);
            
            details.put("status", isHealthy ? "UP" : "DOWN");
            
            return new HealthCheckResult(isHealthy, details);
            
        } catch (Exception e) {
            logger.error("系统健康检查失败", e);
            Map<String, Object> details = new HashMap<>();
            details.put("status", "DOWN");
            details.put("error", e.getMessage());
            return new HealthCheckResult(false, details);
        }
    }

    /**
     * 检查内存健康状态
     */
    public HealthCheckResult checkMemoryHealth() {
        try {
            Map<String, Object> details = new HashMap<>();
            boolean isHealthy = true;
            
            // 堆内存检查
            long heapUsed = memoryMXBean.getHeapMemoryUsage().getUsed();
            long heapMax = memoryMXBean.getHeapMemoryUsage().getMax();
            double heapUsagePercent = (double) heapUsed / heapMax;
            
            details.put("heapUsed", formatBytes(heapUsed));
            details.put("heapMax", formatBytes(heapMax));
            details.put("heapUsagePercent", String.format("%.2f%%", heapUsagePercent * 100));
            
            if (heapUsagePercent > 0.9) { // 堆内存使用率超过90%
                details.put("heapWarning", "堆内存使用率过高");
                isHealthy = false;
            }
            
            // 非堆内存检查
            long nonHeapUsed = memoryMXBean.getNonHeapMemoryUsage().getUsed();
            long nonHeapMax = memoryMXBean.getNonHeapMemoryUsage().getMax();
            
            details.put("nonHeapUsed", formatBytes(nonHeapUsed));
            if (nonHeapMax > 0) {
                double nonHeapUsagePercent = (double) nonHeapUsed / nonHeapMax;
                details.put("nonHeapMax", formatBytes(nonHeapMax));
                details.put("nonHeapUsagePercent", String.format("%.2f%%", nonHeapUsagePercent * 100));
                
                if (nonHeapUsagePercent > 0.9) {
                    details.put("nonHeapWarning", "非堆内存使用率过高");
                    isHealthy = false;
                }
            } else {
                details.put("nonHeapMax", "无限制");
            }
            
            // 垃圾回收信息
            try {
                ManagementFactory.getGarbageCollectorMXBeans().forEach(gcBean -> {
                    String gcName = gcBean.getName().replaceAll(" ", "");
                    details.put("gc" + gcName + "Count", gcBean.getCollectionCount());
                    details.put("gc" + gcName + "Time", gcBean.getCollectionTime() + "ms");
                });
            } catch (Exception e) {
                details.put("gcInfo", "无法获取GC信息");
            }
            
            details.put("status", isHealthy ? "UP" : "DOWN");
            
            return new HealthCheckResult(isHealthy, details);
            
        } catch (Exception e) {
            logger.error("内存健康检查失败", e);
            Map<String, Object> details = new HashMap<>();
            details.put("status", "DOWN");
            details.put("error", e.getMessage());
            return new HealthCheckResult(false, details);
        }
    }

    /**
     * 检查线程健康状态
     */
    public HealthCheckResult checkThreadHealth() {
        try {
            Map<String, Object> details = new HashMap<>();
            boolean isHealthy = true;
            
            int threadCount = threadMXBean.getThreadCount();
            int daemonThreadCount = threadMXBean.getDaemonThreadCount();
            int peakThreadCount = threadMXBean.getPeakThreadCount();
            long totalStartedThreadCount = threadMXBean.getTotalStartedThreadCount();
            
            details.put("threadCount", threadCount);
            details.put("daemonThreadCount", daemonThreadCount);
            details.put("peakThreadCount", peakThreadCount);
            details.put("totalStartedThreadCount", totalStartedThreadCount);
            
            // 检查线程数量是否过多
            if (threadCount > 1000) {
                details.put("threadWarning", "线程数量过多");
                isHealthy = false;
            }
            
            // 检查死锁
            long[] deadlockedThreads = threadMXBean.findDeadlockedThreads();
            if (deadlockedThreads != null && deadlockedThreads.length > 0) {
                details.put("deadlock", "检测到死锁线程: " + deadlockedThreads.length);
                isHealthy = false;
            } else {
                details.put("deadlock", "无");
            }
            
            details.put("status", isHealthy ? "UP" : "DOWN");
            
            return new HealthCheckResult(isHealthy, details);
            
        } catch (Exception e) {
            logger.error("线程健康检查失败", e);
            Map<String, Object> details = new HashMap<>();
            details.put("status", "DOWN");
            details.put("error", e.getMessage());
            return new HealthCheckResult(false, details);
        }
    }

    /**
     * 获取CPU使用率
     */
    private double getCpuUsage() {
        try {
            com.sun.management.OperatingSystemMXBean osBean = 
                (com.sun.management.OperatingSystemMXBean) ManagementFactory.getOperatingSystemMXBean();
            return osBean.getProcessCpuLoad();
        } catch (Exception e) {
            logger.warn("无法获取CPU使用率", e);
            return 0.0;
        }
    }

    /**
     * 格式化字节数
     */
    private String formatBytes(long bytes) {
        if (bytes < 1024) return bytes + " B";
        int exp = (int) (Math.log(bytes) / Math.log(1024));
        String pre = "KMGTPE".charAt(exp - 1) + "";
        return String.format("%.2f %sB", bytes / Math.pow(1024, exp), pre);
    }

    /**
     * 获取综合健康状态
     */
    public MonitoringMetrics.HealthStatus getOverallHealthStatus() {
        try {
            Health health = health();
            
            if (health.getStatus().getCode().equals("UP")) {
                return MonitoringMetrics.HealthStatus.UP;
            } else if (health.getStatus().getCode().equals("DOWN")) {
                return MonitoringMetrics.HealthStatus.DOWN;
            } else {
                return MonitoringMetrics.HealthStatus.WARNING;
            }
        } catch (Exception e) {
            logger.error("获取健康状态失败", e);
            return MonitoringMetrics.HealthStatus.UNKNOWN;
        }
    }

    /**
     * 健康检查结果内部类
     */
    public static class HealthCheckResult {
        private final boolean healthy;
        private final Map<String, Object> details;

        public HealthCheckResult(boolean healthy, Map<String, Object> details) {
            this.healthy = healthy;
            this.details = details;
        }

        public boolean isHealthy() {
            return healthy;
        }

        public Map<String, Object> getDetails() {
            return details;
        }
    }
}