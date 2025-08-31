package example.d2k.order.timeout.consumer;

import com.d2k.spring.boot.autoconfigure.annotation.D2kListener;
import example.d2k.order.timeout.service.OrderService;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * 订单超时消息消费者
 * @author xiajuan96
 */
@Component
public class OrderTimeoutConsumer {
    
    private static final Logger logger = LoggerFactory.getLogger(OrderTimeoutConsumer.class);
    
    @Autowired
    private OrderService orderService;
    
    /**
     * 处理订单超时消息
     */
    @D2kListener(
        topic = "order-timeout",
        groupId = "order-timeout-group",
        clientId = "order-timeout-consumer",
        concurrency = 3,
        autoStartup = true,
        asyncProcessing = true,
        asyncCorePoolSize = 2,
        asyncMaxPoolSize = 5
    )
    public void handleOrderTimeout(ConsumerRecord<String, String> record) {
        String orderNo = record.value();
        String key = record.key();
        
        logger.info("收到订单超时消息 - Topic: {}, Partition: {}, Offset: {}, Key: {}, OrderNo: {}", 
                   record.topic(), record.partition(), record.offset(), key, orderNo);
        
        try {
            // 处理订单超时
            boolean success = orderService.handleOrderTimeout(orderNo);
            
            if (success) {
                logger.info("订单超时处理成功: {}", orderNo);
            } else {
                logger.info("订单超时处理跳过: {} (订单状态已变更或其他原因)", orderNo);
            }
            
        } catch (Exception e) {
            logger.error("订单超时处理失败: {}", orderNo, e);
            // 这里可以考虑重试机制或者发送到死信队列
            throw e; // 重新抛出异常，让Kafka进行重试
        }
    }
    
    /**
     * 处理订单超时消息（简化版本，只接收消息内容）
     */
    @D2kListener(
        topic = "order-timeout-simple",
        groupId = "order-timeout-simple-group",
        clientId = "order-timeout-simple-consumer"
    )
    public void handleOrderTimeoutSimple(String orderNo) {
        logger.info("收到简化订单超时消息: {}", orderNo);
        
        try {
            orderService.handleOrderTimeout(orderNo);
            logger.info("简化订单超时处理完成: {}", orderNo);
        } catch (Exception e) {
            logger.error("简化订单超时处理失败: {}", orderNo, e);
            throw e;
        }
    }
    
    /**
     * 处理订单超时消息（带键值对）
     */
    @D2kListener(
        topic = "order-timeout-with-key",
        groupId = "order-timeout-key-group",
        clientId = "order-timeout-key-consumer",
        concurrency = 2
    )
    public void handleOrderTimeoutWithKey(ConsumerRecord<String, String> record) {
        String key = record.key();
        String orderNo = record.value();
        
        logger.info("收到带键值的订单超时消息 - Topic: {}, Partition: {}, Offset: {}, Key: {}, OrderNo: {}", 
                   record.topic(), record.partition(), record.offset(), key, orderNo);
        
        try {
            // 可以根据key进行一些额外的处理逻辑
            if (key != null && key.equals(orderNo)) {
                orderService.handleOrderTimeout(orderNo);
                logger.info("带键值订单超时处理完成: {}", orderNo);
            } else {
                logger.warn("订单超时消息键值不匹配 - Key: {}, OrderNo: {}", key, orderNo);
            }
        } catch (Exception e) {
            logger.error("带键值订单超时处理失败: {}", orderNo, e);
            throw e;
        }
    }
}