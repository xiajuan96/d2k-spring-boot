package example.d2k.idempotent.message;

import com.d2k.spring.boot.autoconfigure.annotation.EnableD2k;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.web.client.RestTemplate;

/**
 * 消息幂等性处理示例应用程序
 * 
 * 本示例演示了如何使用D2K框架实现消息的幂等性处理，包括：
 * 
 * 功能特性：
 * 1. 消息幂等性检查 - 防止重复消息处理
 * 2. 消息状态管理 - 跟踪消息处理状态
 * 3. 重试机制 - 失败消息自动重试
 * 4. 超时处理 - 处理超时消息自动标记
 * 5. Redis缓存 - 提高幂等性检查性能
 * 6. 数据持久化 - H2内存数据库存储消息记录
 * 7. RESTful API - 提供消息管理接口
 * 8. 异步处理 - 支持异步消息处理
 * 9. 监控支持 - 提供处理统计和健康检查
 * 10. 批量操作 - 支持批量消息处理
 * 
 * 快速体验：
 * 1. 启动Kafka: docker run -d --name kafka -p 9092:9092 apache/kafka:latest
 * 2. 启动Redis: docker run -d --name redis -p 6379:6379 redis:latest
 * 3. 运行应用: mvn spring-boot:run
 * 4. 访问API文档: http://localhost:8084/api/message-idempotent/health
 * 5. 发送测试消息: POST http://localhost:8084/api/message-idempotent/test/send?businessKey=test123
 * 6. 查看消息记录: GET http://localhost:8084/api/message-idempotent/records
 * 7. 查看统计信息: GET http://localhost:8084/api/message-idempotent/statistics/today
 * 
 * 提示：
 * - 相同messageId的消息只会被处理一次
 * - 失败的消息会自动重试，重试次数可配置
 * - 可通过API手动重试或取消消息
 * - 支持按业务键、消息类型、状态等条件查询
 * - 提供丰富的统计信息和监控指标
 * 
 * 监控地址：
 * - 健康检查: http://localhost:8084/api/message-idempotent/health
 * - H2控制台: http://localhost:8084/h2-console (JDBC URL: jdbc:h2:mem:testdb)
 * - 应用指标: http://localhost:8084/actuator/health
 * 
 * @author xiajuan96
 */
@SpringBootApplication
@EnableD2k
@EnableTransactionManagement
@EnableAsync
@EnableScheduling
public class IdempotentMessageExampleApplication {
    
    public static void main(String[] args) {
        SpringApplication.run(IdempotentMessageExampleApplication.class, args);
        
        System.out.println("\n" +
            "=================================================================\n" +
            "  消息幂等性处理示例应用启动成功！\n" +
            "=================================================================\n" +
            "\n" +
            "🚀 功能特性：\n" +
            "   • 消息幂等性检查 - 防止重复处理\n" +
            "   • 消息状态管理 - 完整的状态跟踪\n" +
            "   • 重试机制 - 智能重试策略\n" +
            "   • 超时处理 - 自动超时检测\n" +
            "   • Redis缓存 - 高性能缓存\n" +
            "   • 数据持久化 - 可靠的数据存储\n" +
            "   • RESTful API - 完整的管理接口\n" +
            "   • 异步处理 - 高并发支持\n" +
            "   • 监控支持 - 实时统计监控\n" +
            "   • 批量操作 - 高效批量处理\n" +
            "\n" +
            "📋 快速体验：\n" +
            "   1. 发送测试消息:\n" +
            "      POST http://localhost:8084/api/message-idempotent/test/send?businessKey=test123\n" +
            "\n" +
            "   2. 查看消息记录:\n" +
            "      GET http://localhost:8084/api/message-idempotent/records\n" +
            "\n" +
            "   3. 检查消息状态:\n" +
            "      GET http://localhost:8084/api/message-idempotent/check/{messageId}\n" +
            "\n" +
            "   4. 查看今日统计:\n" +
            "      GET http://localhost:8084/api/message-idempotent/statistics/today\n" +
            "\n" +
            "   5. 批量发送测试消息:\n" +
            "      POST http://localhost:8084/api/message-idempotent/test/batch-send?count=50&failureRate=0.2\n" +
            "\n" +
            "   6. 手动重试失败消息:\n" +
            "      POST http://localhost:8084/api/message-idempotent/retry/{messageId}\n" +
            "\n" +
            "💡 提示信息：\n" +
            "   • 相同messageId的消息只会被处理一次\n" +
            "   • 失败消息会根据配置自动重试\n" +
            "   • 支持多种消息类型和优先级\n" +
            "   • 提供完整的消息生命周期管理\n" +
            "   • 支持按多种条件查询和统计\n" +
            "\n" +
            "🔍 监控地址：\n" +
            "   • 健康检查: http://localhost:8084/api/message-idempotent/health\n" +
            "   • H2控制台: http://localhost:8084/h2-console\n" +
            "     (JDBC URL: jdbc:h2:mem:testdb, 用户名: sa, 密码: 空)\n" +
            "   • 应用指标: http://localhost:8084/actuator/health\n" +
            "   • 应用信息: http://localhost:8084/actuator/info\n" +
            "\n" +
            "=================================================================\n"
        );
    }
    
    /**
     * 配置RestTemplate
     */
    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
    
    /**
     * 配置RedisTemplate
     */
    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);
        
        // 设置key序列化器
        template.setKeySerializer(new StringRedisSerializer());
        template.setHashKeySerializer(new StringRedisSerializer());
        
        // 设置value序列化器
        GenericJackson2JsonRedisSerializer jsonSerializer = new GenericJackson2JsonRedisSerializer();
        template.setValueSerializer(jsonSerializer);
        template.setHashValueSerializer(jsonSerializer);
        
        template.afterPropertiesSet();
        return template;
    }
}