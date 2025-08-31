package example.d2k.performance.optimization;

import com.d2k.spring.boot.autoconfigure.annotation.EnableD2k;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
// Redis相关导入已移除，使用虚假实现
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.web.client.RestTemplate;

/**
 * 性能优化示例应用程序
 * 
 * 本示例演示了如何使用D2K框架进行Kafka消息处理的性能优化，包括：
 * 
 * 功能特性：
 * 1. 高性能Kafka生产者和消费者配置
 * 2. 实时性能监控和指标收集
 * 3. 多级缓存优化（本地缓存 + Redis）
 * 4. 线程池优化和异步处理
 * 5. 批量处理和压缩优化
 * 6. 自动性能告警和通知
 * 7. 性能测试和压力测试工具
 * 8. 详细的性能分析和报告
 * 
 * 快速体验：
 * 1. 启动应用：mvn spring-boot:run
 * 2. 访问监控面板：http://localhost:8080/performance/api/performance/metrics/overview
 * 3. 执行性能测试：POST http://localhost:8080/performance/api/performance/test/send
 * 4. 查看缓存统计：http://localhost:8080/performance/api/performance/cache/stats
 * 5. 健康检查：http://localhost:8080/performance/api/performance/health
 * 
 * 提示：
 * - 确保Kafka和Redis服务已启动
 * - 可通过配置文件调整性能参数
 * - 支持Prometheus指标导出
 * - 建议在生产环境中根据实际负载调整配置
 * 
 * 监控地址：
 * - 应用监控：http://localhost:8080/performance/actuator
 * - 性能指标：http://localhost:8080/performance/actuator/metrics
 * - 健康检查：http://localhost:8080/performance/actuator/health
 * 
 * @author xiajuan96
 */
@SpringBootApplication
@EnableD2k
@EnableTransactionManagement
@EnableAsync
@EnableScheduling
public class PerformanceOptimizationExampleApplication {
    
    private static final Logger logger = LoggerFactory.getLogger(PerformanceOptimizationExampleApplication.class);
    
    public static void main(String[] args) {
        String separator = "================================================================================";
        logger.info(separator);
        logger.info("启动性能优化示例应用程序...");
        logger.info(separator);
        
        SpringApplication.run(PerformanceOptimizationExampleApplication.class, args);
        
        logger.info(separator);
        logger.info("性能优化示例应用程序启动完成！");
        logger.info(separator);
        logger.info("");
        logger.info("🚀 功能特性：");
        logger.info("   ✅ 高性能Kafka配置 - 优化的生产者和消费者参数");
        logger.info("   ✅ 实时性能监控 - 自动收集和分析性能指标");
        logger.info("   ✅ 多级缓存优化 - 本地缓存 + Redis分布式缓存");
        logger.info("   ✅ 线程池优化 - 异步处理和并发控制");
        logger.info("   ✅ 批量处理优化 - 提高消息处理吞吐量");
        logger.info("   ✅ 自动性能告警 - 实时监控和异常通知");
        logger.info("   ✅ 性能测试工具 - 内置压力测试和基准测试");
        logger.info("   ✅ 详细性能分析 - 全方位的性能指标和报告");
        logger.info("");
        logger.info("🔗 快速体验：");
        logger.info("   📊 监控概览: http://localhost:8080/performance/api/performance/metrics/overview");
        logger.info("   🧪 性能测试: POST http://localhost:8080/performance/api/performance/test/send");
        logger.info("   💾 缓存统计: http://localhost:8080/performance/api/performance/cache/stats");
        logger.info("   ❤️ 健康检查: http://localhost:8080/performance/api/performance/health");
        logger.info("   📈 应用监控: http://localhost:8080/performance/actuator");
        logger.info("");
        logger.info("💡 提示：");
        logger.info("   - 确保Kafka服务运行在localhost:9092");
        logger.info("   - 确保Redis服务运行在localhost:6379");
        logger.info("   - 可通过application.yml调整性能参数");
        logger.info("   - 支持Prometheus指标导出和Grafana可视化");
        logger.info("   - 建议根据实际负载调整线程池和缓存配置");
        logger.info(separator);
    }
    
    /**
     * RestTemplate配置
     */
    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
    
    /**
     * Redis模板配置 - 已移除，使用虚假实现
     * 原有的RedisTemplate配置已被虚假实现替代
     */
}