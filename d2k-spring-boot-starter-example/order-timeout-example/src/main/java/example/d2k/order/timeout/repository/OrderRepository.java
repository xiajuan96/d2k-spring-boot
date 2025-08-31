package example.d2k.order.timeout.repository;

import example.d2k.order.timeout.entity.Order;
import example.d2k.order.timeout.entity.OrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 订单数据访问接口
 * @author xiajuan96
 */
@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {
    
    /**
     * 根据订单号查找订单
     */
    Optional<Order> findByOrderNo(String orderNo);
    
    /**
     * 根据用户ID查找订单列表
     */
    List<Order> findByUserIdOrderByCreateTimeDesc(Long userId);
    
    /**
     * 根据状态查找订单列表
     */
    List<Order> findByStatus(OrderStatus status);
    
    /**
     * 查找指定时间之前创建且状态为待支付的订单
     */
    @Query("SELECT o FROM Order o WHERE o.status = :status AND o.createTime < :beforeTime")
    List<Order> findPendingOrdersBeforeTime(@Param("status") OrderStatus status, 
                                           @Param("beforeTime") LocalDateTime beforeTime);
    
    /**
     * 查找超时的待支付订单
     */
    @Query("SELECT o FROM Order o WHERE o.status = 'PENDING' AND " +
           "TIMESTAMPDIFF(MINUTE, o.createTime, :currentTime) >= o.timeoutMinutes")
    List<Order> findTimeoutPendingOrders(@Param("currentTime") LocalDateTime currentTime);
    
    /**
     * 统计用户的订单数量
     */
    long countByUserId(Long userId);
    
    /**
     * 统计指定状态的订单数量
     */
    long countByStatus(OrderStatus status);
    
    /**
     * 查找用户在指定时间范围内的订单
     */
    @Query("SELECT o FROM Order o WHERE o.userId = :userId AND " +
           "o.createTime BETWEEN :startTime AND :endTime ORDER BY o.createTime DESC")
    List<Order> findUserOrdersBetweenTime(@Param("userId") Long userId,
                                         @Param("startTime") LocalDateTime startTime,
                                         @Param("endTime") LocalDateTime endTime);
}