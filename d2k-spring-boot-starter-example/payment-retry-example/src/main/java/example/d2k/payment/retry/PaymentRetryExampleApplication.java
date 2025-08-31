package example.d2k.payment.retry;

import com.d2k.spring.boot.autoconfigure.annotation.EnableD2k;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * 支付结果异步通知重试示例应用
 * 
 * 本示例演示了如何使用 D2K 框架实现支付结果的异步通知重试机制。
 * 
 * 功能特性：
 * 1. 支付通知管理 - 创建、更新、查询支付通知记录
 * 2. 自动重试机制 - 支持多种重试策略和间隔配置
 * 3. HTTP回调 - 向商户系统发送支付结果通知
 * 4. 状态管理 - 完整的支付和通知状态跟踪
 * 5. 延迟消息 - 基于D2K的延迟消息实现重试调度
 * 6. 数据持久化 - 使用JPA和H2数据库存储通知记录
 * 7. RESTful API - 提供完整的API接口进行管理
 * 8. 异步处理 - 支持同步和异步消息处理
 * 9. 事务管理 - 确保数据一致性
 * 10. 监控支持 - 集成健康检查和指标监控
 * 
 * 快速体验：
 * 1. 启动 Kafka: docker run -d --name kafka -p 9092:9092 apache/kafka:latest
 * 2. 运行应用: mvn spring-boot:run
 * 3. 访问 API 文档: http://localhost:8082/payment-retry/api/payment/health
 * 4. 创建支付通知: POST /api/payment/notification
 * 5. 查看 H2 控制台: http://localhost:8082/payment-retry/h2-console
 * 
 * 提示：
 * - 支持多种重试策略：30s, 1m, 5m, 15m, 30m
 * - 默认最大重试次数：5次
 * - 支持手动重试和放弃操作
 * - 提供模拟商户回调接口用于测试
 * 
 * 监控地址：
 * - 健康检查: http://localhost:8082/payment-retry/actuator/health
 * - 应用指标: http://localhost:8082/payment-retry/actuator/metrics
 * 
 * @author xiajuan96
 */
@SpringBootApplication
@EnableD2k
@EnableTransactionManagement
public class PaymentRetryExampleApplication {
    
    public static void main(String[] args) {
        SpringApplication.run(PaymentRetryExampleApplication.class, args);
        
        System.out.println("\n" +
            "=================================================================\n" +
            "  支付结果异步通知重试示例应用启动成功！\n" +
            "=================================================================\n" +
            "\n" +
            "🚀 应用信息:\n" +
            "   • 应用名称: payment-retry-example\n" +
            "   • 服务端口: 8082\n" +
            "   • 上下文路径: /payment-retry\n" +
            "\n" +
            "📋 主要功能:\n" +
            "   • 支付通知管理和重试机制\n" +
            "   • HTTP回调和状态跟踪\n" +
            "   • 延迟消息和异步处理\n" +
            "   • 数据持久化和事务管理\n" +
            "\n" +
            "🔗 快速链接:\n" +
            "   • 健康检查: http://localhost:8082/payment-retry/api/payment/health\n" +
            "   • H2 控制台: http://localhost:8082/payment-retry/h2-console\n" +
            "   • 应用监控: http://localhost:8082/payment-retry/actuator/health\n" +
            "\n" +
            "📖 API 示例:\n" +
            "   • 创建通知: POST /api/payment/notification\n" +
            "   • 更新状态: PUT /api/payment/status\n" +
            "   • 手动重试: POST /api/payment/retry/{paymentId}\n" +
            "   • 查询详情: GET /api/payment/notification/{paymentId}\n" +
            "\n" +
            "⚡ 测试场景:\n" +
            "   1. 创建支付通知记录\n" +
            "   2. 模拟回调失败触发重试\n" +
            "   3. 查看重试状态和次数\n" +
            "   4. 手动重试或放弃通知\n" +
            "\n" +
            "💡 提示: 查看 README.md 获取详细使用说明\n" +
            "=================================================================\n");
    }
    
    /**
     * 配置 WebClient Bean
     */
    @Bean
    public WebClient.Builder webClientBuilder() {
        return WebClient.builder()
                .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(1024 * 1024)) // 1MB
                .defaultHeader("Content-Type", "application/json")
                .defaultHeader("User-Agent", "D2K-Payment-Retry-Example/1.0");
    }
}