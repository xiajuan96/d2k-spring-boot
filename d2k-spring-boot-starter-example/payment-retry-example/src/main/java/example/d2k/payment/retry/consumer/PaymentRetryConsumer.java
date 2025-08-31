package example.d2k.payment.retry.consumer;

import com.d2k.spring.boot.autoconfigure.annotation.D2kListener;
import example.d2k.payment.retry.service.PaymentNotificationService;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * 支付通知重试消费者
 * @author xiajuan96
 */
@Component
public class PaymentRetryConsumer {
    
    private static final Logger logger = LoggerFactory.getLogger(PaymentRetryConsumer.class);
    
    @Autowired
    private PaymentNotificationService paymentNotificationService;
    
    /**
     * 处理支付通知重试消息（接收完整的ConsumerRecord）
     */
    @D2kListener(
        topic = "payment-notification-retry",
        groupId = "payment-retry-group",
        clientId = "payment-retry-consumer-1",
        concurrency = 3,
        asyncProcessing = true
    )
    public void handlePaymentRetryWithRecord(ConsumerRecord<String, String> record) {
        String paymentId = record.value();
        String key = record.key();
        long offset = record.offset();
        int partition = record.partition();
        
        logger.info("接收到支付通知重试消息 [Record]: topic={}, partition={}, offset={}, key={}, paymentId={}", 
                   record.topic(), partition, offset, key, paymentId);
        
        try {
            // 处理支付通知重试
            boolean success = paymentNotificationService.handleNotificationRetry(paymentId);
            
            if (success) {
                logger.info("支付通知重试处理成功 [Record]: paymentId={}, partition={}, offset={}", 
                           paymentId, partition, offset);
            } else {
                logger.warn("支付通知重试处理失败 [Record]: paymentId={}, partition={}, offset={}", 
                           paymentId, partition, offset);
            }
            
        } catch (Exception e) {
            logger.error("支付通知重试处理异常 [Record]: paymentId={}, partition={}, offset={}", 
                        paymentId, partition, offset, e);
            throw e; // 重新抛出异常，触发重试机制
        }
    }
    
    /**
     * 处理支付通知重试消息（只接收消息内容）
     */
    @D2kListener(
        topic = "payment-notification-retry",
        groupId = "payment-retry-simple-group",
        clientId = "payment-retry-consumer-2",
        concurrency = 2,
        asyncProcessing = false
    )
    public void handlePaymentRetrySimple(String paymentId) {
        logger.info("接收到支付通知重试消息 [Simple]: paymentId={}", paymentId);
        
        try {
            // 处理支付通知重试
            boolean success = paymentNotificationService.handleNotificationRetry(paymentId);
            
            if (success) {
                logger.info("支付通知重试处理成功 [Simple]: paymentId={}", paymentId);
            } else {
                logger.warn("支付通知重试处理失败 [Simple]: paymentId={}", paymentId);
            }
            
        } catch (Exception e) {
            logger.error("支付通知重试处理异常 [Simple]: paymentId={}", paymentId, e);
            throw e; // 重新抛出异常，触发重试机制
        }
    }
    
    /**
     * 处理支付通知重试消息（接收键和值）
     */
    @D2kListener(
        topic = "payment-notification-retry",
        groupId = "payment-retry-kv-group",
        clientId = "payment-retry-consumer-3",
        concurrency = 1,
        asyncProcessing = true
    )
    public void handlePaymentRetryWithKeyValue(String key, String paymentId) {
        logger.info("接收到支付通知重试消息 [KeyValue]: key={}, paymentId={}", key, paymentId);
        
        try {
            // 验证key和paymentId是否一致（业务校验）
            if (key != null && !key.equals(paymentId)) {
                logger.warn("支付通知重试消息key与paymentId不一致: key={}, paymentId={}", key, paymentId);
            }
            
            // 处理支付通知重试
            boolean success = paymentNotificationService.handleNotificationRetry(paymentId);
            
            if (success) {
                logger.info("支付通知重试处理成功 [KeyValue]: key={}, paymentId={}", key, paymentId);
            } else {
                logger.warn("支付通知重试处理失败 [KeyValue]: key={}, paymentId={}", key, paymentId);
            }
            
        } catch (Exception e) {
            logger.error("支付通知重试处理异常 [KeyValue]: key={}, paymentId={}", key, paymentId, e);
            throw e; // 重新抛出异常，触发重试机制
        }
    }
    
    /**
     * 处理支付状态变更通知（用于演示不同topic的处理）
     */
    @D2kListener(
        topic = "payment-status-change",
        groupId = "payment-status-group",
        clientId = "payment-status-consumer",
        concurrency = 2,
        asyncProcessing = true
    )
    public void handlePaymentStatusChange(String message) {
        logger.info("接收到支付状态变更消息: {}", message);
        
        try {
            // 这里可以解析消息并处理支付状态变更
            // 例如：解析JSON格式的消息，提取paymentId和新状态
            // 然后调用paymentNotificationService.updatePaymentStatus()
            
            logger.info("支付状态变更处理完成: {}", message);
            
        } catch (Exception e) {
            logger.error("支付状态变更处理异常: {}", message, e);
            throw e;
        }
    }
    
    /**
     * 处理批量支付通知重试（用于演示批量处理）
     */
    @D2kListener(
        topic = "payment-batch-retry",
        groupId = "payment-batch-group",
        clientId = "payment-batch-consumer",
        concurrency = 1,
        asyncProcessing = false
    )
    public void handleBatchPaymentRetry(String batchMessage) {
        logger.info("接收到批量支付通知重试消息: {}", batchMessage);
        
        try {
            // 这里可以解析批量消息，例如逗号分隔的paymentId列表
            String[] paymentIds = batchMessage.split(",");
            
            int successCount = 0;
            int failCount = 0;
            
            for (String paymentId : paymentIds) {
                paymentId = paymentId.trim();
                if (!paymentId.isEmpty()) {
                    try {
                        boolean success = paymentNotificationService.handleNotificationRetry(paymentId);
                        if (success) {
                            successCount++;
                        } else {
                            failCount++;
                        }
                    } catch (Exception e) {
                        logger.error("批量处理中单个支付通知重试失败: {}", paymentId, e);
                        failCount++;
                    }
                }
            }
            
            logger.info("批量支付通知重试处理完成: 总数={}, 成功={}, 失败={}", 
                       paymentIds.length, successCount, failCount);
            
        } catch (Exception e) {
            logger.error("批量支付通知重试处理异常: {}", batchMessage, e);
            throw e;
        }
    }
}