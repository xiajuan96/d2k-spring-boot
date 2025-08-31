# 监控和健康检查示例 (Monitoring Example)

本示例展示了如何使用 D2K Spring Boot Starter 实现全面的监控和健康检查功能，包括实时指标收集、智能告警系统、性能分析和优化建议。

## 功能特性

### 核心功能
- **实时监控指标收集**：自动收集 Kafka、系统、内存、线程、HTTP、缓存等各类指标
- **全面健康检查**：检查数据库、Redis、Kafka 连接状态和系统资源
- **智能告警系统**：基于阈值的多级告警，支持告警抑制和升级
- **性能分析**：提供详细的性能指标分析和优化建议
- **多种通知方式**：支持邮件、Webhook、钉钉、企业微信等通知渠道

### 监控功能
- **Kafka 监控**：消息发送/接收量、错误率、延迟、生产者/消费者状态
- **系统监控**：CPU、内存、堆内存、非堆内存使用率
- **线程监控**：线程数量、状态分布、死锁检测
- **HTTP 监控**：请求量、响应时间、错误率、状态码分布
- **缓存监控**：命中率、未命中率、逐出率、缓存大小
- **连接池监控**：数据库和 Redis 连接池使用情况

### 高级特性
- **指标持久化**：支持数据库、Redis、文件等多种存储方式
- **性能测试**：内置 Kafka、Redis、缓存、HTTP 性能测试工具
- **报表生成**：定时生成监控报表，支持图表和趋势分析
- **监控切面**：通过 AOP 自动监控方法执行时间
- **Prometheus 集成**：支持 Prometheus 指标导出

## 快速开始

### 前置条件
- Java 8+
- Maven 3.6+
- Kafka 2.8+ (可选，用于完整功能体验)
- Redis 6.0+ (可选，用于缓存和告警存储)

### 启动 Kafka 和 Redis (可选)

```bash
# 启动 Kafka (使用 Docker)
docker run -d --name kafka \
  -p 9092:9092 \
  -e KAFKA_ZOOKEEPER_CONNECT=zookeeper:2181 \
  -e KAFKA_ADVERTISED_LISTENERS=PLAINTEXT://localhost:9092 \
  -e KAFKA_OFFSETS_TOPIC_REPLICATION_FACTOR=1 \
  confluentinc/cp-kafka:latest

# 启动 Redis (使用 Docker)
docker run -d --name redis -p 6379:6379 redis:latest
```

### 运行应用

```bash
# 进入项目目录
cd monitoring-example

# 编译并运行
mvn spring-boot:run

# 或者使用 jar 包运行
mvn clean package
java -jar target/monitoring-example-1.0.0-SNAPSHOT.jar
```

应用启动后，访问以下地址：
- 应用首页：http://localhost:8080/monitoring
- 监控概览：http://localhost:8080/monitoring/api/monitoring/overview
- 健康检查：http://localhost:8080/monitoring/actuator/health
- Prometheus 指标：http://localhost:8080/monitoring/actuator/prometheus
- H2 控制台：http://localhost:8080/monitoring/h2-console

## API 接口

### 监控接口

```bash
# 获取监控概览
GET /api/monitoring/overview

# 获取指定实例的监控指标
GET /api/monitoring/metrics/{instanceId}

# 获取历史监控指标
GET /api/monitoring/metrics/history?hours=24&instanceId=xxx

# 重置监控指标
POST /api/monitoring/metrics/reset

# 获取 Micrometer 指标
GET /api/monitoring/micrometer
```

### 健康检查接口

```bash
# 获取健康状态
GET /api/monitoring/health

# 获取详细健康检查结果
GET /actuator/health
```

### 告警接口

```bash
# 获取活跃告警
GET /api/monitoring/alerts

# 获取告警统计
GET /api/monitoring/alerts/stats

# 手动触发测试告警
POST /api/monitoring/alerts/test
```

### 缓存管理接口

```bash
# 获取缓存统计
GET /api/monitoring/cache/stats

# 清理所有缓存
DELETE /api/monitoring/cache/clear
```

### 性能测试接口

```bash
# Kafka 性能测试
POST /api/monitoring/test/kafka?count=1000&batchSize=100

# Redis 性能测试
POST /api/monitoring/test/redis?count=1000&valueSize=1024

# 缓存性能测试
POST /api/monitoring/test/cache?count=1000
```

## 监控配置

### 指标收集配置

```yaml
app:
  monitoring:
    metrics:
      enabled: true
      collection-interval: 60  # 指标收集间隔（秒）
      retention-hours: 24      # 指标保留时间（小时）
      batch-size: 100          # 批量处理大小
```

### 告警配置

```yaml
app:
  monitoring:
    alert:
      enabled: true
      check-interval: 30  # 告警检查间隔（秒）
      
      thresholds:
        kafka-error-rate: 0.05     # Kafka 错误率阈值
        cpu-usage: 0.8             # CPU 使用率阈值
        memory-usage: 0.8          # 内存使用率阈值
        http-response-time: 5000   # HTTP 响应时间阈值（毫秒）
        thread-count: 200          # 线程数量阈值
```

### 通知配置

```yaml
app:
  monitoring:
    notification:
      email:
        enabled: true
        host: smtp.gmail.com
        port: 587
        username: your-email@gmail.com
        password: your-password
        to:
          - admin@example.com
          
      webhook:
        enabled: true
        url: http://your-webhook-url
        timeout: 5000
```

## 监控指标说明

### Kafka 指标
- `kafka.producer.count`：生产者数量
- `kafka.consumer.count`：消费者数量
- `kafka.messages.sent`：发送消息数量
- `kafka.messages.received`：接收消息数量
- `kafka.messages.error`：错误消息数量
- `kafka.latency.avg`：平均延迟
- `kafka.error.rate`：错误率

### 系统指标
- `system.cpu.usage`：CPU 使用率
- `system.memory.usage`：内存使用率
- `system.heap.usage`：堆内存使用率
- `system.nonheap.usage`：非堆内存使用率
- `system.load.average`：系统负载

### 线程指标
- `thread.count`：线程总数
- `thread.daemon.count`：守护线程数
- `thread.peak.count`：峰值线程数
- `thread.deadlock.count`：死锁线程数

### HTTP 指标
- `http.requests.total`：HTTP 请求总数
- `http.requests.error`：HTTP 错误请求数
- `http.response.time.avg`：平均响应时间
- `http.error.rate`：HTTP 错误率

### 缓存指标
- `cache.hits`：缓存命中数
- `cache.misses`：缓存未命中数
- `cache.evictions`：缓存逐出数
- `cache.size`：缓存大小
- `cache.hit.rate`：缓存命中率

## 告警级别

- **INFO**：信息级别，正常状态变化
- **WARNING**：警告级别，需要关注但不紧急
- **ERROR**：错误级别，需要及时处理
- **CRITICAL**：严重级别，需要立即处理

## 性能优化建议

### Kafka 优化
- 调整 `batch-size` 和 `linger-ms` 提高吞吐量
- 使用压缩减少网络传输
- 合理设置 `fetch-min-bytes` 和 `max-poll-records`

### 缓存优化
- 根据业务场景调整缓存大小和过期时间
- 监控缓存命中率，优化缓存策略
- 使用分层缓存提高性能

### 线程池优化
- 根据 CPU 核数和业务特点调整线程池大小
- 监控线程池队列长度，避免任务积压
- 使用不同的线程池处理不同类型的任务

## 故障排除

### 常见问题

1. **Kafka 连接失败**
   - 检查 Kafka 服务是否启动
   - 验证 `bootstrap-servers` 配置
   - 检查网络连接和防火墙设置

2. **Redis 连接失败**
   - 检查 Redis 服务是否启动
   - 验证 Redis 连接配置
   - 检查 Redis 密码和权限设置

3. **内存使用率过高**
   - 检查是否有内存泄漏
   - 调整 JVM 堆内存大小
   - 优化缓存配置和数据结构

4. **告警频繁触发**
   - 调整告警阈值
   - 启用告警抑制功能
   - 检查系统资源是否充足

### 日志分析

```bash
# 查看应用日志
tail -f logs/application.log

# 查看监控相关日志
grep "monitoring" logs/application.log

# 查看告警日志
grep "alert" logs/application.log
```

## 扩展功能

### 自定义监控指标

```java
@Component
public class CustomMetrics {
    
    @Autowired
    private MeterRegistry meterRegistry;
    
    public void recordCustomMetric(String name, double value) {
        Gauge.builder("custom." + name)
            .register(meterRegistry, () -> value);
    }
}
```

### 自定义告警规则

```java
@Component
public class CustomAlertRule {
    
    @EventListener
    public void handleCustomEvent(CustomEvent event) {
        if (shouldTriggerAlert(event)) {
            alertService.triggerAlert(
                AlertLevel.WARNING,
                "Custom Alert",
                "Custom condition met: " + event.getDescription()
            );
        }
    }
}
```

### 自定义健康检查

```java
@Component
public class CustomHealthIndicator implements HealthIndicator {
    
    @Override
    public Health health() {
        // 自定义健康检查逻辑
        boolean isHealthy = checkCustomCondition();
        
        if (isHealthy) {
            return Health.up()
                .withDetail("custom", "All systems operational")
                .build();
        } else {
            return Health.down()
                .withDetail("custom", "System issue detected")
                .build();
        }
    }
}
```

## 最佳实践

1. **监控策略**
   - 监控关键业务指标和技术指标
   - 设置合理的告警阈值，避免告警疲劳
   - 定期回顾和调整监控配置

2. **性能优化**
   - 定期分析性能指标，识别瓶颈
   - 根据业务增长调整系统配置
   - 使用性能测试验证优化效果

3. **告警管理**
   - 建立告警处理流程和责任分工
   - 使用告警抑制避免重复通知
   - 定期清理和归档历史告警

4. **数据管理**
   - 合理设置数据保留期限
   - 定期清理过期数据
   - 备份重要的监控配置

## 注意事项

1. **资源消耗**：监控功能会消耗一定的系统资源，请根据实际情况调整监控频率和保留时间
2. **网络依赖**：部分功能依赖 Kafka 和 Redis，请确保网络连接稳定
3. **权限配置**：生产环境请配置适当的访问权限和安全策略
4. **数据隐私**：监控数据可能包含敏感信息，请注意数据保护
5. **版本兼容**：请确保 Kafka、Redis 等组件版本与应用兼容

## 相关链接

- [Spring Boot Actuator 文档](https://docs.spring.io/spring-boot/docs/current/reference/html/actuator.html)
- [Micrometer 文档](https://micrometer.io/docs)
- [Prometheus 文档](https://prometheus.io/docs/)
- [Kafka 监控指南](https://kafka.apache.org/documentation/#monitoring)
- [Redis 监控指南](https://redis.io/topics/admin)

---

**作者**: xiajuan96  
**版本**: 1.0.0  
**更新时间**: 2024-01-20