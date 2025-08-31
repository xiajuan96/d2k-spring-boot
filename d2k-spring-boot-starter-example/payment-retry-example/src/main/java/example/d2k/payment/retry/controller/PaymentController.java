package example.d2k.payment.retry.controller;

import example.d2k.payment.retry.entity.NotificationStatus;
import example.d2k.payment.retry.entity.PaymentNotification;
import example.d2k.payment.retry.entity.PaymentStatus;
import example.d2k.payment.retry.service.PaymentNotificationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 支付控制器
 * @author xiajuan96
 */
@RestController
@RequestMapping("/api/payment")
public class PaymentController {
    
    private static final Logger logger = LoggerFactory.getLogger(PaymentController.class);
    
    @Autowired
    private PaymentNotificationService paymentNotificationService;
    
    /**
     * 创建支付通知
     */
    @PostMapping("/notification")
    public ResponseEntity<Map<String, Object>> createNotification(
            @RequestParam String paymentId,
            @RequestParam String orderNo,
            @RequestParam BigDecimal amount,
            @RequestParam PaymentStatus status,
            @RequestParam String callbackUrl,
            @RequestParam(required = false) String paymentMethod,
            @RequestParam(required = false) String transactionId) {
        
        logger.info("创建支付通知请求: paymentId={}, orderNo={}, amount={}, status={}, callbackUrl={}", 
                   paymentId, orderNo, amount, status, callbackUrl);
        
        try {
            PaymentNotification notification = paymentNotificationService.createNotification(
                paymentId, orderNo, amount, status, callbackUrl, paymentMethod, transactionId);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "支付通知创建成功");
            response.put("data", notification);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("创建支付通知失败: {}", paymentId, e);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "创建支付通知失败: " + e.getMessage());
            
            return ResponseEntity.badRequest().body(response);
        }
    }
    
    /**
     * 更新支付状态
     */
    @PutMapping("/status")
    public ResponseEntity<Map<String, Object>> updatePaymentStatus(
            @RequestParam String paymentId,
            @RequestParam PaymentStatus status,
            @RequestParam(required = false) String transactionId) {
        
        logger.info("更新支付状态请求: paymentId={}, status={}, transactionId={}", 
                   paymentId, status, transactionId);
        
        try {
            boolean success = paymentNotificationService.updatePaymentStatus(paymentId, status, transactionId);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", success);
            response.put("message", success ? "支付状态更新成功" : "支付状态更新失败");
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("更新支付状态失败: {}", paymentId, e);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "更新支付状态失败: " + e.getMessage());
            
            return ResponseEntity.badRequest().body(response);
        }
    }
    
    /**
     * 手动触发支付通知重试
     */
    @PostMapping("/retry/{paymentId}")
    public ResponseEntity<Map<String, Object>> retryNotification(@PathVariable String paymentId) {
        logger.info("手动触发支付通知重试: {}", paymentId);
        
        try {
            boolean success = paymentNotificationService.handleNotificationRetry(paymentId);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", success);
            response.put("message", success ? "支付通知重试成功" : "支付通知重试失败");
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("手动触发支付通知重试失败: {}", paymentId, e);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "支付通知重试失败: " + e.getMessage());
            
            return ResponseEntity.badRequest().body(response);
        }
    }
    
    /**
     * 查询支付通知详情
     */
    @GetMapping("/notification/{paymentId}")
    public ResponseEntity<Map<String, Object>> getNotification(@PathVariable String paymentId) {
        logger.info("查询支付通知详情: {}", paymentId);
        
        try {
            Optional<PaymentNotification> notificationOpt = paymentNotificationService.findByPaymentId(paymentId);
            
            Map<String, Object> response = new HashMap<>();
            if (notificationOpt.isPresent()) {
                response.put("success", true);
                response.put("message", "查询成功");
                response.put("data", notificationOpt.get());
            } else {
                response.put("success", false);
                response.put("message", "支付通知记录不存在");
            }
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("查询支付通知详情失败: {}", paymentId, e);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "查询失败: " + e.getMessage());
            
            return ResponseEntity.badRequest().body(response);
        }
    }
    
    /**
     * 查询订单的所有支付通知
     */
    @GetMapping("/notifications/order/{orderNo}")
    public ResponseEntity<Map<String, Object>> getNotificationsByOrder(@PathVariable String orderNo) {
        logger.info("查询订单支付通知: {}", orderNo);
        
        try {
            List<PaymentNotification> notifications = paymentNotificationService.findByOrderNo(orderNo);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "查询成功");
            response.put("data", notifications);
            response.put("count", notifications.size());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("查询订单支付通知失败: {}", orderNo, e);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "查询失败: " + e.getMessage());
            
            return ResponseEntity.badRequest().body(response);
        }
    }
    
    /**
     * 查询需要重试的通知
     */
    @GetMapping("/notifications/retry")
    public ResponseEntity<Map<String, Object>> getNotificationsToRetry() {
        logger.info("查询需要重试的支付通知");
        
        try {
            List<PaymentNotification> notifications = paymentNotificationService.findNotificationsToRetry();
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "查询成功");
            response.put("data", notifications);
            response.put("count", notifications.size());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("查询需要重试的支付通知失败", e);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "查询失败: " + e.getMessage());
            
            return ResponseEntity.badRequest().body(response);
        }
    }
    
    /**
     * 查询失败的通知
     */
    @GetMapping("/notifications/failed")
    public ResponseEntity<Map<String, Object>> getFailedNotifications() {
        logger.info("查询失败的支付通知");
        
        try {
            List<PaymentNotification> notifications = paymentNotificationService.findFailedNotifications();
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "查询成功");
            response.put("data", notifications);
            response.put("count", notifications.size());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("查询失败的支付通知失败", e);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "查询失败: " + e.getMessage());
            
            return ResponseEntity.badRequest().body(response);
        }
    }
    
    /**
     * 放弃支付通知
     */
    @PostMapping("/abandon/{paymentId}")
    public ResponseEntity<Map<String, Object>> abandonNotification(
            @PathVariable String paymentId,
            @RequestParam String reason) {
        
        logger.info("放弃支付通知: {}, 原因: {}", paymentId, reason);
        
        try {
            boolean success = paymentNotificationService.abandonNotification(paymentId, reason);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", success);
            response.put("message", success ? "支付通知已放弃" : "放弃支付通知失败");
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("放弃支付通知失败: {}", paymentId, e);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "放弃支付通知失败: " + e.getMessage());
            
            return ResponseEntity.badRequest().body(response);
        }
    }
    
    /**
     * 获取支付状态枚举
     */
    @GetMapping("/status/enum")
    public ResponseEntity<Map<String, Object>> getPaymentStatusEnum() {
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "获取成功");
        response.put("data", PaymentStatus.values());
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * 获取通知状态枚举
     */
    @GetMapping("/notification/status/enum")
    public ResponseEntity<Map<String, Object>> getNotificationStatusEnum() {
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "获取成功");
        response.put("data", NotificationStatus.values());
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * 健康检查
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> health() {
        Map<String, Object> response = new HashMap<>();
        response.put("status", "UP");
        response.put("timestamp", LocalDateTime.now());
        response.put("service", "payment-retry-example");
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * 模拟商户回调接口（用于测试）
     */
    @PostMapping("/callback/mock")
    public ResponseEntity<Map<String, Object>> mockCallback(@RequestBody Map<String, Object> callbackData) {
        logger.info("模拟商户回调接收到数据: {}", callbackData);
        
        // 模拟处理逻辑
        String paymentId = (String) callbackData.get("paymentId");
        String status = (String) callbackData.get("status");
        
        Map<String, Object> response = new HashMap<>();
        
        // 模拟随机成功/失败（用于测试重试机制）
        boolean success = Math.random() > 0.3; // 70% 成功率
        
        if (success) {
            response.put("code", "SUCCESS");
            response.put("message", "回调处理成功");
            logger.info("模拟商户回调处理成功: paymentId={}, status={}", paymentId, status);
        } else {
            response.put("code", "FAILED");
            response.put("message", "回调处理失败");
            logger.warn("模拟商户回调处理失败: paymentId={}, status={}", paymentId, status);
        }
        
        response.put("timestamp", System.currentTimeMillis());
        
        return ResponseEntity.ok(response);
    }
}