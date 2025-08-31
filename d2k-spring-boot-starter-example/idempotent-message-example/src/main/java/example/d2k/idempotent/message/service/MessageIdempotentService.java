package example.d2k.idempotent.message.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import example.d2k.idempotent.message.entity.MessageRecord;
import example.d2k.idempotent.message.entity.MessageType;
import example.d2k.idempotent.message.entity.ProcessingStatus;
import example.d2k.idempotent.message.repository.MessageRecordRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 消息幂等性处理服务
 * 负责消息的幂等性检查、记录管理、重试处理等核心功能
 * @author xiajuan96
 */
@Service
@Transactional
public class MessageIdempotentService {
    
    private static final Logger logger = LoggerFactory.getLogger(MessageIdempotentService.class);
    
    @Autowired
    private MessageRecordRepository messageRecordRepository;
    
    @Autowired
    private RedisTemplate<String, Object> redisTemplate;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    // Redis缓存键前缀
    private static final String CACHE_PREFIX = "message:idempotent:";
    private static final String PROCESSING_LOCK_PREFIX = "message:processing:";
    private static final String RETRY_QUEUE_PREFIX = "message:retry:";
    private static final String STATS_PREFIX = "message:stats:";
    
    // 缓存过期时间
    private static final long CACHE_EXPIRE_HOURS = 24;
    private static final long PROCESSING_LOCK_EXPIRE_MINUTES = 30;
    private static final long STATS_CACHE_EXPIRE_MINUTES = 10;
    
    /**
     * 检查消息是否已处理（幂等性检查）
     */
    public boolean isMessageProcessed(String messageId) {
        if (!StringUtils.hasText(messageId)) {
            return false;
        }
        
        // 先检查Redis缓存
        String cacheKey = CACHE_PREFIX + messageId;
        Boolean cached = (Boolean) redisTemplate.opsForValue().get(cacheKey);
        if (cached != null) {
            logger.debug("从缓存获取消息处理状态: messageId={}, processed={}", messageId, cached);
            return cached;
        }
        
        // 检查数据库
        Optional<MessageRecord> record = messageRecordRepository.findByMessageId(messageId);
        boolean processed = record.isPresent() && record.get().getStatus().isSuccess();
        
        // 缓存结果
        redisTemplate.opsForValue().set(cacheKey, processed, CACHE_EXPIRE_HOURS, TimeUnit.HOURS);
        
        logger.debug("从数据库获取消息处理状态: messageId={}, processed={}", messageId, processed);
        return processed;
    }
    
    /**
     * 检查消息是否重复
     */
    public boolean isDuplicateMessage(String businessKey, MessageType messageType) {
        if (!StringUtils.hasText(businessKey) || messageType == null) {
            return false;
        }
        
        return messageRecordRepository.existsByBusinessKeyAndMessageType(businessKey, messageType);
    }
    
    /**
     * 创建消息记录
     */
    public MessageRecord createMessageRecord(String messageId, String businessKey, MessageType messageType, 
                                           String content, Map<String, String> headers, String consumerGroup) {
        MessageRecord record = new MessageRecord();
        record.setMessageId(messageId);
        record.setBusinessKey(businessKey);
        record.setMessageType(messageType);
        record.setMessageContent(content);
        record.setConsumerGroup(consumerGroup);
        record.setStatus(ProcessingStatus.PENDING);
        record.setRetryCount(0);
        record.setCreatedTime(LocalDateTime.now());
        record.setUpdatedTime(LocalDateTime.now());
        
        // 设置消息头信息
        if (headers != null && !headers.isEmpty()) {
            record.setHeaders(headers);
        }
        
        MessageRecord saved = messageRecordRepository.save(record);
        logger.info("创建消息记录: messageId={}, businessKey={}, messageType={}", messageId, businessKey, messageType);
        
        // 更新缓存
        updateMessageCache(messageId, false);
        
        return saved;
    }
    
    /**
     * 开始处理消息
     */
    public boolean startProcessing(String messageId) {
        // 获取处理锁
        String lockKey = PROCESSING_LOCK_PREFIX + messageId;
        Boolean lockAcquired = redisTemplate.opsForValue().setIfAbsent(lockKey, "processing", PROCESSING_LOCK_EXPIRE_MINUTES, TimeUnit.MINUTES);
        
        if (!Boolean.TRUE.equals(lockAcquired)) {
            logger.warn("获取消息处理锁失败，消息可能正在被其他实例处理: messageId={}", messageId);
            return false;
        }
        
        // 更新数据库状态
        Optional<MessageRecord> recordOpt = messageRecordRepository.findByMessageId(messageId);
        if (recordOpt.isPresent()) {
            MessageRecord record = recordOpt.get();
            if (record.getStatus().canTransitionTo(ProcessingStatus.PROCESSING)) {
                record.setStatus(ProcessingStatus.PROCESSING);
                record.setFirstProcessedTime(LocalDateTime.now());
                record.setUpdatedTime(LocalDateTime.now());
                messageRecordRepository.save(record);
                
                logger.info("开始处理消息: messageId={}", messageId);
                return true;
            } else {
                logger.warn("消息状态不允许开始处理: messageId={}, currentStatus={}", messageId, record.getStatus());
                // 释放锁
                redisTemplate.delete(lockKey);
                return false;
            }
        } else {
            logger.error("消息记录不存在: messageId={}", messageId);
            // 释放锁
            redisTemplate.delete(lockKey);
            return false;
        }
    }
    
    /**
     * 标记消息处理成功
     */
    public void markProcessingSuccess(String messageId, String result) {
        Optional<MessageRecord> recordOpt = messageRecordRepository.findByMessageId(messageId);
        if (recordOpt.isPresent()) {
            MessageRecord record = recordOpt.get();
            record.setStatus(ProcessingStatus.SUCCESS);
            record.setProcessResult(result);
            record.setCompletedTime(LocalDateTime.now());
            record.setUpdatedTime(LocalDateTime.now());
            messageRecordRepository.save(record);
            
            // 更新缓存
            updateMessageCache(messageId, true);
            
            // 释放处理锁
            releaseProcessingLock(messageId);
            
            // 更新统计信息
            updateProcessingStats(record.getMessageType(), true);
            
            logger.info("消息处理成功: messageId={}", messageId);
        } else {
            logger.error("消息记录不存在，无法标记处理成功: messageId={}", messageId);
        }
    }
    
    /**
     * 标记消息处理失败
     */
    public void markProcessingFailure(String messageId, String errorMessage) {
        Optional<MessageRecord> recordOpt = messageRecordRepository.findByMessageId(messageId);
        if (recordOpt.isPresent()) {
            MessageRecord record = recordOpt.get();
            record.setStatus(ProcessingStatus.FAILED);
            record.setErrorMessage(errorMessage);
            record.setCompletedTime(LocalDateTime.now());
            record.setUpdatedTime(LocalDateTime.now());
            
            // 设置下次重试时间
            if (record.canRetry()) {
                int retryIntervalSeconds = record.getMessageType().getRecommendedRetryIntervalSeconds();
                record.setNextRetryTime(LocalDateTime.now().plusSeconds(retryIntervalSeconds));
            }
            
            messageRecordRepository.save(record);
            
            // 释放处理锁
            releaseProcessingLock(messageId);
            
            // 更新统计信息
            updateProcessingStats(record.getMessageType(), false);
            
            logger.error("消息处理失败: messageId={}, error={}", messageId, errorMessage);
        } else {
            logger.error("消息记录不存在，无法标记处理失败: messageId={}", messageId);
        }
    }
    
    /**
     * 重试消息处理
     */
    public boolean retryMessage(String messageId) {
        Optional<MessageRecord> recordOpt = messageRecordRepository.findByMessageId(messageId);
        if (recordOpt.isPresent()) {
            MessageRecord record = recordOpt.get();
            
            if (!record.canRetry()) {
                logger.warn("消息不能重试: messageId={}, status={}, retryCount={}", 
                           messageId, record.getStatus(), record.getRetryCount());
                return false;
            }
            
            // 检查重试次数限制
            int maxRetries = record.getMessageType().getRecommendedRetryCount();
            if (record.getRetryCount() >= maxRetries) {
                record.setStatus(ProcessingStatus.RETRY_EXHAUSTED);
                record.setUpdatedTime(LocalDateTime.now());
                messageRecordRepository.save(record);
                
                logger.warn("消息重试次数已耗尽: messageId={}, retryCount={}, maxRetries={}", 
                           messageId, record.getRetryCount(), maxRetries);
                return false;
            }
            
            // 更新重试信息
            record.incrementRetryCount();
            record.setStatus(ProcessingStatus.RETRYING);
            record.setFirstProcessedTime(LocalDateTime.now());
            record.setUpdatedTime(LocalDateTime.now());
            record.setErrorMessage(null); // 清除之前的错误信息
            
            messageRecordRepository.save(record);
            
            logger.info("开始重试消息: messageId={}, retryCount={}", messageId, record.getRetryCount());
            return true;
        } else {
            logger.error("消息记录不存在，无法重试: messageId={}", messageId);
            return false;
        }
    }
    
    /**
     * 取消消息处理
     */
    public boolean cancelMessage(String messageId, String reason) {
        Optional<MessageRecord> recordOpt = messageRecordRepository.findByMessageId(messageId);
        if (recordOpt.isPresent()) {
            MessageRecord record = recordOpt.get();
            
            if (!record.getStatus().canCancel()) {
                logger.warn("消息状态不允许取消: messageId={}, status={}", messageId, record.getStatus());
                return false;
            }
            
            record.setStatus(ProcessingStatus.CANCELLED);
            record.setErrorMessage("取消原因: " + reason);
            record.setUpdatedTime(LocalDateTime.now());
            messageRecordRepository.save(record);
            
            // 释放处理锁
            releaseProcessingLock(messageId);
            
            logger.info("取消消息处理: messageId={}, reason={}", messageId, reason);
            return true;
        } else {
            logger.error("消息记录不存在，无法取消: messageId={}", messageId);
            return false;
        }
    }
    
    /**
     * 获取需要重试的消息
     */
    @Transactional(readOnly = true)
    public List<MessageRecord> getRetryableMessages(int limit) {
        List<ProcessingStatus> retryableStatuses = Arrays.asList(
            ProcessingStatus.FAILED, ProcessingStatus.RETRY_FAILED, ProcessingStatus.TIMEOUT
        );
        
        List<MessageRecord> messages = messageRecordRepository.findRetryableMessages(
            retryableStatuses, 10, LocalDateTime.now()
        );
        
        return messages.stream().limit(limit).collect(Collectors.toList());
    }
    
    /**
     * 获取超时的消息
     */
    @Transactional(readOnly = true)
    public List<MessageRecord> getTimeoutMessages(int timeoutMinutes) {
        List<ProcessingStatus> processingStatuses = Arrays.asList(
            ProcessingStatus.PROCESSING, ProcessingStatus.RETRYING
        );
        
        LocalDateTime timeoutThreshold = LocalDateTime.now().minusMinutes(timeoutMinutes);
        return messageRecordRepository.findTimeoutMessages(processingStatuses, timeoutThreshold);
    }
    
    /**
     * 处理超时消息
     */
    public void handleTimeoutMessages(int timeoutMinutes) {
        List<MessageRecord> timeoutMessages = getTimeoutMessages(timeoutMinutes);
        
        for (MessageRecord message : timeoutMessages) {
            message.setStatus(ProcessingStatus.TIMEOUT);
            message.setErrorMessage("处理超时，超过 " + timeoutMinutes + " 分钟");
            message.setCompletedTime(LocalDateTime.now());
            message.setUpdatedTime(LocalDateTime.now());
            
            // 设置下次重试时间
            if (message.canRetry()) {
                int retryIntervalSeconds = message.getMessageType().getRecommendedRetryIntervalSeconds();
                message.setNextRetryTime(LocalDateTime.now().plusSeconds(retryIntervalSeconds));
            }
            
            messageRecordRepository.save(message);
            
            // 释放处理锁
            releaseProcessingLock(message.getMessageId());
            
            logger.warn("处理超时消息: messageId={}, processingStartTime={}", 
                       message.getMessageId(), message.getFirstProcessedTime());
        }
    }
    
    /**
     * 清理过期消息
     */
    public int cleanupExpiredMessages(int retentionDays) {
        LocalDateTime expireTime = LocalDateTime.now().minusDays(retentionDays);
        List<ProcessingStatus> finalStatuses = Arrays.asList(ProcessingStatus.getFinalStatuses());
        
        int deletedCount = messageRecordRepository.deleteExpiredMessages(expireTime, finalStatuses);
        
        logger.info("清理过期消息完成: deletedCount={}, retentionDays={}", deletedCount, retentionDays);
        return deletedCount;
    }
    
    /**
     * 归档旧消息
     */
    public int archiveOldMessages(int archiveDays) {
        LocalDateTime archiveTime = LocalDateTime.now().minusDays(archiveDays);
        List<ProcessingStatus> finalStatuses = Arrays.asList(ProcessingStatus.getFinalStatuses());
        
        int archivedCount = messageRecordRepository.archiveOldMessages(archiveTime, finalStatuses, LocalDateTime.now());
        
        logger.info("归档旧消息完成: archivedCount={}, archiveDays={}", archivedCount, archiveDays);
        return archivedCount;
    }
    
    /**
     * 获取消息记录
     */
    @Transactional(readOnly = true)
    public Optional<MessageRecord> getMessageRecord(String messageId) {
        return messageRecordRepository.findByMessageId(messageId);
    }
    
    /**
     * 根据业务键获取消息记录
     */
    @Transactional(readOnly = true)
    public List<MessageRecord> getMessagesByBusinessKey(String businessKey) {
        return messageRecordRepository.findByBusinessKey(businessKey);
    }
    
    /**
     * 分页查询消息记录
     */
    @Transactional(readOnly = true)
    public Page<MessageRecord> getMessages(MessageType messageType, ProcessingStatus status, 
                                         String consumerGroup, LocalDateTime startTime, 
                                         LocalDateTime endTime, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return messageRecordRepository.findByMultipleConditions(
            messageType, status, consumerGroup, startTime, endTime, pageable
        );
    }
    
    /**
     * 获取处理统计信息
     */
    @Transactional(readOnly = true)
    public Map<String, Object> getProcessingStatistics(LocalDateTime startTime, LocalDateTime endTime) {
        Map<String, Object> stats = new HashMap<>();
        
        // 总消息数
        long totalCount = messageRecordRepository.countByCreatedTimeBetween(startTime, endTime);
        stats.put("totalCount", totalCount);
        
        // 成功率
        Double successRate = messageRecordRepository.calculateSuccessRate(startTime, endTime);
        stats.put("successRate", successRate != null ? successRate : 0.0);
        
        // 平均处理时间
        Double avgProcessingTime = messageRecordRepository.calculateAverageProcessingTime(startTime, endTime);
        stats.put("averageProcessingTimeMs", avgProcessingTime != null ? avgProcessingTime : 0.0);
        
        // 状态分布
        List<Object[]> statusDistribution = messageRecordRepository.getStatusDistribution(startTime, endTime);
        Map<String, Long> statusMap = new HashMap<>();
        for (Object[] row : statusDistribution) {
            statusMap.put(((ProcessingStatus) row[0]).getDisplayName(), (Long) row[1]);
        }
        stats.put("statusDistribution", statusMap);
        
        // 消息类型分布
        List<Object[]> typeDistribution = messageRecordRepository.getMessageTypeDistribution(startTime, endTime);
        Map<String, Long> typeMap = new HashMap<>();
        for (Object[] row : typeDistribution) {
            typeMap.put(((MessageType) row[0]).getDisplayName(), (Long) row[1]);
        }
        stats.put("messageTypeDistribution", typeMap);
        
        return stats;
    }
    
    /**
     * 获取今日统计信息
     */
    @Transactional(readOnly = true)
    public Map<String, Object> getTodayStatistics() {
        String cacheKey = STATS_PREFIX + "today:" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        
        @SuppressWarnings("unchecked")
        Map<String, Object> cached = (Map<String, Object>) redisTemplate.opsForValue().get(cacheKey);
        if (cached != null) {
            return cached;
        }
        
        Map<String, Object> stats = new HashMap<>();
        
        // 今日总消息数
        long todayCount = messageRecordRepository.countTodayMessages();
        stats.put("todayCount", todayCount);
        
        // 今日状态分布
        List<Object[]> todayStatusDistribution = messageRecordRepository.getTodayStatusDistribution();
        Map<String, Long> statusMap = new HashMap<>();
        for (Object[] row : todayStatusDistribution) {
            statusMap.put(((ProcessingStatus) row[0]).getDisplayName(), (Long) row[1]);
        }
        stats.put("todayStatusDistribution", statusMap);
        
        // 缓存结果
        redisTemplate.opsForValue().set(cacheKey, stats, STATS_CACHE_EXPIRE_MINUTES, TimeUnit.MINUTES);
        
        return stats;
    }
    
    // 私有辅助方法
    
    private void updateMessageCache(String messageId, boolean processed) {
        String cacheKey = CACHE_PREFIX + messageId;
        redisTemplate.opsForValue().set(cacheKey, processed, CACHE_EXPIRE_HOURS, TimeUnit.HOURS);
    }
    
    private void releaseProcessingLock(String messageId) {
        String lockKey = PROCESSING_LOCK_PREFIX + messageId;
        redisTemplate.delete(lockKey);
    }
    
    private void updateProcessingStats(MessageType messageType, boolean success) {
        String statsKey = STATS_PREFIX + "processing:" + messageType.name();
        String field = success ? "success" : "failure";
        redisTemplate.opsForHash().increment(statsKey, field, 1);
        redisTemplate.expire(statsKey, 1, TimeUnit.DAYS);
    }
}