package example.d2k.payment.retry.service;

import com.d2k.spring.boot.autoconfigure.template.D2kTemplate;
import example.d2k.payment.retry.entity.NotificationStatus;
import example.d2k.payment.retry.entity.PaymentNotification;
import example.d2k.payment.retry.entity.PaymentStatus;
import example.d2k.payment.retry.repository.PaymentNotificationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 支付通知服务类
 * @author xiajuan96
 */
@Service
@Transactional
public class PaymentNotificationService {
    
    private static final Logger logger = LoggerFactory.getLogger(PaymentNotificationService.class);
    
    @Autowired
    private PaymentNotificationRepository notificationRepository;
    
    @Autowired
    private D2kTemplate d2kTemplate;
    
    @Autowired
    private WebClient.Builder webClientBuilder;
    
    // 重试间隔配置（秒）
    private static final int[] RETRY_INTERVALS = {30, 60, 300, 900, 1800}; // 30s, 1m, 5m, 15m, 30m
    
    /**
     * 创建支付通知记录
     */
    public PaymentNotification createNotification(String paymentId, String orderNo, 
                                                 BigDecimal amount, PaymentStatus status, 
                                                 String callbackUrl) {
        return createNotification(paymentId, orderNo, amount, status, callbackUrl, null, null);
    }
    
    /**
     * 创建支付通知记录（完整版）
     */
    public PaymentNotification createNotification(String paymentId, String orderNo, 
                                                 BigDecimal amount, PaymentStatus status, 
                                                 String callbackUrl, String paymentMethod, 
                                                 String transactionId) {
        // 检查是否已存在
        Optional<PaymentNotification> existingOpt = notificationRepository.findByPaymentId(paymentId);
        if (existingOpt.isPresent()) {
            logger.warn("支付通知记录已存在: {}", paymentId);
            return existingOpt.get();
        }
        
        // 创建新记录
        PaymentNotification notification = new PaymentNotification(paymentId, orderNo, amount, status, callbackUrl);
        notification.setPaymentMethod(paymentMethod);
        notification.setTransactionId(transactionId);
        
        // 保存记录
        notification = notificationRepository.save(notification);
        
        // 如果是需要通知的状态，立即发送通知
        if (status.needsNotification()) {
            sendNotificationMessage(notification, 0); // 立即发送
        }
        
        logger.info("支付通知记录创建成功: {}, 状态: {}", paymentId, status);
        return notification;
    }
    
    /**
     * 更新支付状态并触发通知
     */
    public boolean updatePaymentStatus(String paymentId, PaymentStatus newStatus, String transactionId) {
        Optional<PaymentNotification> notificationOpt = notificationRepository.findByPaymentId(paymentId);
        if (!notificationOpt.isPresent()) {
            logger.warn("支付通知记录不存在: {}", paymentId);
            return false;
        }
        
        PaymentNotification notification = notificationOpt.get();
        PaymentStatus oldStatus = notification.getStatus();
        
        // 更新状态
        notification.setStatus(newStatus);
        if (transactionId != null) {
            notification.setTransactionId(transactionId);
        }
        
        // 如果状态变为需要通知的状态，重置通知状态并发送通知
        if (newStatus.needsNotification() && oldStatus != newStatus) {
            notification.setNotificationStatus(NotificationStatus.PENDING);
            notification.setRetryCount(0);
            notification.setErrorMessage(null);
            
            notificationRepository.save(notification);
            
            // 发送通知消息
            sendNotificationMessage(notification, 0);
        } else {
            notificationRepository.save(notification);
        }
        
        logger.info("支付状态更新成功: {}, {} -> {}", paymentId, oldStatus, newStatus);
        return true;
    }
    
    /**
     * 处理支付通知重试
     */
    public boolean handleNotificationRetry(String paymentId) {
        Optional<PaymentNotification> notificationOpt = notificationRepository.findByPaymentId(paymentId);
        if (!notificationOpt.isPresent()) {
            logger.warn("处理重试时支付通知记录不存在: {}", paymentId);
            return false;
        }
        
        PaymentNotification notification = notificationOpt.get();
        
        // 检查是否可以重试
        if (!notification.canRetry()) {
            logger.info("支付通知无需重试: {}, 状态: {}, 重试次数: {}/{}", 
                       paymentId, notification.getNotificationStatus(), 
                       notification.getRetryCount(), notification.getMaxRetryCount());
            return false;
        }
        
        // 更新状态为重试中
        notification.setNotificationStatus(NotificationStatus.RETRYING);
        notification.incrementRetryCount();
        
        try {
            // 执行HTTP回调
            boolean success = executeHttpCallback(notification);
            
            if (success) {
                // 通知成功
                notification.setNotificationStatus(NotificationStatus.SUCCESS);
                notification.setErrorMessage(null);
                logger.info("支付通知重试成功: {}, 重试次数: {}", paymentId, notification.getRetryCount());
            } else {
                // 通知失败，检查是否还能重试
                if (notification.canRetry()) {
                    notification.setNotificationStatus(NotificationStatus.FAILED);
                    // 计算下次重试时间
                    scheduleNextRetry(notification);
                    logger.warn("支付通知重试失败: {}, 重试次数: {}/{}, 将在 {} 后重试", 
                               paymentId, notification.getRetryCount(), notification.getMaxRetryCount(),
                               notification.getNextRetryTime());
                } else {
                    // 达到最大重试次数
                    notification.setNotificationStatus(NotificationStatus.RETRY_FAILED);
                    logger.error("支付通知重试失败，已达到最大重试次数: {}, 重试次数: {}", 
                                paymentId, notification.getRetryCount());
                }
            }
            
        } catch (Exception e) {
            logger.error("支付通知重试异常: {}", paymentId, e);
            notification.setNotificationStatus(NotificationStatus.FAILED);
            notification.setErrorMessage(e.getMessage());
            
            // 如果还能重试，安排下次重试
            if (notification.canRetry()) {
                scheduleNextRetry(notification);
            } else {
                notification.setNotificationStatus(NotificationStatus.RETRY_FAILED);
            }
        }
        
        notificationRepository.save(notification);
        return notification.getNotificationStatus() == NotificationStatus.SUCCESS;
    }
    
    /**
     * 执行HTTP回调
     */
    private boolean executeHttpCallback(PaymentNotification notification) {
        try {
            // 构建回调数据
            Map<String, Object> callbackData = buildCallbackData(notification);
            
            // 发送HTTP请求
            WebClient webClient = webClientBuilder.build();
            
            String response = webClient.post()
                    .uri(notification.getCallbackUrl())
                    .bodyValue(callbackData)
                    .retrieve()
                    .bodyToMono(String.class)
                    .timeout(java.time.Duration.ofSeconds(30))
                    .block();
            
            // 检查响应（这里简化处理，实际应根据业务规则判断）
            boolean success = response != null && (response.contains("success") || response.contains("SUCCESS"));
            
            if (!success) {
                notification.setErrorMessage("回调响应异常: " + response);
            }
            
            logger.debug("HTTP回调执行完成: {}, URL: {}, 响应: {}, 成功: {}", 
                        notification.getPaymentId(), notification.getCallbackUrl(), response, success);
            
            return success;
            
        } catch (Exception e) {
            logger.error("HTTP回调执行失败: {}, URL: {}", 
                        notification.getPaymentId(), notification.getCallbackUrl(), e);
            notification.setErrorMessage("HTTP回调异常: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * 构建回调数据
     */
    private Map<String, Object> buildCallbackData(PaymentNotification notification) {
        Map<String, Object> data = new HashMap<>();
        data.put("paymentId", notification.getPaymentId());
        data.put("orderNo", notification.getOrderNo());
        data.put("amount", notification.getAmount());
        data.put("status", notification.getStatus().name());
        data.put("paymentMethod", notification.getPaymentMethod());
        data.put("transactionId", notification.getTransactionId());
        data.put("timestamp", System.currentTimeMillis());
        
        // 这里可以添加签名等安全措施
        // data.put("signature", generateSignature(data));
        
        return data;
    }
    
    /**
     * 发送通知消息
     */
    private void sendNotificationMessage(PaymentNotification notification, long delaySeconds) {
        try {
            long delayMs = delaySeconds * 1000L;
            
            d2kTemplate.send(
                "payment-notification-retry",
                notification.getPaymentId(),
                notification.getPaymentId()
            );
            
            logger.info("支付通知消息发送成功: {}, 延迟: {}秒", 
                       notification.getPaymentId(), delaySeconds);
        } catch (Exception e) {
            logger.error("支付通知消息发送失败: {}", notification.getPaymentId(), e);
        }
    }
    
    /**
     * 安排下次重试
     */
    private void scheduleNextRetry(PaymentNotification notification) {
        int retryIndex = Math.min(notification.getRetryCount() - 1, RETRY_INTERVALS.length - 1);
        int delaySeconds = RETRY_INTERVALS[retryIndex];
        
        // 设置下次重试时间
        notification.setNextRetryTime(LocalDateTime.now().plusSeconds(delaySeconds));
        
        // 发送延迟消息
        sendNotificationMessage(notification, delaySeconds);
    }
    
    /**
     * 查找支付通知记录
     */
    public Optional<PaymentNotification> findByPaymentId(String paymentId) {
        return notificationRepository.findByPaymentId(paymentId);
    }
    
    /**
     * 查找订单的通知记录
     */
    public List<PaymentNotification> findByOrderNo(String orderNo) {
        return notificationRepository.findByOrderNoOrderByCreateTimeDesc(orderNo);
    }
    
    /**
     * 查找需要重试的通知
     */
    public List<PaymentNotification> findNotificationsToRetry() {
        return notificationRepository.findNotificationsToRetry(LocalDateTime.now());
    }
    
    /**
     * 查找失败的通知
     */
    public List<PaymentNotification> findFailedNotifications() {
        return notificationRepository.findFailedNotifications();
    }
    
    /**
     * 放弃通知（手动操作）
     */
    public boolean abandonNotification(String paymentId, String reason) {
        Optional<PaymentNotification> notificationOpt = notificationRepository.findByPaymentId(paymentId);
        if (!notificationOpt.isPresent()) {
            return false;
        }
        
        PaymentNotification notification = notificationOpt.get();
        notification.setNotificationStatus(NotificationStatus.ABANDONED);
        notification.setErrorMessage("手动放弃: " + reason);
        
        notificationRepository.save(notification);
        
        logger.info("支付通知已放弃: {}, 原因: {}", paymentId, reason);
        return true;
    }
}