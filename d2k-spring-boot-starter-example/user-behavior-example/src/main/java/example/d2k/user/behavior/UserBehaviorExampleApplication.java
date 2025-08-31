package example.d2k.user.behavior;

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

import com.d2k.spring.boot.autoconfigure.annotation.EnableD2k;

/**
 * 用户行为分析延迟处理示例应用程序
 * 
 * 本示例演示如何使用 D2K 框架实现用户行为事件的收集、分析和延迟处理。
 * 
 * 功能特性：
 * 1. 用户行为事件收集 - 收集用户在网站/应用中的各种行为事件
 * 2. 延迟处理机制 - 对某些事件进行延迟处理，避免实时处理压力
 * 3. 批量处理优化 - 将相似事件进行批量处理，提高处理效率
 * 4. 重试机制 - 对处理失败的事件进行自动重试
 * 5. 状态管理 - 完整的事件处理状态跟踪
 * 6. Redis缓存 - 使用Redis缓存用户会话和统计数据
 * 7. 数据持久化 - 使用H2内存数据库存储事件数据
 * 8. RESTful API - 提供完整的API接口进行事件管理
 * 9. 异步处理 - 支持异步事件处理，提高系统响应性能
 * 10. 监控支持 - 集成Spring Boot Actuator进行健康检查和监控
 * 
 * 快速体验：
 * 1. 启动Kafka服务：docker run -d --name kafka -p 9092:9092 apache/kafka:latest
 * 2. 启动Redis服务：docker run -d --name redis -p 6379:6379 redis:latest
 * 3. 运行应用：mvn spring-boot:run
 * 4. 访问API文档：http://localhost:8083/user-behavior/api/user-behavior/health
 * 5. 模拟用户行为：POST http://localhost:8083/user-behavior/api/user-behavior/simulate
 * 6. 查看事件统计：GET http://localhost:8083/user-behavior/api/user-behavior/statistics
 * 
 * 提示：
 * - 本示例使用H2内存数据库，重启后数据会丢失
 * - 生产环境建议使用MySQL/PostgreSQL等持久化数据库
 * - Redis用于缓存用户会话和实时统计数据
 * - 可通过配置文件调整延迟处理、重试策略等参数
 * 
 * 监控地址：
 * - 健康检查：http://localhost:8083/user-behavior/actuator/health
 * - 应用信息：http://localhost:8083/user-behavior/actuator/info
 * - H2控制台：http://localhost:8083/user-behavior/h2-console
 * 
 * @author xiajuan96
 */
@SpringBootApplication
@EnableD2k
@EnableTransactionManagement
@EnableAsync
@EnableScheduling
public class UserBehaviorExampleApplication {

    public static void main(String[] args) {
        SpringApplication.run(UserBehaviorExampleApplication.class, args);
        
        System.out.println("\n" +
            "=================================================================\n" +
            "  用户行为分析延迟处理示例应用启动成功！\n" +
            "=================================================================\n" +
            "\n" +
            "🚀 功能特性：\n" +
            "   • 用户行为事件收集和分析\n" +
            "   • 延迟处理和批量优化\n" +
            "   • 自动重试和状态管理\n" +
            "   • Redis缓存和数据持久化\n" +
            "   • RESTful API和异步处理\n" +
            "   • 监控支持和健康检查\n" +
            "\n" +
            "📋 快速体验：\n" +
            "   1. 健康检查: http://localhost:8083/user-behavior/api/user-behavior/health\n" +
            "   2. 模拟用户行为: POST http://localhost:8083/user-behavior/api/user-behavior/simulate\n" +
            "   3. 查看事件统计: GET http://localhost:8083/user-behavior/api/user-behavior/statistics\n" +
            "   4. 创建行为事件: POST http://localhost:8083/user-behavior/api/user-behavior/events\n" +
            "   5. 查询用户事件: GET http://localhost:8083/user-behavior/api/user-behavior/users/{userId}/events\n" +
            "\n" +
            "🔧 管理接口：\n" +
            "   • 触发批量处理: POST http://localhost:8083/user-behavior/api/user-behavior/events/batch-process\n" +
            "   • 触发延迟处理: POST http://localhost:8083/user-behavior/api/user-behavior/events/process-delayed\n" +
            "   • 触发重试处理: POST http://localhost:8083/user-behavior/api/user-behavior/events/process-retry\n" +
            "   • 清理过期事件: POST http://localhost:8083/user-behavior/api/user-behavior/events/cleanup\n" +
            "\n" +
            "📊 监控地址：\n" +
            "   • 健康检查: http://localhost:8083/user-behavior/actuator/health\n" +
            "   • 应用信息: http://localhost:8083/user-behavior/actuator/info\n" +
            "   • H2控制台: http://localhost:8083/user-behavior/h2-console\n" +
            "\n" +
            "💡 提示：\n" +
            "   • 确保Kafka服务运行在localhost:9092\n" +
            "   • 确保Redis服务运行在localhost:6379\n" +
            "   • H2数据库用户名: sa，密码为空\n" +
            "   • 查看application.yml了解详细配置\n" +
            "\n" +
            "=================================================================\n");
    }
    
    /**
     * 配置RestTemplate Bean
     */
    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
    
    /**
     * 配置RedisTemplate Bean
     */
    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);
        
        // 设置key序列化器
        template.setKeySerializer(new StringRedisSerializer());
        template.setHashKeySerializer(new StringRedisSerializer());
        
        // 设置value序列化器
        template.setValueSerializer(new GenericJackson2JsonRedisSerializer());
        template.setHashValueSerializer(new GenericJackson2JsonRedisSerializer());
        
        template.afterPropertiesSet();
        return template;
    }
}