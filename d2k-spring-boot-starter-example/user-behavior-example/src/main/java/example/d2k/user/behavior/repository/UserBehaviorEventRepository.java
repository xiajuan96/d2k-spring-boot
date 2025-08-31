package example.d2k.user.behavior.repository;

import example.d2k.user.behavior.entity.BehaviorEventType;
import example.d2k.user.behavior.entity.ProcessStatus;
import example.d2k.user.behavior.entity.UserBehaviorEvent;
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
 * 用户行为事件数据访问层
 * @author xiajuan96
 */
@Repository
public interface UserBehaviorEventRepository extends JpaRepository<UserBehaviorEvent, String> {
    
    /**
     * 根据用户ID查找事件
     */
    List<UserBehaviorEvent> findByUserId(String userId);
    
    /**
     * 根据用户ID和事件类型查找事件
     */
    List<UserBehaviorEvent> findByUserIdAndEventType(String userId, BehaviorEventType eventType);
    
    /**
     * 根据用户ID和事件类型分页查找事件
     */
    Page<UserBehaviorEvent> findByUserIdAndEventType(String userId, BehaviorEventType eventType, Pageable pageable);
    
    /**
     * 根据事件类型查找事件
     */
    List<UserBehaviorEvent> findByEventType(BehaviorEventType eventType);
    
    /**
     * 根据事件类型分页查找事件
     */
    Page<UserBehaviorEvent> findByEventType(BehaviorEventType eventType, Pageable pageable);
    
    /**
     * 根据处理状态查找事件
     */
    List<UserBehaviorEvent> findByProcessStatus(ProcessStatus processStatus);
    
    /**
     * 根据处理状态分页查找事件
     */
    Page<UserBehaviorEvent> findByProcessStatus(ProcessStatus processStatus, Pageable pageable);
    
    /**
     * 根据用户ID和处理状态查找事件
     */
    List<UserBehaviorEvent> findByUserIdAndProcessStatus(String userId, ProcessStatus processStatus);
    
    /**
     * 根据用户ID和处理状态分页查找事件
     */
    Page<UserBehaviorEvent> findByUserIdAndProcessStatus(String userId, ProcessStatus processStatus, Pageable pageable);
    
    /**
     * 查找需要处理的事件（待处理和失败状态）
     */
    @Query("SELECT e FROM UserBehaviorEvent e WHERE e.processStatus IN :statuses ORDER BY e.eventTimestamp ASC")
    List<UserBehaviorEvent> findEventsToProcess(@Param("statuses") List<ProcessStatus> statuses);
    
    /**
     * 分页查找需要处理的事件
     */
    @Query("SELECT e FROM UserBehaviorEvent e WHERE e.processStatus IN :statuses ORDER BY e.eventTimestamp ASC")
    Page<UserBehaviorEvent> findEventsToProcess(@Param("statuses") List<ProcessStatus> statuses, Pageable pageable);
    
    /**
     * 查找需要延迟处理的事件
     */
    @Query("SELECT e FROM UserBehaviorEvent e WHERE e.processStatus = 'DELAYED' AND e.delayUntil <= :currentTime ORDER BY e.delayUntil ASC")
    List<UserBehaviorEvent> findDelayedEventsReadyToProcess(@Param("currentTime") LocalDateTime currentTime);
    
    /**
     * 分页查找需要延迟处理的事件
     */
    @Query("SELECT e FROM UserBehaviorEvent e WHERE e.processStatus = 'DELAYED' AND e.delayUntil <= :currentTime ORDER BY e.delayUntil ASC")
    Page<UserBehaviorEvent> findDelayedEventsReadyToProcess(@Param("currentTime") LocalDateTime currentTime, Pageable pageable);
    
    /**
     * 查找需要重试的事件
     */
    @Query("SELECT e FROM UserBehaviorEvent e WHERE e.processStatus = 'FAILED' AND e.retryCount < e.maxRetryCount AND e.nextRetryTime <= :currentTime ORDER BY e.nextRetryTime ASC")
    List<UserBehaviorEvent> findEventsToRetry(@Param("currentTime") LocalDateTime currentTime);
    
    /**
     * 分页查找需要重试的事件
     */
    @Query("SELECT e FROM UserBehaviorEvent e WHERE e.processStatus = 'FAILED' AND e.retryCount < e.maxRetryCount AND e.nextRetryTime <= :currentTime ORDER BY e.nextRetryTime ASC")
    Page<UserBehaviorEvent> findEventsToRetry(@Param("currentTime") LocalDateTime currentTime, Pageable pageable);
    
    /**
     * 根据时间范围查找事件
     */
    @Query("SELECT e FROM UserBehaviorEvent e WHERE e.eventTimestamp BETWEEN :startTime AND :endTime ORDER BY e.eventTimestamp DESC")
    List<UserBehaviorEvent> findEventsByTimeRange(@Param("startTime") LocalDateTime startTime, @Param("endTime") LocalDateTime endTime);
    
    /**
     * 分页根据时间范围查找事件
     */
    @Query("SELECT e FROM UserBehaviorEvent e WHERE e.eventTimestamp BETWEEN :startTime AND :endTime ORDER BY e.eventTimestamp DESC")
    Page<UserBehaviorEvent> findEventsByTimeRange(@Param("startTime") LocalDateTime startTime, @Param("endTime") LocalDateTime endTime, Pageable pageable);
    
    /**
     * 根据用户ID和时间范围查找事件
     */
    @Query("SELECT e FROM UserBehaviorEvent e WHERE e.userId = :userId AND e.eventTimestamp BETWEEN :startTime AND :endTime ORDER BY e.eventTimestamp DESC")
    List<UserBehaviorEvent> findEventsByUserAndTimeRange(@Param("userId") String userId, @Param("startTime") LocalDateTime startTime, @Param("endTime") LocalDateTime endTime);
    
    /**
     * 分页根据用户ID和时间范围查找事件
     */
    @Query("SELECT e FROM UserBehaviorEvent e WHERE e.userId = :userId AND e.eventTimestamp BETWEEN :startTime AND :endTime ORDER BY e.eventTimestamp DESC")
    Page<UserBehaviorEvent> findEventsByUserAndTimeRange(@Param("userId") String userId, @Param("startTime") LocalDateTime startTime, @Param("endTime") LocalDateTime endTime, Pageable pageable);
    
    /**
     * 根据页面路径查找事件
     */
    List<UserBehaviorEvent> findByPagePath(String pagePath);
    
    /**
     * 根据页面路径分页查找事件
     */
    Page<UserBehaviorEvent> findByPagePath(String pagePath, Pageable pageable);
    
    /**
     * 根据设备类型查找事件
     */
    List<UserBehaviorEvent> findByDeviceType(String deviceType);
    
    /**
     * 根据设备类型分页查找事件
     */
    Page<UserBehaviorEvent> findByDeviceType(String deviceType, Pageable pageable);
    
    /**
     * 根据IP地址查找事件
     */
    List<UserBehaviorEvent> findByIpAddress(String ipAddress);
    
    /**
     * 根据IP地址分页查找事件
     */
    Page<UserBehaviorEvent> findByIpAddress(String ipAddress, Pageable pageable);
    
    /**
     * 统计用户事件总数
     */
    @Query("SELECT COUNT(e) FROM UserBehaviorEvent e WHERE e.userId = :userId")
    long countByUserId(@Param("userId") String userId);
    
    /**
     * 统计用户特定类型事件数量
     */
    @Query("SELECT COUNT(e) FROM UserBehaviorEvent e WHERE e.userId = :userId AND e.eventType = :eventType")
    long countByUserIdAndEventType(@Param("userId") String userId, @Param("eventType") BehaviorEventType eventType);
    
    /**
     * 统计特定状态的事件数量
     */
    @Query("SELECT COUNT(e) FROM UserBehaviorEvent e WHERE e.processStatus = :status")
    long countByProcessStatus(@Param("status") ProcessStatus status);
    
    /**
     * 统计时间范围内的事件数量
     */
    @Query("SELECT COUNT(e) FROM UserBehaviorEvent e WHERE e.eventTimestamp BETWEEN :startTime AND :endTime")
    long countByTimeRange(@Param("startTime") LocalDateTime startTime, @Param("endTime") LocalDateTime endTime);
    
    /**
     * 统计用户在时间范围内的事件数量
     */
    @Query("SELECT COUNT(e) FROM UserBehaviorEvent e WHERE e.userId = :userId AND e.eventTimestamp BETWEEN :startTime AND :endTime")
    long countByUserAndTimeRange(@Param("userId") String userId, @Param("startTime") LocalDateTime startTime, @Param("endTime") LocalDateTime endTime);
    
    /**
     * 获取事件处理成功率
     */
    @Query("SELECT CAST(COUNT(CASE WHEN e.processStatus = 'PROCESSED' THEN 1 END) AS double) / COUNT(e) FROM UserBehaviorEvent e")
    double getProcessSuccessRate();
    
    /**
     * 获取用户事件处理成功率
     */
    @Query("SELECT CAST(COUNT(CASE WHEN e.processStatus = 'PROCESSED' THEN 1 END) AS double) / COUNT(e) FROM UserBehaviorEvent e WHERE e.userId = :userId")
    double getProcessSuccessRateByUser(@Param("userId") String userId);
    
    /**
     * 获取特定事件类型的处理成功率
     */
    @Query("SELECT CAST(COUNT(CASE WHEN e.processStatus = 'PROCESSED' THEN 1 END) AS double) / COUNT(e) FROM UserBehaviorEvent e WHERE e.eventType = :eventType")
    double getProcessSuccessRateByEventType(@Param("eventType") BehaviorEventType eventType);
    
    /**
     * 查找最近的用户事件
     */
    @Query("SELECT e FROM UserBehaviorEvent e WHERE e.userId = :userId ORDER BY e.eventTimestamp DESC")
    List<UserBehaviorEvent> findRecentEventsByUser(@Param("userId") String userId, Pageable pageable);
    
    /**
     * 查找用户最后一次特定类型的事件
     */
    @Query("SELECT e FROM UserBehaviorEvent e WHERE e.userId = :userId AND e.eventType = :eventType ORDER BY e.eventTimestamp DESC")
    Optional<UserBehaviorEvent> findLastEventByUserAndType(@Param("userId") String userId, @Param("eventType") BehaviorEventType eventType);
    
    /**
     * 批量更新事件状态
     */
    @Modifying
    @Query("UPDATE UserBehaviorEvent e SET e.processStatus = :newStatus, e.lastProcessedTime = :processedTime WHERE e.id IN :eventIds")
    int batchUpdateStatus(@Param("eventIds") List<String> eventIds, @Param("newStatus") ProcessStatus newStatus, @Param("processedTime") LocalDateTime processedTime);
    
    /**
     * 批量更新重试信息
     */
    @Modifying
    @Query("UPDATE UserBehaviorEvent e SET e.retryCount = e.retryCount + 1, e.nextRetryTime = :nextRetryTime, e.lastProcessedTime = :processedTime WHERE e.id IN :eventIds")
    int batchUpdateRetryInfo(@Param("eventIds") List<String> eventIds, @Param("nextRetryTime") LocalDateTime nextRetryTime, @Param("processedTime") LocalDateTime processedTime);
    
    /**
     * 删除过期的已处理事件
     */
    @Modifying
    @Query("DELETE FROM UserBehaviorEvent e WHERE e.processStatus IN ('PROCESSED', 'ARCHIVED') AND e.lastProcessedTime < :expireTime")
    int deleteExpiredProcessedEvents(@Param("expireTime") LocalDateTime expireTime);
    
    /**
     * 删除过期的失败事件
     */
    @Modifying
    @Query("DELETE FROM UserBehaviorEvent e WHERE e.processStatus IN ('RETRY_FAILED', 'CANCELLED') AND e.lastProcessedTime < :expireTime")
    int deleteExpiredFailedEvents(@Param("expireTime") LocalDateTime expireTime);
    
    /**
     * 归档旧事件
     */
    @Modifying
    @Query("UPDATE UserBehaviorEvent e SET e.processStatus = 'ARCHIVED', e.lastProcessedTime = :archiveTime WHERE e.processStatus = 'PROCESSED' AND e.lastProcessedTime < :archiveTime")
    int archiveOldEvents(@Param("archiveTime") LocalDateTime archiveTime);
    
    /**
     * 查找热门页面（按访问次数排序）
     */
    @Query("SELECT e.pagePath, COUNT(e) as visitCount FROM UserBehaviorEvent e WHERE e.eventType = 'PAGE_VIEW' AND e.eventTimestamp BETWEEN :startTime AND :endTime GROUP BY e.pagePath ORDER BY visitCount DESC")
    List<Object[]> findPopularPages(@Param("startTime") LocalDateTime startTime, @Param("endTime") LocalDateTime endTime, Pageable pageable);
    
    /**
     * 查找活跃用户（按事件数量排序）
     */
    @Query("SELECT e.userId, COUNT(e) as eventCount FROM UserBehaviorEvent e WHERE e.eventTimestamp BETWEEN :startTime AND :endTime GROUP BY e.userId ORDER BY eventCount DESC")
    List<Object[]> findActiveUsers(@Param("startTime") LocalDateTime startTime, @Param("endTime") LocalDateTime endTime, Pageable pageable);
    
    /**
     * 查找事件类型分布
     */
    @Query("SELECT e.eventType, COUNT(e) as eventCount FROM UserBehaviorEvent e WHERE e.eventTimestamp BETWEEN :startTime AND :endTime GROUP BY e.eventType ORDER BY eventCount DESC")
    List<Object[]> findEventTypeDistribution(@Param("startTime") LocalDateTime startTime, @Param("endTime") LocalDateTime endTime);
    
    /**
     * 查找设备类型分布
     */
    @Query("SELECT e.deviceType, COUNT(e) as deviceCount FROM UserBehaviorEvent e WHERE e.eventTimestamp BETWEEN :startTime AND :endTime GROUP BY e.deviceType ORDER BY deviceCount DESC")
    List<Object[]> findDeviceTypeDistribution(@Param("startTime") LocalDateTime startTime, @Param("endTime") LocalDateTime endTime);
    
    /**
     * 查找用户行为路径（按时间顺序）
     */
    @Query("SELECT e FROM UserBehaviorEvent e WHERE e.userId = :userId AND e.eventTimestamp BETWEEN :startTime AND :endTime ORDER BY e.eventTimestamp ASC")
    List<UserBehaviorEvent> findUserBehaviorPath(@Param("userId") String userId, @Param("startTime") LocalDateTime startTime, @Param("endTime") LocalDateTime endTime);
    
    /**
     * 查找异常事件（重试次数过多）
     */
    @Query("SELECT e FROM UserBehaviorEvent e WHERE e.retryCount >= :minRetryCount ORDER BY e.retryCount DESC, e.eventTimestamp DESC")
    List<UserBehaviorEvent> findAnomalousEvents(@Param("minRetryCount") int minRetryCount, Pageable pageable);
    
    /**
     * 检查用户是否存在特定事件
     */
    @Query("SELECT CASE WHEN COUNT(e) > 0 THEN true ELSE false END FROM UserBehaviorEvent e WHERE e.userId = :userId AND e.eventType = :eventType")
    boolean existsByUserIdAndEventType(@Param("userId") String userId, @Param("eventType") BehaviorEventType eventType);
    
    /**
     * 检查用户在时间范围内是否存在特定事件
     */
    @Query("SELECT CASE WHEN COUNT(e) > 0 THEN true ELSE false END FROM UserBehaviorEvent e WHERE e.userId = :userId AND e.eventType = :eventType AND e.eventTimestamp BETWEEN :startTime AND :endTime")
    boolean existsByUserIdAndEventTypeAndTimeRange(@Param("userId") String userId, @Param("eventType") BehaviorEventType eventType, @Param("startTime") LocalDateTime startTime, @Param("endTime") LocalDateTime endTime);
}