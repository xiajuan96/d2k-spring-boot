package example.d2k.order.timeout;

import com.d2k.spring.boot.autoconfigure.annotation.EnableD2k;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * 订单超时处理示例应用程序
 * 
 * 本示例演示如何使用D2K处理电商订单超时场景：
 * 1. 用户创建订单后，系统自动发送延迟消息
 * 2. 在指定时间后，如果订单仍未支付，自动取消订单
 * 3. 支持自定义超时时间
 * 4. 提供完整的订单管理API
 * 
 * @author xiajuan96
 */
@SpringBootApplication
@EnableD2k
@EnableTransactionManagement
public class OrderTimeoutExampleApplication {
    
    public static void main(String[] args) {
        SpringApplication.run(OrderTimeoutExampleApplication.class, args);
        System.out.println("\n" +
                "=================================================\n" +
                "  订单超时处理示例应用启动成功！\n" +
                "=================================================\n" +
                "\n" +
                "📋 功能特性：\n" +
                "  • 订单创建与管理\n" +
                "  • 自动超时取消机制\n" +
                "  • 延迟消息处理\n" +
                "  • RESTful API接口\n" +
                "\n" +
                "🚀 快速体验：\n" +
                "  1. 创建订单：\n" +
                "     POST /api/orders?userId=1&productName=iPhone&amount=8999&timeoutMinutes=2\n" +
                "\n" +
                "  2. 查询订单：\n" +
                "     GET /api/orders/{orderNo}\n" +
                "\n" +
                "  3. 支付订单：\n" +
                "     POST /api/orders/{orderNo}/pay\n" +
                "\n" +
                "  4. 查看待支付订单：\n" +
                "     GET /api/orders/pending\n" +
                "\n" +
                "💡 提示：\n" +
                "  • 订单创建后会自动发送延迟消息\n" +
                "  • 超时时间到达后自动取消未支付订单\n" +
                "  • 可通过日志观察消息处理过程\n" +
                "\n" +
                "📊 监控地址：\n" +
                "  • 应用健康检查: http://localhost:8080/actuator/health\n" +
                "  • H2数据库控制台: http://localhost:8080/h2-console\n" +
                "\n" +
                "=================================================\n");
    }
}