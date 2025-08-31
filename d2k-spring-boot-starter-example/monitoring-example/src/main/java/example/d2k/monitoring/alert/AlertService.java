package example.d2k.monitoring.alert;

import example.d2k.monitoring.metrics.MonitoringMetrics;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import javax.annotation.PostConstruct;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * 告警服务
 * 负责监控指标告警、通知发送等功能
 * 
 * @author xiajuan96
 */
@Service
public class AlertService {

    private static final Logger logger = LoggerFactory.getLogger(AlertService.class);

    // Redis依赖已移除，使用内存存储替代

    @Autowired(required = false)
    private JavaMailSender mailSender;

    private final WebClient webClient = WebClient.builder().build();

    // 告警配置
    @Value("${app.monitoring.alert.enabled:true}")
    private boolean alertEnabled;

    @Value("${app.monitoring.alert.kafka-error-rate-threshold:0.05}")
    private double kafkaErrorRateThreshold;

    @Value("${app.monitoring.alert.memory-threshold:0.9}")
    private double memoryThreshold;

    @Value("${app.monitoring.alert.cpu-threshold:0.8}")
    private double cpuThreshold;

    @Value("${app.monitoring.alert.response-time-threshold:5000}")
    private long responseTimeThreshold;

    @Value("${app.monitoring.alert.thread-count-threshold:1000}")
    private int threadCountThreshold;

    // 通知配置
    @Value("${app.monitoring.notification.email.enabled:false}")
    private boolean emailEnabled;

    @Value("${app.monitoring.notification.email.to:}")
    private String emailTo;

    @Value("${app.monitoring.notification.email.from:}")
    private String emailFrom;

    @Value("${app.monitoring.notification.webhook.enabled:false}")
    private boolean webhookEnabled;

    @Value("${app.monitoring.notification.webhook.url:}")
    private String webhookUrl;

    // 告警抑制配置
    @Value("${app.monitoring.alert.suppression.duration:300}")
    private long alertSuppressionDuration; // 秒

    @Value("${app.monitoring.alert.escalation.duration:1800}")
    private long alertEscalationDuration; // 秒

    // 内存中的告警状态管理
    private final Map<String, AlertState> activeAlerts = new ConcurrentHashMap<>();
    private final Map<String, Long> alertSuppressionMap = new ConcurrentHashMap<>();

    @PostConstruct
    public void init() {
        logger.info("告警服务初始化完成 - 告警启用: {}, 邮件通知: {}, Webhook通知: {}", 
                   alertEnabled, emailEnabled, webhookEnabled);
    }

    /**
     * 检查监控指标并触发告警
     */
    public void checkMetricsAndAlert(MonitoringMetrics metrics) {
        if (!alertEnabled) {
            return;
        }

        try {
            List<Alert> triggeredAlerts = new ArrayList<>();

            // 检查Kafka错误率
            double kafkaErrorRate = metrics.getKafkaErrorRate();
            if (kafkaErrorRate > kafkaErrorRateThreshold) {
                triggeredAlerts.add(createAlert(
                    "KAFKA_ERROR_RATE_HIGH",
                    AlertLevel.ERROR,
                    String.format("Kafka错误率过高: %.2f%% (阈值: %.2f%%)", 
                                kafkaErrorRate * 100, kafkaErrorRateThreshold * 100),
                    metrics.getInstanceId()
                ));
            }

            // 检查内存使用率
            if (metrics.getMemoryUsagePercent() > memoryThreshold) {
                triggeredAlerts.add(createAlert(
                    "MEMORY_USAGE_HIGH",
                    AlertLevel.WARNING,
                    String.format("内存使用率过高: %.2f%% (阈值: %.2f%%)", 
                                metrics.getMemoryUsagePercent() * 100, memoryThreshold * 100),
                    metrics.getInstanceId()
                ));
            }

            // 检查CPU使用率
            if (metrics.getCpuUsage() > cpuThreshold) {
                triggeredAlerts.add(createAlert(
                    "CPU_USAGE_HIGH",
                    AlertLevel.WARNING,
                    String.format("CPU使用率过高: %.2f%% (阈值: %.2f%%)", 
                                metrics.getCpuUsage() * 100, cpuThreshold * 100),
                    metrics.getInstanceId()
                ));
            }

            // 检查HTTP响应时间
            if (metrics.getHttpMaxResponseTime() > responseTimeThreshold) {
                triggeredAlerts.add(createAlert(
                    "RESPONSE_TIME_HIGH",
                    AlertLevel.WARNING,
                    String.format("HTTP响应时间过长: %.2fms (阈值: %dms)", 
                                metrics.getHttpMaxResponseTime(), responseTimeThreshold),
                    metrics.getInstanceId()
                ));
            }

            // 检查线程数量
            if (metrics.getThreadCount() > threadCountThreshold) {
                triggeredAlerts.add(createAlert(
                    "THREAD_COUNT_HIGH",
                    AlertLevel.WARNING,
                    String.format("线程数量过多: %d (阈值: %d)", 
                                metrics.getThreadCount(), threadCountThreshold),
                    metrics.getInstanceId()
                ));
            }

            // 检查健康状态
            if (metrics.getHealthStatus() == MonitoringMetrics.HealthStatus.DOWN) {
                triggeredAlerts.add(createAlert(
                    "HEALTH_CHECK_FAILED",
                    AlertLevel.CRITICAL,
                    "健康检查失败: " + metrics.getHealthDetails(),
                    metrics.getInstanceId()
                ));
            }

            // 处理触发的告警
            for (Alert alert : triggeredAlerts) {
                processAlert(alert);
            }

            // 清理已恢复的告警
            cleanupResolvedAlerts(metrics);

        } catch (Exception e) {
            logger.error("告警检查失败", e);
        }
    }

    /**
     * 处理告警
     */
    private void processAlert(Alert alert) {
        String alertKey = alert.getType() + ":" + alert.getInstanceId();
        
        // 检查告警抑制
        if (isAlertSuppressed(alertKey)) {
            logger.debug("告警被抑制: {}", alertKey);
            return;
        }

        AlertState existingAlert = activeAlerts.get(alertKey);
        
        if (existingAlert == null) {
            // 新告警
            AlertState newAlertState = new AlertState(alert);
            activeAlerts.put(alertKey, newAlertState);
            
            logger.warn("触发新告警: {} - {}", alert.getType(), alert.getMessage());
            
            // 发送通知
            sendNotification(alert, "NEW");
            
            // 设置告警抑制
            setAlertSuppression(alertKey);
            
        } else {
            // 更新现有告警
            existingAlert.updateOccurrence();
            existingAlert.setLastOccurrence(LocalDateTime.now());
            
            // 检查是否需要升级告警
            if (shouldEscalateAlert(existingAlert)) {
                alert.setLevel(escalateAlertLevel(alert.getLevel()));
                sendNotification(alert, "ESCALATED");
                existingAlert.setEscalated(true);
                logger.error("告警升级: {} - {}", alert.getType(), alert.getMessage());
            }
        }
        
        // 存储到Redis
        storeAlertToRedis(alert);
    }

    /**
     * 清理已恢复的告警
     */
    private void cleanupResolvedAlerts(MonitoringMetrics metrics) {
        List<String> resolvedAlerts = new ArrayList<>();
        
        for (Map.Entry<String, AlertState> entry : activeAlerts.entrySet()) {
            String alertKey = entry.getKey();
            AlertState alertState = entry.getValue();
            
            if (isAlertResolved(alertState.getAlert(), metrics)) {
                resolvedAlerts.add(alertKey);
                
                // 发送恢复通知
                sendNotification(alertState.getAlert(), "RESOLVED");
                logger.info("告警已恢复: {} - {}", 
                          alertState.getAlert().getType(), alertState.getAlert().getMessage());
            }
        }
        
        // 移除已恢复的告警
        for (String alertKey : resolvedAlerts) {
            activeAlerts.remove(alertKey);
            removeAlertFromRedis(alertKey);
        }
    }

    /**
     * 判断告警是否已恢复
     */
    private boolean isAlertResolved(Alert alert, MonitoringMetrics metrics) {
        switch (alert.getType()) {
            case "KAFKA_ERROR_RATE_HIGH":
                return metrics.getKafkaErrorRate() <= kafkaErrorRateThreshold * 0.8; // 恢复阈值
            case "MEMORY_USAGE_HIGH":
                return metrics.getMemoryUsagePercent() <= memoryThreshold * 0.8;
            case "CPU_USAGE_HIGH":
                return metrics.getCpuUsage() <= cpuThreshold * 0.8;
            case "RESPONSE_TIME_HIGH":
                return metrics.getHttpMaxResponseTime() <= responseTimeThreshold * 0.8;
            case "THREAD_COUNT_HIGH":
                return metrics.getThreadCount() <= threadCountThreshold * 0.8;
            case "HEALTH_CHECK_FAILED":
                return metrics.getHealthStatus() == MonitoringMetrics.HealthStatus.UP;
            default:
                return false;
        }
    }

    /**
     * 发送通知
     */
    @Async
    public void sendNotification(Alert alert, String action) {
        try {
            String subject = String.format("[%s] %s - %s", action, alert.getLevel(), alert.getType());
            String content = formatNotificationContent(alert, action);
            
            // 发送邮件通知
            if (emailEnabled && mailSender != null && !emailTo.isEmpty()) {
                sendEmailNotification(subject, content);
            }
            
            // 发送Webhook通知
            if (webhookEnabled && !webhookUrl.isEmpty()) {
                sendWebhookNotification(alert, action);
            }
            
            logger.info("告警通知已发送: {} - {}", action, alert.getType());
            
        } catch (Exception e) {
            logger.error("发送告警通知失败", e);
        }
    }

    /**
     * 发送邮件通知
     */
    private void sendEmailNotification(String subject, String content) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(emailFrom);
            message.setTo(emailTo.split(","));
            message.setSubject(subject);
            message.setText(content);
            
            mailSender.send(message);
            logger.debug("邮件通知发送成功");
            
        } catch (Exception e) {
            logger.error("邮件通知发送失败", e);
        }
    }

    /**
     * 发送Webhook通知
     */
    private void sendWebhookNotification(Alert alert, String action) {
        try {
            Map<String, Object> payload = new HashMap<>();
            payload.put("action", action);
            payload.put("alert", alert);
            payload.put("timestamp", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
            
            webClient.post()
                    .uri(webhookUrl)
                    .bodyValue(payload)
                    .retrieve()
                    .bodyToMono(String.class)
                    .subscribe(
                        response -> logger.debug("Webhook通知发送成功: {}", response),
                        error -> logger.error("Webhook通知发送失败", error)
                    );
                    
        } catch (Exception e) {
            logger.error("Webhook通知发送失败", e);
        }
    }

    /**
     * 格式化通知内容
     */
    private String formatNotificationContent(Alert alert, String action) {
        StringBuilder content = new StringBuilder();
        content.append("告警详情:\n");
        content.append("动作: ").append(action).append("\n");
        content.append("类型: ").append(alert.getType()).append("\n");
        content.append("级别: ").append(alert.getLevel()).append("\n");
        content.append("实例: ").append(alert.getInstanceId()).append("\n");
        content.append("消息: ").append(alert.getMessage()).append("\n");
        content.append("时间: ").append(alert.getTimestamp().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))).append("\n");
        
        if (alert.getDetails() != null && !alert.getDetails().isEmpty()) {
            content.append("详细信息: ").append(alert.getDetails()).append("\n");
        }
        
        return content.toString();
    }

    /**
     * 创建告警对象
     */
    private Alert createAlert(String type, AlertLevel level, String message, String instanceId) {
        Alert alert = new Alert();
        alert.setId(UUID.randomUUID().toString());
        alert.setType(type);
        alert.setLevel(level);
        alert.setMessage(message);
        alert.setInstanceId(instanceId);
        alert.setTimestamp(LocalDateTime.now());
        return alert;
    }

    /**
     * 检查告警是否被抑制
     */
    private boolean isAlertSuppressed(String alertKey) {
        Long suppressionTime = alertSuppressionMap.get(alertKey);
        if (suppressionTime == null) {
            return false;
        }
        
        long currentTime = System.currentTimeMillis();
        if (currentTime - suppressionTime > alertSuppressionDuration * 1000) {
            alertSuppressionMap.remove(alertKey);
            return false;
        }
        
        return true;
    }

    /**
     * 设置告警抑制
     */
    private void setAlertSuppression(String alertKey) {
        alertSuppressionMap.put(alertKey, System.currentTimeMillis());
    }

    /**
     * 判断是否需要升级告警
     */
    private boolean shouldEscalateAlert(AlertState alertState) {
        if (alertState.isEscalated()) {
            return false;
        }
        
        long duration = System.currentTimeMillis() - alertState.getFirstOccurrence().atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli();
        return duration > alertEscalationDuration * 1000;
    }

    /**
     * 升级告警级别
     */
    private AlertLevel escalateAlertLevel(AlertLevel currentLevel) {
        switch (currentLevel) {
            case INFO:
                return AlertLevel.WARNING;
            case WARNING:
                return AlertLevel.ERROR;
            case ERROR:
                return AlertLevel.CRITICAL;
            default:
                return currentLevel;
        }
    }

    /**
     * 虚假的告警存储方法（原Redis存储）
     */
    private void storeAlertToRedis(Alert alert) {
        // 虚假实现：仅记录日志，不进行实际存储
        logger.info("Mock: Alert stored to cache - ID: {}, Type: {}, Level: {}", 
                   alert.getId(), alert.getType(), alert.getLevel());
    }

    /**
     * 虚假的告警移除方法（原Redis移除）
     */
    private void removeAlertFromRedis(String alertKey) {
        // 虚假实现：仅记录日志，不进行实际移除
        logger.info("Mock: Alert removed from cache - Key: {}", alertKey);
    }

    /**
     * 获取活跃告警列表
     */
    public List<Alert> getActiveAlerts() {
        return activeAlerts.values().stream()
                .map(AlertState::getAlert)
                .sorted((a1, a2) -> a2.getTimestamp().compareTo(a1.getTimestamp()))
                .collect(java.util.stream.Collectors.toList());
    }

    /**
     * 获取告警统计信息
     */
    public Map<String, Object> getAlertStatistics() {
        Map<String, Object> stats = new HashMap<>();
        
        long totalAlerts = activeAlerts.size();
        long criticalAlerts = activeAlerts.values().stream()
                .mapToLong(state -> state.getAlert().getLevel() == AlertLevel.CRITICAL ? 1 : 0)
                .sum();
        long errorAlerts = activeAlerts.values().stream()
                .mapToLong(state -> state.getAlert().getLevel() == AlertLevel.ERROR ? 1 : 0)
                .sum();
        long warningAlerts = activeAlerts.values().stream()
                .mapToLong(state -> state.getAlert().getLevel() == AlertLevel.WARNING ? 1 : 0)
                .sum();
        
        stats.put("totalAlerts", totalAlerts);
        stats.put("criticalAlerts", criticalAlerts);
        stats.put("errorAlerts", errorAlerts);
        stats.put("warningAlerts", warningAlerts);
        stats.put("alertEnabled", alertEnabled);
        stats.put("emailEnabled", emailEnabled);
        stats.put("webhookEnabled", webhookEnabled);
        
        return stats;
    }

    /**
     * 手动触发告警测试
     */
    public void triggerTestAlert(String instanceId) {
        Alert testAlert = createAlert(
            "TEST_ALERT",
            AlertLevel.INFO,
            "这是一个测试告警",
            instanceId
        );
        
        sendNotification(testAlert, "TEST");
        logger.info("测试告警已触发: {}", instanceId);
    }

    /**
     * 告警级别枚举
     */
    public enum AlertLevel {
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

    /**
     * 告警实体类
     */
    public static class Alert {
        private String id;
        private String type;
        private AlertLevel level;
        private String message;
        private String instanceId;
        private LocalDateTime timestamp;
        private Map<String, Object> details;

        // Getters and Setters
        public String getId() { return id; }
        public void setId(String id) { this.id = id; }
        public String getType() { return type; }
        public void setType(String type) { this.type = type; }
        public AlertLevel getLevel() { return level; }
        public void setLevel(AlertLevel level) { this.level = level; }
        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
        public String getInstanceId() { return instanceId; }
        public void setInstanceId(String instanceId) { this.instanceId = instanceId; }
        public LocalDateTime getTimestamp() { return timestamp; }
        public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }
        public Map<String, Object> getDetails() { return details; }
        public void setDetails(Map<String, Object> details) { this.details = details; }
    }

    /**
     * 告警状态内部类
     */
    private static class AlertState {
        private final Alert alert;
        private final LocalDateTime firstOccurrence;
        private LocalDateTime lastOccurrence;
        private int occurrenceCount;
        private boolean escalated;

        public AlertState(Alert alert) {
            this.alert = alert;
            this.firstOccurrence = alert.getTimestamp();
            this.lastOccurrence = alert.getTimestamp();
            this.occurrenceCount = 1;
            this.escalated = false;
        }

        public void updateOccurrence() {
            this.occurrenceCount++;
        }

        // Getters and Setters
        public Alert getAlert() { return alert; }
        public LocalDateTime getFirstOccurrence() { return firstOccurrence; }
        public LocalDateTime getLastOccurrence() { return lastOccurrence; }
        public void setLastOccurrence(LocalDateTime lastOccurrence) { this.lastOccurrence = lastOccurrence; }
        public int getOccurrenceCount() { return occurrenceCount; }
        public boolean isEscalated() { return escalated; }
        public void setEscalated(boolean escalated) { this.escalated = escalated; }
    }
}