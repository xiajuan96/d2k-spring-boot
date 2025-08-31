package example.d2k.monitoring;

import com.d2k.spring.boot.autoconfigure.annotation.EnableD2k;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.client.RestTemplate;
import org.springframework.context.annotation.Bean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 监控示例应用程序
 * <p>
 * 本示例演示了如何使用D2K框架进行全面的监控和健康检查，包括：
 * <p>
 * 核心功能：
 * - 实时监控指标收集（Kafka、HTTP、缓存、系统资源）
 * - 健康检查（数据库、Redis、Kafka连接状态）
 * - 智能告警系统（阈值监控、多级告警、通知）
 * - 性能分析和优化建议
 * <p>
 * 监控功能：
 * - Kafka生产者/消费者性能监控
 * - HTTP请求响应时间监控
 * - 系统资源使用率监控（CPU、内存、线程）
 * - 缓存命中率和性能监控
 * - 数据库连接池监控
 * <p>
 * 高级特性：
 * - 基于Micrometer的指标收集
 * - Prometheus指标导出
 * - 自定义监控切面
 * - 多级缓存策略
 * - 异步告警处理
 * - 历史数据存储和分析
 * <p>
 * 快速体验：
 * 1. 启动应用：mvn spring-boot:run
 * 2. 访问监控概览：http://localhost:8080/monitoring/api/monitoring/overview
 * 3. 查看健康检查：http://localhost:8080/monitoring/api/monitoring/health
 * 4. 查看活跃告警：http://localhost:8080/monitoring/api/monitoring/alerts
 * 5. 性能测试：http://localhost:8080/monitoring/api/monitoring/test/kafka/send
 * 6. Prometheus指标：http://localhost:8080/monitoring/actuator/prometheus
 * <p>
 * 提示：
 * - 确保Kafka和Redis服务已启动
 * - 可通过配置文件调整监控参数和告警阈值
 * - 支持多实例监控和集群健康检查
 * - 集成了邮件和Webhook告警通知
 * <p>
 * 监控地址：
 * - 应用监控：http://localhost:8080/monitoring/api/monitoring/overview
 * - 健康检查：http://localhost:8080/monitoring/actuator/health
 * - 指标监控：http://localhost:8080/monitoring/actuator/metrics
 * - Prometheus：http://localhost:8080/monitoring/actuator/prometheus
 *
 * @author xiajuan96
 */
@SpringBootApplication
@EnableD2k
@EnableTransactionManagement
@EnableAsync
@EnableScheduling
public class MonitoringExampleApplication {

    public static void main(String[] args) {
        System.out.println("\n" +
                "=================================================================\n" +
                "                    D2K 监控示例应用                              \n" +
                "=================================================================\n" +
                "\n" +
                "🚀 功能特性：\n" +
                "   • 实时监控指标收集（Kafka、HTTP、缓存、系统资源）\n" +
                "   • 健康检查（数据库、Redis、Kafka连接状态）\n" +
                "   • 智能告警系统（阈值监控、多级告警、通知）\n" +
                "   • 性能分析和优化建议\n" +
                "   • Prometheus指标导出\n" +
                "   • 多实例监控支持\n" +
                "\n" +
                "📊 监控地址：\n" +
                "   • 监控概览：http://localhost:8080/monitoring/api/monitoring/overview\n" +
                "   • 健康检查：http://localhost:8080/monitoring/api/monitoring/health\n" +
                "   • 活跃告警：http://localhost:8080/monitoring/api/monitoring/alerts\n" +
                "   • 缓存统计：http://localhost:8080/monitoring/api/monitoring/cache/stats\n" +
                "   • Micrometer指标：http://localhost:8080/monitoring/api/monitoring/micrometer\n" +
                "\n" +
                "🧪 性能测试：\n" +
                "   • Kafka发送测试：POST http://localhost:8080/monitoring/api/monitoring/test/kafka/send\n" +
                "   • Redis性能测试：POST http://localhost:8080/monitoring/api/monitoring/test/redis\n" +
                "   • 缓存性能测试：POST http://localhost:8080/monitoring/api/monitoring/test/cache\n" +
                "\n" +
                "📈 Actuator端点：\n" +
                "   • 健康检查：http://localhost:8080/monitoring/actuator/health\n" +
                "   • 指标监控：http://localhost:8080/monitoring/actuator/metrics\n" +
                "   • Prometheus：http://localhost:8080/monitoring/actuator/prometheus\n" +
                "   • 应用信息：http://localhost:8080/monitoring/actuator/info\n" +
                "\n" +
                "⚠️  注意事项：\n" +
                "   • 确保Kafka服务已启动（localhost:9092）\n" +
                "   • 确保Redis服务已启动（localhost:6379）\n" +
                "   • 可通过application.yml调整监控参数\n" +
                "   • 支持邮件和Webhook告警通知配置\n" +
                "\n" +
                "=================================================================\n");

        SpringApplication.run(MonitoringExampleApplication.class, args);

        System.out.println("\n" +
                "🎉 监控示例应用启动成功！\n" +
                "\n" +
                "📋 快速开始：\n" +
                "   1. 访问监控概览查看系统状态\n" +
                "   2. 运行性能测试验证监控功能\n" +
                "   3. 查看告警配置和通知设置\n" +
                "   4. 集成Prometheus进行指标收集\n" +
                "\n" +
                "📚 更多信息请查看README.md文件\n" +
                "\n");
    }

    /**
     * 配置RestTemplate
     */
    @Bean
    @ConditionalOnMissingBean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }


}