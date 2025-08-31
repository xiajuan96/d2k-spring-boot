package example.d2k.idempotent.message.repository;

import example.d2k.idempotent.message.entity.MessageRecord;
import example.d2k.idempotent.message.entity.MessageType;
import example.d2k.idempotent.message.entity.ProcessingStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 消息记录Repository接口
 * 提供消息记录的数据访问操作
 * @author xiajuan96
 */
@Repository
public interface MessageRecordRepository extends JpaRepository<MessageRecord, Long> {
    
    /**
     * 根据消息ID查找消息记录
     */
    Optional<MessageRecord> findByMessageId(String messageId);
    
    /**
     * 根据业务键查找消息记录
     */
    List<MessageRecord> findByBusinessKey(String businessKey);
    
    /**
     * 根据业务键和消息类型查找消息记录
     */
    Optional<MessageRecord> findByBusinessKeyAndMessageType(String businessKey, MessageType messageType);
    
    /**
     * 根据消息类型查找消息记录
     */
    List<MessageRecord> findByMessageType(MessageType messageType);
    
    /**
     * 根据处理状态查找消息记录
     */
    List<MessageRecord> findByStatus(ProcessingStatus status);
    
    /**
     * 根据消费者组查找消息记录
     */
    List<MessageRecord> findByConsumerGroup(String consumerGroup);
    
    /**
     * 根据处理状态和消费者组查找消息记录
     */
    List<MessageRecord> findByStatusAndConsumerGroup(ProcessingStatus status, String consumerGroup);
    
    /**
     * 根据时间范围查找消息记录
     */
    List<MessageRecord> findByCreatedTimeBetween(LocalDateTime startTime, LocalDateTime endTime);
    
    /**
     * 根据处理状态和时间范围查找消息记录
     */
    List<MessageRecord> findByStatusAndCreatedTimeBetween(ProcessingStatus status, LocalDateTime startTime, LocalDateTime endTime);
    
    /**
     * 根据消息类型和时间范围查找消息记录
     */
    List<MessageRecord> findByMessageTypeAndCreatedTimeBetween(MessageType messageType, LocalDateTime startTime, LocalDateTime endTime);
    
    /**
     * 查找需要重试的消息记录
     */
    @Query("SELECT m FROM MessageRecord m WHERE m.status IN :retryableStatuses AND m.retryCount < :maxRetryCount AND m.nextRetryTime <= :currentTime")
    List<MessageRecord> findRetryableMessages(@Param("retryableStatuses") List<ProcessingStatus> retryableStatuses, 
                                            @Param("maxRetryCount") int maxRetryCount, 
                                            @Param("currentTime") LocalDateTime currentTime);
    
    /**
     * 查找超时的消息记录
     */
    @Query("SELECT m FROM MessageRecord m WHERE m.status IN :processingStatuses AND m.processingStartTime IS NOT NULL AND m.processingStartTime < :timeoutThreshold")
    List<MessageRecord> findTimeoutMessages(@Param("processingStatuses") List<ProcessingStatus> processingStatuses, 
                                          @Param("timeoutThreshold") LocalDateTime timeoutThreshold);
    
    /**
     * 查找重试次数超过限制的消息记录
     */
    @Query("SELECT m FROM MessageRecord m WHERE m.retryCount >= :maxRetryCount AND m.status IN :failedStatuses")
    List<MessageRecord> findRetryExhaustedMessages(@Param("maxRetryCount") int maxRetryCount, 
                                                  @Param("failedStatuses") List<ProcessingStatus> failedStatuses);
    
    /**
     * 查找最近的消息记录
     */
    List<MessageRecord> findTop10ByOrderByCreatedTimeDesc();
    
    /**
     * 查找最近失败的消息记录
     */
    @Query("SELECT m FROM MessageRecord m WHERE m.status IN :failedStatuses ORDER BY m.updatedTime DESC")
    List<MessageRecord> findRecentFailedMessages(@Param("failedStatuses") List<ProcessingStatus> failedStatuses, Pageable pageable);
    
    /**
     * 统计消息总数
     */
    long count();
    
    /**
     * 根据状态统计消息数量
     */
    long countByStatus(ProcessingStatus status);
    
    /**
     * 根据消息类型统计消息数量
     */
    long countByMessageType(MessageType messageType);
    
    /**
     * 根据消费者组统计消息数量
     */
    long countByConsumerGroup(String consumerGroup);
    
    /**
     * 统计时间范围内的消息数量
     */
    long countByCreatedTimeBetween(LocalDateTime startTime, LocalDateTime endTime);
    
    /**
     * 统计处理成功率
     */
    @Query("SELECT CAST(COUNT(CASE WHEN m.status = 'SUCCESS' THEN 1 END) AS double) / COUNT(*) FROM MessageRecord m WHERE m.createdTime BETWEEN :startTime AND :endTime")
    Double calculateSuccessRate(@Param("startTime") LocalDateTime startTime, @Param("endTime") LocalDateTime endTime);
    
    /**
     * 统计平均处理时间（毫秒）
     */
    @Query("SELECT AVG(TIMESTAMPDIFF(MICROSECOND, m.processingStartTime, m.processingEndTime) / 1000) FROM MessageRecord m WHERE m.status = 'SUCCESS' AND m.processingStartTime IS NOT NULL AND m.processingEndTime IS NOT NULL AND m.createdTime BETWEEN :startTime AND :endTime")
    Double calculateAverageProcessingTime(@Param("startTime") LocalDateTime startTime, @Param("endTime") LocalDateTime endTime);
    
    /**
     * 统计各状态的消息数量分布
     */
    @Query("SELECT m.status, COUNT(m) FROM MessageRecord m WHERE m.createdTime BETWEEN :startTime AND :endTime GROUP BY m.status")
    List<Object[]> getStatusDistribution(@Param("startTime") LocalDateTime startTime, @Param("endTime") LocalDateTime endTime);
    
    /**
     * 统计各消息类型的数量分布
     */
    @Query("SELECT m.messageType, COUNT(m) FROM MessageRecord m WHERE m.createdTime BETWEEN :startTime AND :endTime GROUP BY m.messageType")
    List<Object[]> getMessageTypeDistribution(@Param("startTime") LocalDateTime startTime, @Param("endTime") LocalDateTime endTime);
    
    /**
     * 统计各消费者组的处理情况
     */
    @Query("SELECT m.consumerGroup, m.status, COUNT(m) FROM MessageRecord m WHERE m.createdTime BETWEEN :startTime AND :endTime GROUP BY m.consumerGroup, m.status")
    List<Object[]> getConsumerGroupStatistics(@Param("startTime") LocalDateTime startTime, @Param("endTime") LocalDateTime endTime);
    
    /**
     * 查找重复的消息记录
     */
    @Query("SELECT m.businessKey, m.messageType, COUNT(m) FROM MessageRecord m GROUP BY m.businessKey, m.messageType HAVING COUNT(m) > 1")
    List<Object[]> findDuplicateMessages();
    
    /**
     * 查找处理时间最长的消息记录
     */
    @Query("SELECT m FROM MessageRecord m WHERE m.processingStartTime IS NOT NULL AND m.processingEndTime IS NOT NULL ORDER BY (TIMESTAMPDIFF(MICROSECOND, m.processingStartTime, m.processingEndTime)) DESC")
    List<MessageRecord> findLongestProcessingMessages(Pageable pageable);
    
    /**
     * 查找重试次数最多的消息记录
     */
    List<MessageRecord> findTop10ByOrderByRetryCountDesc();
    
    /**
     * 批量更新消息状态
     */
    @Modifying
    @Query("UPDATE MessageRecord m SET m.status = :newStatus, m.updatedTime = :updateTime WHERE m.id IN :ids")
    int batchUpdateStatus(@Param("ids") List<Long> ids, @Param("newStatus") ProcessingStatus newStatus, @Param("updateTime") LocalDateTime updateTime);
    
    /**
     * 批量更新重试信息
     */
    @Modifying
    @Query("UPDATE MessageRecord m SET m.retryCount = m.retryCount + 1, m.nextRetryTime = :nextRetryTime, m.updatedTime = :updateTime WHERE m.id IN :ids")
    int batchUpdateRetryInfo(@Param("ids") List<Long> ids, @Param("nextRetryTime") LocalDateTime nextRetryTime, @Param("updateTime") LocalDateTime updateTime);
    
    /**
     * 删除过期的消息记录
     */
    @Modifying
    @Query("DELETE FROM MessageRecord m WHERE m.createdTime < :expireTime AND m.status IN :finalStatuses")
    int deleteExpiredMessages(@Param("expireTime") LocalDateTime expireTime, @Param("finalStatuses") List<ProcessingStatus> finalStatuses);
    
    /**
     * 归档旧的消息记录
     */
    @Modifying
    @Query("UPDATE MessageRecord m SET m.status = 'ARCHIVED', m.updatedTime = :updateTime WHERE m.createdTime < :archiveTime AND m.status IN :finalStatuses AND m.status != 'ARCHIVED'")
    int archiveOldMessages(@Param("archiveTime") LocalDateTime archiveTime, @Param("finalStatuses") List<ProcessingStatus> finalStatuses, @Param("updateTime") LocalDateTime updateTime);
    
    /**
     * 检查消息是否存在
     */
    boolean existsByMessageId(String messageId);
    
    /**
     * 检查业务键和消息类型的组合是否存在
     */
    boolean existsByBusinessKeyAndMessageType(String businessKey, MessageType messageType);
    
    /**
     * 查找指定时间内的消息记录（分页）
     */
    Page<MessageRecord> findByCreatedTimeBetween(LocalDateTime startTime, LocalDateTime endTime, Pageable pageable);
    
    /**
     * 根据多个条件查找消息记录（分页）
     */
    @Query("SELECT m FROM MessageRecord m WHERE " +
           "(:messageType IS NULL OR m.messageType = :messageType) AND " +
           "(:status IS NULL OR m.status = :status) AND " +
           "(:consumerGroup IS NULL OR m.consumerGroup = :consumerGroup) AND " +
           "(:startTime IS NULL OR m.createdTime >= :startTime) AND " +
           "(:endTime IS NULL OR m.createdTime <= :endTime) " +
           "ORDER BY m.createdTime DESC")
    Page<MessageRecord> findByMultipleConditions(@Param("messageType") MessageType messageType,
                                                @Param("status") ProcessingStatus status,
                                                @Param("consumerGroup") String consumerGroup,
                                                @Param("startTime") LocalDateTime startTime,
                                                @Param("endTime") LocalDateTime endTime,
                                                Pageable pageable);
    
    /**
     * 查找异常消息记录
     */
    @Query("SELECT m FROM MessageRecord m WHERE m.errorMessage IS NOT NULL AND m.errorMessage != '' ORDER BY m.updatedTime DESC")
    List<MessageRecord> findMessagesWithErrors(Pageable pageable);
    
    /**
     * 统计今日消息数量
     */
    @Query("SELECT COUNT(m) FROM MessageRecord m WHERE DATE(m.createdTime) = CURRENT_DATE")
    long countTodayMessages();
    
    /**
     * 统计今日各状态消息数量
     */
    @Query("SELECT m.status, COUNT(m) FROM MessageRecord m WHERE DATE(m.createdTime) = CURRENT_DATE GROUP BY m.status")
    List<Object[]> getTodayStatusDistribution();
    
    /**
     * 查找最近处理的消息记录
     */
    @Query("SELECT m FROM MessageRecord m WHERE m.processingEndTime IS NOT NULL ORDER BY m.processingEndTime DESC")
    List<MessageRecord> findRecentlyProcessedMessages(Pageable pageable);
    
    /**
     * 查找处理时间超过阈值的消息记录
     */
    @Query("SELECT m FROM MessageRecord m WHERE m.processingStartTime IS NOT NULL AND m.processingEndTime IS NOT NULL AND TIMESTAMPDIFF(SECOND, m.processingStartTime, m.processingEndTime) > :thresholdSeconds ORDER BY (TIMESTAMPDIFF(SECOND, m.processingStartTime, m.processingEndTime)) DESC")
    List<MessageRecord> findSlowProcessingMessages(@Param("thresholdSeconds") long thresholdSeconds, Pageable pageable);
}