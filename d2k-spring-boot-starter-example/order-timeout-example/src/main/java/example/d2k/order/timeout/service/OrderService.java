package example.d2k.order.timeout.service;

import com.d2k.spring.boot.autoconfigure.template.D2kTemplate;
import example.d2k.order.timeout.entity.Order;
import example.d2k.order.timeout.entity.OrderStatus;
import example.d2k.order.timeout.repository.OrderRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * 订单服务类
 * @author xiajuan96
 */
@Service
@Transactional
public class OrderService {
    
    private static final Logger logger = LoggerFactory.getLogger(OrderService.class);
    
    @Autowired
    private OrderRepository orderRepository;
    
    @Autowired
    private D2kTemplate d2kTemplate;
    
    /**
     * 创建订单
     */
    public Order createOrder(Long userId, String productName, BigDecimal amount) {
        return createOrder(userId, productName, amount, 30); // 默认30分钟超时
    }
    
    /**
     * 创建订单（指定超时时间）
     */
    public Order createOrder(Long userId, String productName, BigDecimal amount, Integer timeoutMinutes) {
        // 生成订单号
        String orderNo = generateOrderNo();
        
        // 创建订单
        Order order = new Order(orderNo, userId, productName, amount);
        order.setTimeoutMinutes(timeoutMinutes);
        
        // 保存订单
        order = orderRepository.save(order);
        
        // 发送延迟消息，用于订单超时处理
        sendOrderTimeoutMessage(order);
        
        logger.info("订单创建成功: {}, 超时时间: {}分钟", orderNo, timeoutMinutes);
        return order;
    }
    
    /**
     * 支付订单
     */
    public boolean payOrder(String orderNo) {
        Optional<Order> orderOpt = orderRepository.findByOrderNo(orderNo);
        if (!orderOpt.isPresent()) {
            logger.warn("订单不存在: {}", orderNo);
            return false;
        }
        
        Order order = orderOpt.get();
        if (order.getStatus() != OrderStatus.PENDING) {
            logger.warn("订单状态不正确，无法支付: {}, 当前状态: {}", orderNo, order.getStatus());
            return false;
        }
        
        // 更新订单状态为已支付
        order.setStatus(OrderStatus.PAID);
        orderRepository.save(order);
        
        logger.info("订单支付成功: {}", orderNo);
        return true;
    }
    
    /**
     * 取消订单
     */
    public boolean cancelOrder(String orderNo) {
        Optional<Order> orderOpt = orderRepository.findByOrderNo(orderNo);
        if (!orderOpt.isPresent()) {
            logger.warn("订单不存在: {}", orderNo);
            return false;
        }
        
        Order order = orderOpt.get();
        if (!order.getStatus().canCancel()) {
            logger.warn("订单状态不允许取消: {}, 当前状态: {}", orderNo, order.getStatus());
            return false;
        }
        
        // 更新订单状态为已取消
        order.setStatus(OrderStatus.CANCELLED);
        orderRepository.save(order);
        
        logger.info("订单取消成功: {}", orderNo);
        return true;
    }
    
    /**
     * 处理订单超时
     */
    public boolean handleOrderTimeout(String orderNo) {
        Optional<Order> orderOpt = orderRepository.findByOrderNo(orderNo);
        if (!orderOpt.isPresent()) {
            logger.warn("处理超时时订单不存在: {}", orderNo);
            return false;
        }
        
        Order order = orderOpt.get();
        
        // 只有待支付状态的订单才能超时取消
        if (order.getStatus() != OrderStatus.PENDING) {
            logger.info("订单状态已变更，无需超时处理: {}, 当前状态: {}", orderNo, order.getStatus());
            return false;
        }
        
        // 检查是否真的超时了
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime timeoutTime = order.getCreateTime().plusMinutes(order.getTimeoutMinutes());
        
        if (now.isBefore(timeoutTime)) {
            logger.info("订单尚未超时，无需处理: {}, 超时时间: {}", orderNo, timeoutTime);
            return false;
        }
        
        // 更新订单状态为超时取消
        order.setStatus(OrderStatus.TIMEOUT_CANCELLED);
        orderRepository.save(order);
        
        logger.info("订单超时取消成功: {}, 创建时间: {}, 超时时间: {}分钟", 
                   orderNo, order.getCreateTime(), order.getTimeoutMinutes());
        return true;
    }
    
    /**
     * 根据订单号查找订单
     */
    public Optional<Order> findByOrderNo(String orderNo) {
        return orderRepository.findByOrderNo(orderNo);
    }
    
    /**
     * 查找用户的订单列表
     */
    public List<Order> findUserOrders(Long userId) {
        return orderRepository.findByUserIdOrderByCreateTimeDesc(userId);
    }
    
    /**
     * 查找所有待支付订单
     */
    public List<Order> findPendingOrders() {
        return orderRepository.findByStatus(OrderStatus.PENDING);
    }
    
    /**
     * 发送订单超时延迟消息
     */
    private void sendOrderTimeoutMessage(Order order) {
        try {
            // 计算延迟时间（毫秒）
            long delayMs = order.getTimeoutMinutes() * 60 * 1000L;
            
            // 发送延迟消息（使用预配置的延迟时间）
            d2kTemplate.send(
                "order-timeout",           // topic
                order.getOrderNo(),        // key
                order.getOrderNo()         // message
            );
            
            logger.info("订单超时延迟消息发送成功: {}, 延迟时间: {}分钟", 
                       order.getOrderNo(), order.getTimeoutMinutes());
        } catch (Exception e) {
            logger.error("订单超时延迟消息发送失败: {}", order.getOrderNo(), e);
            // 这里可以考虑重试或者其他补偿机制
        }
    }
    
    /**
     * 生成订单号
     */
    private String generateOrderNo() {
        return "ORD" + System.currentTimeMillis() + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }
}