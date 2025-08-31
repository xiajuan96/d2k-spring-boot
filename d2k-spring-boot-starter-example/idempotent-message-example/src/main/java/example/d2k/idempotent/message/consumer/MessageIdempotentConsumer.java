package example.d2k.idempotent.message.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import example.d2k.idempotent.message.entity.MessageRecord;
import example.d2k.idempotent.message.entity.MessageType;
import example.d2k.idempotent.message.service.MessageIdempotentService;
import com.d2k.spring.boot.autoconfigure.annotation.D2kListener;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 消息幂等性处理消费者
 * 演示如何使用D2K框架实现消息的幂等性处理
 * @author xiajuan96
 */
@Component
public class MessageIdempotentConsumer {
    
    private static final Logger logger = LoggerFactory.getLogger(MessageIdempotentConsumer.class);
    
    @Autowired
    private MessageIdempotentService messageIdempotentService;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    /**
     * 处理订单创建消息（幂等性处理）
     */
    @D2kListener(
        topic = "order-created-topic",
        groupId = "order-service-group",
        clientId = "order-consumer-1",
        concurrency = 3
    )
    public void handleOrderCreatedMessage(ConsumerRecord<String, String> record) {
        String messageId = extractMessageId(record);
        String businessKey = record.key();
        String content = record.value();
        
        logger.info("接收到订单创建消息: messageId={}, businessKey={}, content={}", messageId, businessKey, content);
        
        // 幂等性检查
        if (messageIdempotentService.isMessageProcessed(messageId)) {
            logger.info("消息已处理，跳过: messageId={}", messageId);
            return;
        }
        
        // 检查重复消息
        if (messageIdempotentService.isDuplicateMessage(businessKey, MessageType.ORDER_CREATED)) {
            logger.warn("检测到重复消息: businessKey={}, messageType={}", businessKey, MessageType.ORDER_CREATED);
            return;
        }
        
        // 创建消息记录
        Map<String, String> headers = extractHeaders(record);
        MessageRecord messageRecord = messageIdempotentService.createMessageRecord(
            messageId, businessKey, MessageType.ORDER_CREATED, content, headers, "order-service-group"
        );
        
        // 开始处理
        if (!messageIdempotentService.startProcessing(messageId)) {
            logger.warn("无法开始处理消息: messageId={}", messageId);
            return;
        }
        
        try {
            // 模拟订单创建业务逻辑
            processOrderCreation(content);
            
            // 标记处理成功
            messageIdempotentService.markProcessingSuccess(messageId, "订单创建成功");
            
        } catch (Exception e) {
            logger.error("处理订单创建消息失败: messageId={}", messageId, e);
            messageIdempotentService.markProcessingFailure(messageId, e.getMessage());
        }
    }
    
    /**
     * 处理支付成功消息（幂等性处理）
     */
    @D2kListener(
        topic = "payment-success-topic",
        groupId = "payment-service-group",
        clientId = "payment-consumer-1",
        concurrency = 5
    )
    public void handlePaymentSuccessMessage(ConsumerRecord<String, String> record) {
        String messageId = extractMessageId(record);
        String businessKey = record.key();
        String content = record.value();
        
        logger.info("接收到支付成功消息: messageId={}, businessKey={}, content={}", messageId, businessKey, content);
        
        // 幂等性检查
        if (messageIdempotentService.isMessageProcessed(messageId)) {
            logger.info("消息已处理，跳过: messageId={}", messageId);
            return;
        }
        
        // 创建消息记录
        Map<String, String> headers = extractHeaders(record);
        MessageRecord messageRecord = messageIdempotentService.createMessageRecord(
            messageId, businessKey, MessageType.PAYMENT_SUCCESS, content, headers, "payment-service-group"
        );
        
        // 开始处理
        if (!messageIdempotentService.startProcessing(messageId)) {
            logger.warn("无法开始处理消息: messageId={}", messageId);
            return;
        }
        
        try {
            // 模拟支付成功业务逻辑
            processPaymentSuccess(content);
            
            // 标记处理成功
            messageIdempotentService.markProcessingSuccess(messageId, "支付处理成功");
            
        } catch (Exception e) {
            logger.error("处理支付成功消息失败: messageId={}", messageId, e);
            messageIdempotentService.markProcessingFailure(messageId, e.getMessage());
        }
    }
    
    /**
     * 处理库存预留消息（幂等性处理）
     */
    @D2kListener(
        topic = "inventory-reserved-topic",
        groupId = "inventory-service-group",
        clientId = "inventory-consumer-1",
        concurrency = 2
    )
    public void handleInventoryReservedMessage(ConsumerRecord<String, String> record) {
        String messageId = extractMessageId(record);
        String businessKey = record.key();
        String content = record.value();
        
        logger.info("接收到库存预留消息: messageId={}, businessKey={}, content={}", messageId, businessKey, content);
        
        // 幂等性检查
        if (messageIdempotentService.isMessageProcessed(messageId)) {
            logger.info("消息已处理，跳过: messageId={}", messageId);
            return;
        }
        
        // 创建消息记录
        Map<String, String> headers = extractHeaders(record);
        MessageRecord messageRecord = messageIdempotentService.createMessageRecord(
            messageId, businessKey, MessageType.INVENTORY_RESERVED, content, headers, "inventory-service-group"
        );
        
        // 开始处理
        if (!messageIdempotentService.startProcessing(messageId)) {
            logger.warn("无法开始处理消息: messageId={}", messageId);
            return;
        }
        
        try {
            // 模拟库存预留业务逻辑
            processInventoryReservation(content);
            
            // 标记处理成功
            messageIdempotentService.markProcessingSuccess(messageId, "库存预留成功");
            
        } catch (Exception e) {
            logger.error("处理库存预留消息失败: messageId={}", messageId, e);
            messageIdempotentService.markProcessingFailure(messageId, e.getMessage());
        }
    }
    
    /**
     * 处理用户注册消息（非幂等性要求）
     */
    @D2kListener(
        topic = "user-registered-topic",
        groupId = "user-service-group",
        clientId = "user-consumer-1",
        concurrency = 1
    )
    public void handleUserRegisteredMessage(ConsumerRecord<String, String> record) {
        String messageId = extractMessageId(record);
        String businessKey = record.key();
        String content = record.value();
        
        logger.info("接收到用户注册消息: messageId={}, businessKey={}, content={}", messageId, businessKey, content);
        
        // 创建消息记录（非关键业务，不需要严格幂等性）
        Map<String, String> headers = extractHeaders(record);
        MessageRecord messageRecord = messageIdempotentService.createMessageRecord(
            messageId, businessKey, MessageType.USER_REGISTERED, content, headers, "user-service-group"
        );
        
        // 开始处理
        if (!messageIdempotentService.startProcessing(messageId)) {
            logger.warn("无法开始处理消息: messageId={}", messageId);
            return;
        }
        
        try {
            // 模拟用户注册后续处理
            processUserRegistration(content);
            
            // 标记处理成功
            messageIdempotentService.markProcessingSuccess(messageId, "用户注册处理成功");
            
        } catch (Exception e) {
            logger.error("处理用户注册消息失败: messageId={}", messageId, e);
            messageIdempotentService.markProcessingFailure(messageId, e.getMessage());
        }
    }
    
    /**
     * 批量处理通知消息
     */
    @D2kListener(
        topic = "notification-topic",
        groupId = "notification-service-group",
        clientId = "notification-consumer-1",
        concurrency = 1
    )
    public void handleNotificationMessages(List<ConsumerRecord<String, String>> records) {
        logger.info("批量接收到通知消息: count={}", records.size());
        
        for (ConsumerRecord<String, String> record : records) {
            String messageId = extractMessageId(record);
            String businessKey = record.key();
            String content = record.value();
            
            try {
                // 创建消息记录
                Map<String, String> headers = extractHeaders(record);
                MessageRecord messageRecord = messageIdempotentService.createMessageRecord(
                    messageId, businessKey, MessageType.EMAIL_NOTIFICATION, content, headers, "notification-service-group"
                );
                
                // 开始处理
                if (messageIdempotentService.startProcessing(messageId)) {
                    // 模拟通知发送
                    processNotification(content);
                    messageIdempotentService.markProcessingSuccess(messageId, "通知发送成功");
                } else {
                    logger.warn("无法开始处理通知消息: messageId={}", messageId);
                }
                
            } catch (Exception e) {
                logger.error("处理通知消息失败: messageId={}", messageId, e);
                messageIdempotentService.markProcessingFailure(messageId, e.getMessage());
            }
        }
    }
    
    /**
     * 处理重试消息
     */
    @D2kListener(
        topic = "message-retry-topic",
        groupId = "retry-service-group",
        clientId = "retry-consumer-1",
        concurrency = 2
    )
    public void handleRetryMessages(ConsumerRecord<String, String> record) {
        String messageId = extractMessageId(record);
        
        logger.info("接收到重试消息: messageId={}", messageId);
        
        try {
            // 尝试重试消息
            if (messageIdempotentService.retryMessage(messageId)) {
                logger.info("消息重试成功: messageId={}", messageId);
            } else {
                logger.warn("消息重试失败: messageId={}", messageId);
            }
        } catch (Exception e) {
            logger.error("处理重试消息异常: messageId={}", messageId, e);
        }
    }
    
    /**
     * 处理测试消息（用于演示）
     */
    @D2kListener(
        topic = "test-message-topic",
        groupId = "test-service-group",
        clientId = "test-consumer-1",
        concurrency = 1
    )
    public void handleTestMessage(ConsumerRecord<String, String> record) {
        String messageId = extractMessageId(record);
        String businessKey = record.key();
        String content = record.value();
        
        logger.info("接收到测试消息: messageId={}, businessKey={}, content={}", messageId, businessKey, content);
        
        // 幂等性检查
        if (messageIdempotentService.isMessageProcessed(messageId)) {
            logger.info("测试消息已处理，跳过: messageId={}", messageId);
            return;
        }
        
        // 创建消息记录
        Map<String, String> headers = extractHeaders(record);
        MessageRecord messageRecord = messageIdempotentService.createMessageRecord(
            messageId, businessKey, MessageType.TEST_MESSAGE, content, headers, "test-service-group"
        );
        
        // 开始处理
        if (!messageIdempotentService.startProcessing(messageId)) {
            logger.warn("无法开始处理测试消息: messageId={}", messageId);
            return;
        }
        
        try {
            // 模拟测试业务逻辑
            processTestMessage(content);
            
            // 标记处理成功
            messageIdempotentService.markProcessingSuccess(messageId, "测试消息处理成功");
            
        } catch (Exception e) {
            logger.error("处理测试消息失败: messageId={}", messageId, e);
            messageIdempotentService.markProcessingFailure(messageId, e.getMessage());
        }
    }
    
    // 私有辅助方法
    
    /**
     * 提取消息ID
     */
    private String extractMessageId(ConsumerRecord<String, String> record) {
        // 优先从消息头获取
        if (record.headers() != null) {
            org.apache.kafka.common.header.Header messageIdHeader = record.headers().lastHeader("messageId");
            if (messageIdHeader != null && messageIdHeader.value() != null) {
                return new String(messageIdHeader.value());
            }
        }
        
        // 如果没有消息头，生成一个基于分区、偏移量的唯一ID
        return String.format("%s-%d-%d", record.topic(), record.partition(), record.offset());
    }
    
    /**
     * 提取消息头信息
     */
    private Map<String, String> extractHeaders(ConsumerRecord<String, String> record) {
        Map<String, String> headers = new HashMap<>();
        
        if (record.headers() != null) {
            record.headers().forEach(header -> {
                if (header.value() != null) {
                    headers.put(header.key(), new String(header.value()));
                }
            });
        }
        
        // 添加一些基本信息
        headers.put("topic", record.topic());
        headers.put("partition", String.valueOf(record.partition()));
        headers.put("offset", String.valueOf(record.offset()));
        headers.put("timestamp", String.valueOf(record.timestamp()));
        
        return headers;
    }
    
    // 模拟业务处理方法
    
    private void processOrderCreation(String content) throws Exception {
        logger.info("处理订单创建业务逻辑: {}", content);
        
        // 模拟业务处理时间
        Thread.sleep(100);
        
        // 模拟随机失败（10%概率）
        if (Math.random() < 0.1) {
            throw new RuntimeException("订单创建失败：库存不足");
        }
        
        logger.info("订单创建业务处理完成");
    }
    
    private void processPaymentSuccess(String content) throws Exception {
        logger.info("处理支付成功业务逻辑: {}", content);
        
        // 模拟业务处理时间
        Thread.sleep(200);
        
        // 模拟随机失败（5%概率）
        if (Math.random() < 0.05) {
            throw new RuntimeException("支付处理失败：系统异常");
        }
        
        logger.info("支付成功业务处理完成");
    }
    
    private void processInventoryReservation(String content) throws Exception {
        logger.info("处理库存预留业务逻辑: {}", content);
        
        // 模拟业务处理时间
        Thread.sleep(300);
        
        // 模拟随机失败（15%概率）
        if (Math.random() < 0.15) {
            throw new RuntimeException("库存预留失败：库存不足");
        }
        
        logger.info("库存预留业务处理完成");
    }
    
    private void processUserRegistration(String content) throws Exception {
        logger.info("处理用户注册业务逻辑: {}", content);
        
        // 模拟业务处理时间
        Thread.sleep(50);
        
        // 模拟随机失败（3%概率）
        if (Math.random() < 0.03) {
            throw new RuntimeException("用户注册处理失败：邮件发送失败");
        }
        
        logger.info("用户注册业务处理完成");
    }
    
    private void processNotification(String content) throws Exception {
        logger.info("处理通知业务逻辑: {}", content);
        
        // 模拟业务处理时间
        Thread.sleep(30);
        
        // 模拟随机失败（2%概率）
        if (Math.random() < 0.02) {
            throw new RuntimeException("通知发送失败：网络异常");
        }
        
        logger.info("通知业务处理完成");
    }
    
    private void processTestMessage(String content) throws Exception {
        logger.info("处理测试消息业务逻辑: {}", content);
        
        // 模拟业务处理时间
        Thread.sleep(50);
        
        // 根据消息内容决定是否失败
        if (content != null && content.contains("fail")) {
            throw new RuntimeException("测试消息处理失败：模拟异常");
        }
        
        logger.info("测试消息业务处理完成");
    }
}