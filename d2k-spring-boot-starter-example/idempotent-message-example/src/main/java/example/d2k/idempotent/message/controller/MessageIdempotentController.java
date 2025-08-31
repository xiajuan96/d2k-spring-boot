package example.d2k.idempotent.message.controller;

import example.d2k.idempotent.message.entity.MessageRecord;
import example.d2k.idempotent.message.entity.MessageType;
import example.d2k.idempotent.message.entity.ProcessingStatus;
import example.d2k.idempotent.message.service.MessageIdempotentService;
import com.d2k.spring.boot.autoconfigure.template.D2kTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.*;

/**
 * 消息幂等性处理控制器
 * 提供消息幂等性管理的RESTful API接口
 * @author xiajuan96
 */
@RestController
@RequestMapping("/api/message-idempotent")
public class MessageIdempotentController {
    
    private static final Logger logger = LoggerFactory.getLogger(MessageIdempotentController.class);
    
    @Autowired
    private MessageIdempotentService messageIdempotentService;
    
    @Autowired
    private D2kTemplate d2kTemplate;
    
    /**
     * 检查消息是否已处理
     */
    @GetMapping("/check/{messageId}")
    public ResponseEntity<Map<String, Object>> checkMessageProcessed(@PathVariable String messageId) {
        try {
            boolean processed = messageIdempotentService.isMessageProcessed(messageId);
            Optional<MessageRecord> record = messageIdempotentService.getMessageRecord(messageId);
            
            Map<String, Object> result = new HashMap<>();
            result.put("messageId", messageId);
            result.put("processed", processed);
            result.put("exists", record.isPresent());
            
            if (record.isPresent()) {
                result.put("record", record.get());
            }
            
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            logger.error("检查消息处理状态失败", e);
            Map<String, Object> errorResult = new HashMap<>();
            errorResult.put("error", "检查消息处理状态失败: " + e.getMessage());
            return ResponseEntity.internalServerError().body(errorResult);
        }
    }
    
    /**
     * 获取消息记录详情
     */
    @GetMapping("/record/{messageId}")
    public ResponseEntity<Object> getMessageRecord(@PathVariable String messageId) {
        try {
            Optional<MessageRecord> record = messageIdempotentService.getMessageRecord(messageId);
            
            if (record.isPresent()) {
                return ResponseEntity.ok(record.get());
            } else {
                Map<String, Object> errorResult = new HashMap<>();
                errorResult.put("error", "消息记录不存在");
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            logger.error("获取消息记录失败", e);
            Map<String, Object> errorResult = new HashMap<>();
            errorResult.put("error", "获取消息记录失败: " + e.getMessage());
            return ResponseEntity.internalServerError().body(errorResult);
        }
    }
    
    /**
     * 根据业务键获取消息列表
     */
    @GetMapping("/records/business/{businessKey}")
    public ResponseEntity<Object> getMessagesByBusinessKey(@PathVariable String businessKey) {
        try {
            List<MessageRecord> messages = messageIdempotentService.getMessagesByBusinessKey(businessKey);
            
            Map<String, Object> result = new HashMap<>();
            result.put("businessKey", businessKey);
            result.put("count", messages.size());
            result.put("messages", messages);
            
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            logger.error("获取业务消息列表失败", e);
            Map<String, Object> errorResult = new HashMap<>();
            errorResult.put("error", "获取业务消息列表失败: " + e.getMessage());
            return ResponseEntity.internalServerError().body(errorResult);
        }
    }
    
    /**
     * 分页查询消息记录
     */
    @GetMapping("/records")
    public ResponseEntity<Object> getMessages(
            @RequestParam(required = false) MessageType messageType,
            @RequestParam(required = false) ProcessingStatus status,
            @RequestParam(required = false) String consumerGroup,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        try {
            Page<MessageRecord> messages = messageIdempotentService.getMessages(
                messageType, status, consumerGroup, startTime, endTime, page, size);
            
            Map<String, Object> result = new HashMap<>();
            result.put("content", messages.getContent());
            result.put("totalElements", messages.getTotalElements());
            result.put("totalPages", messages.getTotalPages());
            result.put("currentPage", page);
            result.put("size", size);
            result.put("hasNext", messages.hasNext());
            result.put("hasPrevious", messages.hasPrevious());
            
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            logger.error("分页查询消息记录失败", e);
            Map<String, Object> errorResult = new HashMap<>();
            errorResult.put("error", "分页查询消息记录失败: " + e.getMessage());
            return ResponseEntity.internalServerError().body(errorResult);
        }
    }
    
    /**
     * 重试指定消息
     */
    @PostMapping("/retry/{messageId}")
    public ResponseEntity<Map<String, Object>> retryMessage(@PathVariable String messageId) {
        try {
            boolean success = messageIdempotentService.retryMessage(messageId);
            
            Map<String, Object> result = new HashMap<>();
            result.put("messageId", messageId);
            result.put("success", success);
            result.put("message", success ? "消息重试成功" : "消息重试失败");
            
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            logger.error("重试消息失败", e);
            Map<String, Object> errorResult = new HashMap<>();
            errorResult.put("error", "重试消息失败: " + e.getMessage());
            return ResponseEntity.internalServerError().body(errorResult);
        }
    }
    
    /**
     * 取消消息处理
     */
    @PostMapping("/cancel/{messageId}")
    public ResponseEntity<Map<String, Object>> cancelMessage(
            @PathVariable String messageId,
            @RequestParam(defaultValue = "手动取消") String reason) {
        try {
            boolean success = messageIdempotentService.cancelMessage(messageId, reason);
            
            Map<String, Object> result = new HashMap<>();
            result.put("messageId", messageId);
            result.put("success", success);
            result.put("reason", reason);
            result.put("message", success ? "消息取消成功" : "消息取消失败");
            
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            logger.error("取消消息失败", e);
            Map<String, Object> errorResult = new HashMap<>();
            errorResult.put("error", "取消消息失败: " + e.getMessage());
            return ResponseEntity.internalServerError().body(errorResult);
        }
    }
    
    /**
     * 获取可重试的消息队列
     */
    @GetMapping("/retry-queue")
    public ResponseEntity<Object> getRetryableMessages(@RequestParam(defaultValue = "50") int limit) {
        try {
            List<MessageRecord> messages = messageIdempotentService.getRetryableMessages(limit);
            
            Map<String, Object> result = new HashMap<>();
            result.put("count", messages.size());
            result.put("limit", limit);
            result.put("messages", messages);
            
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            logger.error("获取重试队列失败", e);
            Map<String, Object> errorResult = new HashMap<>();
            errorResult.put("error", "获取重试队列失败: " + e.getMessage());
            return ResponseEntity.internalServerError().body(errorResult);
        }
    }
    
    /**
     * 获取超时的消息
     */
    @GetMapping("/timeout-messages")
    public ResponseEntity<Object> getTimeoutMessages(@RequestParam(defaultValue = "30") int timeoutMinutes) {
        try {
            List<MessageRecord> messages = messageIdempotentService.getTimeoutMessages(timeoutMinutes);
            
            Map<String, Object> result = new HashMap<>();
            result.put("count", messages.size());
            result.put("timeoutMinutes", timeoutMinutes);
            result.put("messages", messages);
            
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            logger.error("获取超时消息失败", e);
            Map<String, Object> errorResult = new HashMap<>();
            errorResult.put("error", "获取超时消息失败: " + e.getMessage());
            return ResponseEntity.internalServerError().body(errorResult);
        }
    }
    
    /**
     * 处理超时消息
     */
    @PostMapping("/handle-timeout")
    public ResponseEntity<Map<String, Object>> handleTimeoutMessages(
            @RequestParam(defaultValue = "30") int timeoutMinutes) {
        try {
            messageIdempotentService.handleTimeoutMessages(timeoutMinutes);
            
            Map<String, Object> result = new HashMap<>();
            result.put("message", "超时消息处理完成");
            result.put("timeoutMinutes", timeoutMinutes);
            
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            logger.error("处理超时消息失败", e);
            Map<String, Object> errorResult = new HashMap<>();
            errorResult.put("error", "处理超时消息失败: " + e.getMessage());
            return ResponseEntity.internalServerError().body(errorResult);
        }
    }
    
    /**
     * 清理过期消息
     */
    @PostMapping("/cleanup")
    public ResponseEntity<Map<String, Object>> cleanupExpiredMessages(
            @RequestParam(defaultValue = "7") int retentionDays) {
        try {
            int deletedCount = messageIdempotentService.cleanupExpiredMessages(retentionDays);
            
            Map<String, Object> result = new HashMap<>();
            result.put("deletedCount", deletedCount);
            result.put("retentionDays", retentionDays);
            result.put("message", "过期消息清理完成");
            
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            logger.error("清理过期消息失败", e);
            Map<String, Object> errorResult = new HashMap<>();
            errorResult.put("error", "清理过期消息失败: " + e.getMessage());
            return ResponseEntity.internalServerError().body(errorResult);
        }
    }
    
    /**
     * 归档旧消息
     */
    @PostMapping("/archive")
    public ResponseEntity<Map<String, Object>> archiveOldMessages(
            @RequestParam(defaultValue = "3") int archiveDays) {
        try {
            int archivedCount = messageIdempotentService.archiveOldMessages(archiveDays);
            
            Map<String, Object> result = new HashMap<>();
            result.put("archivedCount", archivedCount);
            result.put("archiveDays", archiveDays);
            result.put("message", "旧消息归档完成");
            
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            logger.error("归档旧消息失败", e);
            Map<String, Object> errorResult = new HashMap<>();
            errorResult.put("error", "归档旧消息失败: " + e.getMessage());
            return ResponseEntity.internalServerError().body(errorResult);
        }
    }
    
    /**
     * 获取处理统计信息
     */
    @GetMapping("/statistics")
    public ResponseEntity<Object> getProcessingStatistics(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime) {
        try {
            Map<String, Object> statistics = messageIdempotentService.getProcessingStatistics(startTime, endTime);
            
            Map<String, Object> result = new HashMap<>();
            result.put("startTime", startTime);
            result.put("endTime", endTime);
            result.put("statistics", statistics);
            
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            logger.error("获取处理统计信息失败", e);
            Map<String, Object> errorResult = new HashMap<>();
            errorResult.put("error", "获取处理统计信息失败: " + e.getMessage());
            return ResponseEntity.internalServerError().body(errorResult);
        }
    }
    
    /**
     * 获取今日统计信息
     */
    @GetMapping("/statistics/today")
    public ResponseEntity<Object> getTodayStatistics() {
        try {
            Map<String, Object> statistics = messageIdempotentService.getTodayStatistics();
            return ResponseEntity.ok(statistics);
        } catch (Exception e) {
            logger.error("获取今日统计信息失败", e);
            Map<String, Object> errorResult = new HashMap<>();
            errorResult.put("error", "获取今日统计信息失败: " + e.getMessage());
            return ResponseEntity.internalServerError().body(errorResult);
        }
    }
    
    /**
     * 获取所有消息类型
     */
    @GetMapping("/message-types")
    public ResponseEntity<Object> getMessageTypes() {
        try {
            Map<String, Object> result = new HashMap<>();
            result.put("ORDER_CREATED", MessageType.ORDER_CREATED);
            result.put("ORDER_PAID", MessageType.ORDER_PAID);
            result.put("ORDER_CANCELLED", MessageType.ORDER_CANCELLED);
            result.put("ORDER_SHIPPED", MessageType.ORDER_SHIPPED);
            result.put("ORDER_DELIVERED", MessageType.ORDER_DELIVERED);
            result.put("ORDER_REFUNDED", MessageType.ORDER_REFUNDED);
            result.put("PAYMENT_CREATED", MessageType.PAYMENT_CREATED);
            result.put("PAYMENT_SUCCESS", MessageType.PAYMENT_SUCCESS);
            result.put("PAYMENT_FAILED", MessageType.PAYMENT_FAILED);
            result.put("PAYMENT_REFUND", MessageType.PAYMENT_REFUND);
            result.put("USER_REGISTERED", MessageType.USER_REGISTERED);
            result.put("INVENTORY_UPDATED", MessageType.INVENTORY_UPDATED);
            result.put("TEST_MESSAGE", MessageType.TEST_MESSAGE);
            
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            logger.error("获取消息类型失败", e);
            Map<String, Object> errorResult = new HashMap<>();
            errorResult.put("error", "获取消息类型失败: " + e.getMessage());
            return ResponseEntity.internalServerError().body(errorResult);
        }
    }
    
    /**
     * 获取所有处理状态
     */
    @GetMapping("/processing-statuses")
    public ResponseEntity<Object> getProcessingStatuses() {
        try {
            Map<String, Object> result = new HashMap<>();
            result.put("PENDING", ProcessingStatus.PENDING);
            result.put("PROCESSING", ProcessingStatus.PROCESSING);
            result.put("SUCCESS", ProcessingStatus.SUCCESS);
            result.put("FAILED", ProcessingStatus.FAILED);
            result.put("RETRYING", ProcessingStatus.RETRYING);
            result.put("RETRY_FAILED", ProcessingStatus.RETRY_FAILED);
            result.put("CANCELLED", ProcessingStatus.CANCELLED);
            result.put("TIMEOUT", ProcessingStatus.TIMEOUT);
            result.put("SKIPPED", ProcessingStatus.SKIPPED);
            
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            logger.error("获取处理状态失败", e);
            Map<String, Object> errorResult = new HashMap<>();
            errorResult.put("error", "获取处理状态失败: " + e.getMessage());
            return ResponseEntity.internalServerError().body(errorResult);
        }
    }
    
    /**
     * 发送测试消息
     */
    @PostMapping("/test/send")
    public ResponseEntity<Map<String, Object>> sendTestMessage(
            @RequestParam String businessKey,
            @RequestParam(defaultValue = "TEST_MESSAGE") MessageType messageType,
            @RequestParam(defaultValue = "这是一条测试消息") String content,
            @RequestParam(defaultValue = "false") boolean shouldFail) {
        try {
            Map<String, String> headers = new HashMap<>();
            headers.put("shouldFail", String.valueOf(shouldFail));
            headers.put("timestamp", String.valueOf(System.currentTimeMillis()));
            
            // 发送消息到D2K
            d2kTemplate.send("test-topic", businessKey, content);
            
            Map<String, Object> result = new HashMap<>();
            result.put("businessKey", businessKey);
            result.put("messageType", messageType);
            result.put("content", content);
            result.put("shouldFail", shouldFail);
            result.put("message", "测试消息发送成功");
            
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            logger.error("发送测试消息失败", e);
            Map<String, Object> errorResult = new HashMap<>();
            errorResult.put("error", "发送测试消息失败: " + e.getMessage());
            return ResponseEntity.internalServerError().body(errorResult);
        }
    }
    
    /**
     * 批量发送测试消息
     */
    @PostMapping("/test/batch-send")
    public ResponseEntity<Map<String, Object>> batchSendTestMessages(
            @RequestParam(defaultValue = "10") int count,
            @RequestParam(defaultValue = "0.1") double failureRate) {
        try {
            int successCount = 0;
            int failureCount = 0;
            Random random = new Random();
            
            for (int i = 0; i < count; i++) {
                try {
                    String businessKey = "test-batch-" + System.currentTimeMillis() + "-" + i;
                    String content = "批量测试消息 #" + (i + 1);
                    boolean shouldFail = random.nextDouble() < failureRate;
                    
                    Map<String, String> headers = new HashMap<>();
                    headers.put("shouldFail", String.valueOf(shouldFail));
                    headers.put("batchIndex", String.valueOf(i));
                    headers.put("timestamp", String.valueOf(System.currentTimeMillis()));
                    
                    // 发送消息到D2K
                    d2kTemplate.send("test-topic", businessKey, content);
                    successCount++;
                } catch (Exception e) {
                    failureCount++;
                    logger.warn("批量发送第{}条消息失败: {}", i + 1, e.getMessage());
                }
            }
            
            Map<String, Object> result = new HashMap<>();
            result.put("totalCount", count);
            result.put("successCount", successCount);
            result.put("failureCount", failureCount);
            result.put("failureRate", failureRate);
            result.put("message", "批量测试消息发送完成");
            
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            logger.error("批量发送测试消息失败", e);
            Map<String, Object> errorResult = new HashMap<>();
            errorResult.put("error", "批量发送测试消息失败: " + e.getMessage());
            return ResponseEntity.internalServerError().body(errorResult);
        }
    }
    
    /**
     * 健康检查
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> healthCheck() {
        try {
            Map<String, Object> health = new HashMap<>();
            health.put("status", "UP");
            health.put("timestamp", LocalDateTime.now());
            health.put("service", "MessageIdempotentService");
            health.put("version", "1.0.0");
            
            // 简单检查服务是否可用
            try {
                messageIdempotentService.getTodayStatistics();
                health.put("serviceHealthy", true);
                return ResponseEntity.ok(health);
            } catch (Exception serviceException) {
                health.put("status", "DOWN");
                health.put("serviceHealthy", false);
                health.put("serviceError", serviceException.getMessage());
                return ResponseEntity.status(503).body(health);
            }
        } catch (Exception e) {
            logger.error("健康检查失败", e);
            Map<String, Object> errorResult = new HashMap<>();
            errorResult.put("status", "DOWN");
            errorResult.put("error", "健康检查失败: " + e.getMessage());
            errorResult.put("timestamp", LocalDateTime.now());
            return ResponseEntity.status(503).body(errorResult);
        }
    }
}