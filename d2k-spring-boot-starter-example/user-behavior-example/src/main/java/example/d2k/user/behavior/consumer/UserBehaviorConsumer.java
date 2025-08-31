package example.d2k.user.behavior.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import example.d2k.user.behavior.service.UserBehaviorEventService;
import com.d2k.spring.boot.autoconfigure.annotation.D2kListener;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * 用户行为事件消费者
 * @author xiajuan96
 */
@Component
public class UserBehaviorConsumer {
    
    private static final Logger logger = LoggerFactory.getLogger(UserBehaviorConsumer.class);
    
    @Autowired
    private UserBehaviorEventService eventService;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    /**
     * 处理用户行为事件（接收完整的ConsumerRecord）
     */
    @D2kListener(
        topic = "user-behavior-processing",
        groupId = "user-behavior-processing-group",
        clientId = "user-behavior-processing-client",
        concurrency = 3

    )
    public void handleUserBehaviorEvent(ConsumerRecord<String, Object> record) {
        try {
            String eventId = record.key();
            Object messageValue = record.value();
            
            logger.info("接收到用户行为事件处理消息: eventId={}, partition={}, offset={}", 
                       eventId, record.partition(), record.offset());
            
            // 解析消息内容
            Map<String, Object> message = parseMessage(messageValue);
            String action = (String) message.get("action");
            
            if ("process".equals(action)) {
                // 处理事件
                eventService.processEvent(eventId);
                logger.info("用户行为事件处理完成: eventId={}", eventId);
            } else {
                logger.warn("未知的处理动作: eventId={}, action={}", eventId, action);
            }
            
            
        } catch (Exception e) {
            logger.error("处理用户行为事件消息失败: key={}", record.key(), e);
            throw e; // 重新抛出异常以触发重试机制
        }
    }
    
    /**
     * 处理用户行为事件（只接收消息内容）
     */
    @D2kListener(
        topic = "user-behavior-simple-processing",
        groupId = "user-behavior-simple-group",
        clientId = "user-behavior-simple-client",
        concurrency = 2
    
    )
    public void handleSimpleUserBehaviorEvent( Object message) {
        try {
            logger.info("接收到简单用户行为事件消息: {}", message);
            
            // 解析消息内容
            Map<String, Object> messageMap = parseMessage(message);
            String eventId = (String) messageMap.get("eventId");
            String action = (String) messageMap.get("action");
            
            if ("process".equals(action) && eventId != null) {
                eventService.processEvent(eventId);
                logger.info("简单用户行为事件处理完成: eventId={}", eventId);
            } else {
                logger.warn("无效的简单事件消息: eventId={}, action={}", eventId, action);
            }
            
        } catch (Exception e) {
            logger.error("处理简单用户行为事件消息失败: message={}", message, e);
            throw e;
        }
    }
    
 
    
    /**
     * 处理延迟用户行为事件
     */
    @D2kListener(
        topic = "user-behavior-delayed-processing",
        groupId = "user-behavior-delayed-group",
        clientId = "user-behavior-delayed-client",
        concurrency = 2
    
    )
    public void handleDelayedUserBehaviorEvent(ConsumerRecord<String, Object> record) {
        try {
            String eventId = record.key();
            Object messageValue = record.value();
            
            logger.info("接收到延迟用户行为事件处理消息: eventId={}, partition={}, offset={}", 
                       eventId, record.partition(), record.offset());
            
            // 解析消息内容
            Map<String, Object> message = parseMessage(messageValue);
            String action = (String) message.get("action");
            
            if ("delayed_process".equals(action)) {
                // 处理延迟事件
                eventService.processEvent(eventId);
                logger.info("延迟用户行为事件处理完成: eventId={}", eventId);
            } else {
                logger.warn("未知的延迟处理动作: eventId={}, action={}", eventId, action);
            }
            
        } catch (Exception e) {
            logger.error("处理延迟用户行为事件消息失败: key={}", record.key(), e);
            throw e;
        }
    }
    
    /**
     * 处理用户行为事件重试
     */
    @D2kListener(
        topic = "user-behavior-retry",
        groupId = "user-behavior-retry-group",
        clientId = "user-behavior-retry-client",
        concurrency = 2
      
    )
    public void handleUserBehaviorEventRetry(ConsumerRecord<String, Object> record) {
        try {
            String eventId = record.key();
            Object messageValue = record.value();
            
            logger.info("接收到用户行为事件重试消息: eventId={}, partition={}, offset={}", 
                       eventId, record.partition(), record.offset());
            
            // 解析消息内容
            Map<String, Object> message = parseMessage(messageValue);
            String action = (String) message.get("action");
            Integer retryCount = (Integer) message.get("retryCount");
            
            if ("retry".equals(action)) {
                logger.info("开始重试用户行为事件: eventId={}, retryCount={}", eventId, retryCount);
                
                // 处理重试事件
                eventService.processEvent(eventId);
                logger.info("用户行为事件重试完成: eventId={}, retryCount={}", eventId, retryCount);
            } else {
                logger.warn("未知的重试动作: eventId={}, action={}", eventId, action);
            }
            
    
        } catch (Exception e) {
            logger.error("处理用户行为事件重试消息失败: key={}", record.key(), e);
            throw e;
        }
    }
    
    /**
     * 批量处理用户行为事件
     */
    @D2kListener(
        topic = "user-behavior-batch-processing",
        groupId = "user-behavior-batch-group",
        clientId = "user-behavior-batch-client",
        concurrency = 1
     
    )
    public void handleBatchUserBehaviorEvents(Object message ) {
        try {
            logger.info("接收到批量用户行为事件处理消息: {}", message);
            
            // 解析消息内容
            Map<String, Object> messageMap = parseMessage(message);
            String action = (String) messageMap.get("action");
            
            if ("batch_process".equals(action)) {
                // 执行批量处理
                eventService.batchProcessEvents();
                logger.info("批量用户行为事件处理完成");
            } else {
                logger.warn("未知的批量处理动作: action={}", action);
            }
            
          
            
        } catch (Exception e) {
            logger.error("处理批量用户行为事件消息失败: message={}", message, e);
            throw e;
        }
    }
    
    /**
     * 处理延迟事件检查
     */
    @D2kListener(
        topic = "user-behavior-delayed-check",
        groupId = "user-behavior-delayed-check-group",
        clientId = "user-behavior-delayed-check-client",
        concurrency = 1
      
    )
    public void handleDelayedEventCheck(Object message) {
        try {
            logger.info("接收到延迟事件检查消息: {}", message);
            
            // 解析消息内容
            Map<String, Object> messageMap = parseMessage(message);
            String action = (String) messageMap.get("action");
            
            if ("check_delayed".equals(action)) {
                // 处理延迟事件
                eventService.processDelayedEvents();
                logger.info("延迟事件检查处理完成");
            } else {
                logger.warn("未知的延迟检查动作: action={}", action);
            }
            
        } catch (Exception e) {
            logger.error("处理延迟事件检查消息失败: message={}", message, e);
            throw e;
        }
    }
    
    /**
     * 处理重试事件检查
     */
    @D2kListener(
        topic = "user-behavior-retry-check",
        groupId = "user-behavior-retry-check-group",
        clientId = "user-behavior-retry-check-client",
        concurrency = 1

    )
    public void handleRetryEventCheck(Object message) {
        try {
            logger.info("接收到重试事件检查消息: {}", message);
            
            // 解析消息内容
            Map<String, Object> messageMap = parseMessage(message);
            String action = (String) messageMap.get("action");
            
            if ("check_retry".equals(action)) {
                // 处理重试事件
                eventService.processRetryEvents();
                logger.info("重试事件检查处理完成");
            } else {
                logger.warn("未知的重试检查动作: action={}", action);
            }
            
        } catch (Exception e) {
            logger.error("处理重试事件检查消息失败: message={}", message, e);
            throw e;
        }
    }
    
    /**
     * 处理事件清理
     */
    @D2kListener(
        topic = "user-behavior-cleanup",
        groupId = "user-behavior-cleanup-group",
        clientId = "user-behavior-cleanup-client",
        concurrency = 1
       
    )
    public void handleEventCleanup( Object message) {
        try {
            logger.info("接收到事件清理消息: {}", message);
            
            // 解析消息内容
            Map<String, Object> messageMap = parseMessage(message);
            String action = (String) messageMap.get("action");
            
            if ("cleanup".equals(action)) {
                // 执行事件清理
                eventService.cleanupExpiredEvents();
                logger.info("事件清理处理完成");
            } else {
                logger.warn("未知的清理动作: action={}", action);
            }
            
        } catch (Exception e) {
            logger.error("处理事件清理消息失败: message={}", message, e);
            throw e;
        }
    }
    
    /**
     * 处理用户行为分析
     */
    @D2kListener(
        topic = "user-behavior-analysis",
        groupId = "user-behavior-analysis-group",
        clientId = "user-behavior-analysis-client",
        concurrency = 2

    )
    public void handleUserBehaviorAnalysis(ConsumerRecord<String, Object> record) {
        try {
            String userId = record.key();
            Object messageValue = record.value();
            
            logger.info("接收到用户行为分析消息: userId={}, partition={}, offset={}", 
                       userId, record.partition(), record.offset());
            
            // 解析消息内容
            Map<String, Object> message = parseMessage(messageValue);
            String action = (String) message.get("action");
            
            if ("analyze".equals(action)) {
                // 执行用户行为分析
                performUserBehaviorAnalysis(userId, message);
                logger.info("用户行为分析完成: userId={}", userId);
            } else {
                logger.warn("未知的分析动作: userId={}, action={}", userId, action);
            }

            
        } catch (Exception e) {
            logger.error("处理用户行为分析消息失败: key={}", record.key(), e);
            throw e;
        }
    }
    

    /**
     * 解析消息内容
     */
    @SuppressWarnings("unchecked")
    private Map<String, Object> parseMessage(Object message) {
        try {
            if (message instanceof Map) {
                return (Map<String, Object>) message;
            } else if (message instanceof String) {
                return objectMapper.readValue((String) message, Map.class);
            } else {
                // 尝试将对象转换为JSON字符串再解析
                String jsonString = objectMapper.writeValueAsString(message);
                return objectMapper.readValue(jsonString, Map.class);
            }
        } catch (Exception e) {
            logger.error("解析消息内容失败: message={}", message, e);
            throw new RuntimeException("解析消息内容失败", e);
        }
    }
    
    /**
     * 执行用户行为分析
     */
    private void performUserBehaviorAnalysis(String userId, Map<String, Object> message) {
        try {
            logger.info("开始执行用户行为分析: userId={}", userId);
            
            // 获取用户统计信息
            Map<String, Object> userStats = eventService.getUserStatistics(userId);
            logger.info("用户统计信息: userId={}, stats={}", userId, userStats);
            
            // 这里可以添加更复杂的用户行为分析逻辑
            // 例如：用户偏好分析、行为模式识别、异常行为检测等
            
            // 分析用户最近的行为事件
            List<example.d2k.user.behavior.entity.UserBehaviorEvent> recentEvents = eventService.findEventsByUser(userId, 0, 10).getContent();
            logger.info("用户最近事件数量: userId={}, count={}", userId, recentEvents.size());
            
            logger.info("用户行为分析完成: userId={}", userId);
            
        } catch (Exception e) {
            logger.error("执行用户行为分析失败: userId={}", userId, e);
            throw e;
        }
    }
    
    /**
     * 执行实时处理
     */
    private void performRealtimeProcessing(String eventId, Map<String, Object> message) {
        try {
            logger.info("开始执行实时处理: eventId={}", eventId);
            
            // 实时处理逻辑（简化版本）
            String userId = (String) message.get("userId");
            String eventType = (String) message.get("eventType");
            
            if (userId != null && eventType != null) {
                logger.info("实时处理事件: eventId={}, userId={}, eventType={}", eventId, userId, eventType);
                
                // 这里可以添加实时处理逻辑
                // 例如：实时推荐、实时告警、实时统计等
            }
            
            logger.info("实时处理完成: eventId={}", eventId);
            
        } catch (Exception e) {
            logger.error("执行实时处理失败: eventId={}", eventId, e);
            throw e;
        }
    }
}