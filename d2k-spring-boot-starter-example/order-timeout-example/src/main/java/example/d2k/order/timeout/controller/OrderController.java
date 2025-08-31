package example.d2k.order.timeout.controller;

import example.d2k.order.timeout.entity.Order;
import example.d2k.order.timeout.service.OrderService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 订单控制器
 * @author xiajuan96
 */
@RestController
@RequestMapping("/api/orders")
public class OrderController {
    
    private static final Logger logger = LoggerFactory.getLogger(OrderController.class);
    
    @Autowired
    private OrderService orderService;
    
    /**
     * 创建订单
     */
    @PostMapping
    public ResponseEntity<Map<String, Object>> createOrder(
            @RequestParam Long userId,
            @RequestParam String productName,
            @RequestParam BigDecimal amount,
            @RequestParam(defaultValue = "30") Integer timeoutMinutes) {
        
        Map<String, Object> response = new HashMap<>();
        
        try {
            Order order = orderService.createOrder(userId, productName, amount, timeoutMinutes);
            
            response.put("success", true);
            response.put("message", "订单创建成功");
            response.put("data", order);
            
            logger.info("订单创建API调用成功: {}", order.getOrderNo());
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("订单创建API调用失败", e);
            
            response.put("success", false);
            response.put("message", "订单创建失败: " + e.getMessage());
            
            return ResponseEntity.badRequest().body(response);
        }
    }
    
    /**
     * 支付订单
     */
    @PostMapping("/{orderNo}/pay")
    public ResponseEntity<Map<String, Object>> payOrder(@PathVariable String orderNo) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            boolean success = orderService.payOrder(orderNo);
            
            response.put("success", success);
            response.put("message", success ? "订单支付成功" : "订单支付失败");
            
            logger.info("订单支付API调用: {}, 结果: {}", orderNo, success);
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("订单支付API调用失败: {}", orderNo, e);
            
            response.put("success", false);
            response.put("message", "订单支付失败: " + e.getMessage());
            
            return ResponseEntity.badRequest().body(response);
        }
    }
    
    /**
     * 取消订单
     */
    @PostMapping("/{orderNo}/cancel")
    public ResponseEntity<Map<String, Object>> cancelOrder(@PathVariable String orderNo) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            boolean success = orderService.cancelOrder(orderNo);
            
            response.put("success", success);
            response.put("message", success ? "订单取消成功" : "订单取消失败");
            
            logger.info("订单取消API调用: {}, 结果: {}", orderNo, success);
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("订单取消API调用失败: {}", orderNo, e);
            
            response.put("success", false);
            response.put("message", "订单取消失败: " + e.getMessage());
            
            return ResponseEntity.badRequest().body(response);
        }
    }
    
    /**
     * 查询订单详情
     */
    @GetMapping("/{orderNo}")
    public ResponseEntity<Map<String, Object>> getOrder(@PathVariable String orderNo) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            Optional<Order> orderOpt = orderService.findByOrderNo(orderNo);
            
            if (orderOpt.isPresent()) {
                response.put("success", true);
                response.put("message", "订单查询成功");
                response.put("data", orderOpt.get());
            } else {
                response.put("success", false);
                response.put("message", "订单不存在");
            }
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("订单查询API调用失败: {}", orderNo, e);
            
            response.put("success", false);
            response.put("message", "订单查询失败: " + e.getMessage());
            
            return ResponseEntity.badRequest().body(response);
        }
    }
    
    /**
     * 查询用户订单列表
     */
    @GetMapping
    public ResponseEntity<Map<String, Object>> getUserOrders(@RequestParam Long userId) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            List<Order> orders = orderService.findUserOrders(userId);
            
            response.put("success", true);
            response.put("message", "用户订单查询成功");
            response.put("data", orders);
            response.put("total", orders.size());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("用户订单查询API调用失败: {}", userId, e);
            
            response.put("success", false);
            response.put("message", "用户订单查询失败: " + e.getMessage());
            
            return ResponseEntity.badRequest().body(response);
        }
    }
    
    /**
     * 查询所有待支付订单
     */
    @GetMapping("/pending")
    public ResponseEntity<Map<String, Object>> getPendingOrders() {
        Map<String, Object> response = new HashMap<>();
        
        try {
            List<Order> orders = orderService.findPendingOrders();
            
            response.put("success", true);
            response.put("message", "待支付订单查询成功");
            response.put("data", orders);
            response.put("total", orders.size());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("待支付订单查询API调用失败", e);
            
            response.put("success", false);
            response.put("message", "待支付订单查询失败: " + e.getMessage());
            
            return ResponseEntity.badRequest().body(response);
        }
    }
    
    /**
     * 手动触发订单超时处理（用于测试）
     */
    @PostMapping("/{orderNo}/timeout")
    public ResponseEntity<Map<String, Object>> triggerOrderTimeout(@PathVariable String orderNo) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            boolean success = orderService.handleOrderTimeout(orderNo);
            
            response.put("success", success);
            response.put("message", success ? "订单超时处理成功" : "订单超时处理跳过");
            
            logger.info("手动触发订单超时处理: {}, 结果: {}", orderNo, success);
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("手动触发订单超时处理失败: {}", orderNo, e);
            
            response.put("success", false);
            response.put("message", "订单超时处理失败: " + e.getMessage());
            
            return ResponseEntity.badRequest().body(response);
        }
    }
}