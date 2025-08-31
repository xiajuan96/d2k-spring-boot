package example.d2k.payment.retry.repository;

import example.d2k.payment.retry.entity.NotificationStatus;
import example.d2k.payment.retry.entity.PaymentNotification;
import example.d2k.payment.retry.entity.PaymentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 支付通知数据访问接口
 * @author xiajuan96
 */
@Repository
public interface PaymentNotificationRepository extends JpaRepository<PaymentNotification, Long> {
    
    /**
     * 根据支付ID查找通知记录
     */
    Optional<PaymentNotification> findByPaymentId(String paymentId);
    
    /**
     * 根据订单号查找通知记录列表
     */
    List<PaymentNotification> findByOrderNoOrderByCreateTimeDesc(String orderNo);
    
    /**
     * 根据通知状态查找记录
     */
    List<PaymentNotification> findByNotificationStatus(NotificationStatus notificationStatus);
    
    /**
     * 根据支付状态查找记录
     */
    List<PaymentNotification> findByStatus(PaymentStatus status);
    
    /**
     * 查找需要重试的通知记录
     */
    @Query("SELECT p FROM PaymentNotification p WHERE " +
           "p.notificationStatus IN ('PENDING', 'FAILED', 'RETRYING') AND " +
           "p.retryCount < p.maxRetryCount AND " +
           "(p.nextRetryTime IS NULL OR p.nextRetryTime <= :currentTime)")
    List<PaymentNotification> findNotificationsToRetry(@Param("currentTime") LocalDateTime currentTime);
    
    /**
     * 查找指定时间之前创建的待处理通知
     */
    @Query("SELECT p FROM PaymentNotification p WHERE " +
           "p.notificationStatus = 'PENDING' AND p.createTime < :beforeTime")
    List<PaymentNotification> findPendingNotificationsBeforeTime(@Param("beforeTime") LocalDateTime beforeTime);
    
    /**
     * 查找重试失败的通知记录
     */
    @Query("SELECT p FROM PaymentNotification p WHERE " +
           "p.retryCount >= p.maxRetryCount AND " +
           "p.notificationStatus NOT IN ('SUCCESS', 'ABANDONED')")
    List<PaymentNotification> findFailedNotifications();
    
    /**
     * 统计指定状态的通知数量
     */
    long countByNotificationStatus(NotificationStatus notificationStatus);
    
    /**
     * 统计指定支付状态的通知数量
     */
    long countByStatus(PaymentStatus status);
    
    /**
     * 查找指定时间范围内的通知记录
     */
    @Query("SELECT p FROM PaymentNotification p WHERE " +
           "p.createTime BETWEEN :startTime AND :endTime ORDER BY p.createTime DESC")
    List<PaymentNotification> findNotificationsBetweenTime(@Param("startTime") LocalDateTime startTime,
                                                           @Param("endTime") LocalDateTime endTime);
    
    /**
     * 查找重试次数超过指定值的通知
     */
    @Query("SELECT p FROM PaymentNotification p WHERE p.retryCount > :retryCount")
    List<PaymentNotification> findNotificationsWithRetryCountGreaterThan(@Param("retryCount") Integer retryCount);
    
    /**
     * 查找指定回调URL的通知记录
     */
    List<PaymentNotification> findByCallbackUrlOrderByCreateTimeDesc(String callbackUrl);
    
    /**
     * 查找最近的通知记录（用于监控）
     */
    @Query("SELECT p FROM PaymentNotification p ORDER BY p.createTime DESC")
    List<PaymentNotification> findRecentNotifications();
    
    /**
     * 统计成功率
     */
    @Query("SELECT COUNT(p) FROM PaymentNotification p WHERE " +
           "p.notificationStatus = 'SUCCESS' AND p.createTime >= :since")
    long countSuccessfulNotificationsSince(@Param("since") LocalDateTime since);
    
    /**
     * 统计总数
     */
    @Query("SELECT COUNT(p) FROM PaymentNotification p WHERE p.createTime >= :since")
    long countTotalNotificationsSince(@Param("since") LocalDateTime since);
}