# 性能优化示例 (Performance Optimization Example)

本示例演示了如何使用D2K框架进行Kafka消息处理的性能优化，包括高性能配置、实时监控、缓存优化、线程池调优等功能。

## 功能特性

### 核心功能
- **高性能Kafka配置** - 优化的生产者和消费者参数配置
- **实时性能监控** - 自动收集和分析性能指标
- **多级缓存优化** - 本地缓存(Caffeine) + Redis分布式缓存
- **线程池优化** - 异步处理和并发控制
- **批量处理优化** - 提高消息处理吞吐量
- **压缩优化** - 支持LZ4、GZIP等压缩算法

### 监控功能
- **性能指标收集** - 吞吐量、延迟、错误率等关键指标
- **实时告警** - 基于阈值的自动告警机制
- **历史数据分析** - 性能趋势分析和报告
- **Micrometer集成** - 支持Prometheus指标导出
- **健康检查** - 全面的系统健康状态检查

### 高级特性
- **自适应优化** - 根据负载自动调整参数
- **性能测试工具** - 内置压力测试和基准测试
- **缓存统计** - 详细的缓存命中率和性能统计
- **异常处理** - 完善的错误处理和恢复机制

## 快速开始

### 前置条件
1. JDK 8+
2. Maven 3.6+
3. Kafka 2.8+ (运行在 localhost:9092)
4. Redis 6.0+ (运行在 localhost:6379)

### 启动Kafka和Redis

```bash
# 启动Kafka
bin/kafka-server-start.sh config/server.properties

# 启动Redis
redis-server

# 创建测试主题
bin/kafka-topics.sh --create --topic performance-test --bootstrap-server localhost:9092 --partitions 3 --replication-factor 1
```

### 运行应用

```bash
# 编译和运行
mvn clean compile
mvn spring-boot:run

# 或者打包后运行
mvn clean package
java -jar target/performance-optimization-example-1.0.0-SNAPSHOT.jar
```

应用启动后访问：http://localhost:8080/performance

## API 接口

### 性能监控

#### 获取性能指标概览
```http
GET /api/performance/metrics/overview
```

#### 获取指定实例的性能指标
```http
GET /api/performance/metrics/instance/{instanceId}
```

#### 获取历史性能指标
```http
GET /api/performance/metrics/history/{instanceId}?hours=1
```

#### 重置性能指标
```http
POST /api/performance/metrics/reset?instanceId={instanceId}
```

### 性能测试

#### 消息发送性能测试
```http
POST /api/performance/test/send
Content-Type: application/x-www-form-urlencoded

topic=performance-test&messageCount=1000&messageSize=1024&async=true
```

#### 批量发送性能测试
```http
POST /api/performance/test/batch-send
Content-Type: application/x-www-form-urlencoded

topic=performance-test&totalMessages=10000&batchSize=100&messageSize=1024
```

#### 缓存性能测试
```http
POST /api/performance/test/cache
Content-Type: application/x-www-form-urlencoded

operationCount=10000&cacheType=local
```

### 缓存管理

#### 获取缓存统计信息
```http
GET /api/performance/cache/stats
```

#### 清理缓存
```http
POST /api/performance/cache/clear?cacheType=all
```

### 系统监控

#### 获取Micrometer指标
```http
GET /api/performance/metrics/micrometer
```

#### 健康检查
```http
GET /api/performance/health
```

## 性能优化配置

### Kafka生产者优化

```yaml
app:
  performance:
    producer:
      batch-size: 16384          # 批处理大小
      linger-ms: 10              # 批处理等待时间
      buffer-memory: 33554432    # 缓冲区内存大小
      compression-type: lz4      # 压缩类型
      acks: 1                    # 确认模式
      retries: 3                 # 重试次数
      max-in-flight: 5           # 最大飞行请求数
```

### Kafka消费者优化

```yaml
app:
  performance:
    consumer:
      fetch-min-bytes: 1024      # 最小拉取字节数
      fetch-max-wait: 500        # 最大等待时间
      max-poll-records: 500      # 最大拉取记录数
      session-timeout: 30000     # 会话超时时间
      heartbeat-interval: 3000   # 心跳间隔
      concurrency: 3             # 并发消费者数量
```

### 线程池优化

```yaml
app:
  performance:
    threadpool:
      core-size: 10              # 核心线程数
      max-size: 20               # 最大线程数
      queue-capacity: 1000       # 队列容量
      keep-alive: 60             # 线程保活时间（秒）
```

### 缓存优化

```yaml
app:
  performance:
    cache:
      maximum-size: 10000        # 最大缓存条目数
      expire-after-write: 300    # 写入后过期时间（秒）
      expire-after-access: 600   # 访问后过期时间（秒）
```

## 监控和告警

### 性能指标

应用自动收集以下性能指标：

- **吞吐量指标**：消息发送/消费速率
- **延迟指标**：发送延迟、处理时间
- **错误指标**：错误率、错误类型分布
- **系统指标**：内存使用率、线程数、连接数
- **缓存指标**：命中率、驱逐率、大小

### 告警配置

```yaml
app:
  performance:
    alert:
      enabled: true
      throughput-threshold: 100      # 吞吐量告警阈值（msg/s）
      latency-threshold: 1000        # 延迟告警阈值（毫秒）
      error-rate-threshold: 0.05     # 错误率告警阈值（5%）
      memory-threshold: 0.9          # 内存使用率告警阈值（90%）
```

### Prometheus集成

应用支持Prometheus指标导出：

```http
GET /actuator/prometheus
```

可以配合Grafana进行可视化监控。

## 性能测试场景

### 1. 基础性能测试

```bash
# 发送1000条消息测试
curl -X POST "http://localhost:8080/performance/api/performance/test/send" \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "messageCount=1000&messageSize=1024&async=false"
```

### 2. 高并发测试

```bash
# 异步发送10000条消息
curl -X POST "http://localhost:8080/performance/api/performance/test/send" \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "messageCount=10000&messageSize=1024&async=true"
```

### 3. 批量处理测试

```bash
# 批量发送测试
curl -X POST "http://localhost:8080/performance/api/performance/test/batch-send" \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "totalMessages=50000&batchSize=500&messageSize=2048"
```

### 4. 缓存性能测试

```bash
# 本地缓存测试
curl -X POST "http://localhost:8080/performance/api/performance/test/cache" \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "operationCount=100000&cacheType=local"

# Redis缓存测试
curl -X POST "http://localhost:8080/performance/api/performance/test/cache" \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "operationCount=10000&cacheType=redis"
```

## 代码结构

```
src/main/java/example/d2k/performance/optimization/
├── PerformanceOptimizationExampleApplication.java  # 主应用程序类
├── config/
│   └── PerformanceOptimizationConfig.java         # 性能优化配置
├── service/
│   └── PerformanceMonitoringService.java          # 性能监控服务
├── controller/
│   └── PerformanceOptimizationController.java     # 性能优化控制器
├── aspect/
│   └── PerformanceMonitoringAspect.java           # 性能监控切面
└── metrics/
    └── PerformanceMetrics.java                    # 性能指标实体
```

## 最佳实践

### 1. 生产者优化
- 合理设置批处理大小和等待时间
- 选择合适的压缩算法
- 启用幂等性保证消息不重复
- 根据业务需求选择合适的acks级别

### 2. 消费者优化
- 调整拉取参数以平衡延迟和吞吐量
- 合理设置并发消费者数量
- 使用手动提交控制消息确认
- 实现优雅的错误处理和重试机制

### 3. 缓存优化
- 根据数据访问模式选择合适的缓存策略
- 监控缓存命中率和内存使用情况
- 合理设置缓存过期时间
- 实现缓存预热和降级机制

### 4. 监控优化
- 设置合理的告警阈值
- 定期分析性能趋势
- 建立性能基线和SLA
- 实现自动化的性能回归测试

## 故障排除

### 常见问题

1. **Kafka连接失败**
   - 检查Kafka服务是否启动
   - 验证bootstrap-servers配置
   - 检查网络连接和防火墙设置

2. **Redis连接失败**
   - 检查Redis服务是否启动
   - 验证Redis连接配置
   - 检查Redis内存使用情况

3. **性能指标异常**
   - 检查监控服务是否正常运行
   - 验证指标收集配置
   - 查看应用日志中的错误信息

4. **内存使用过高**
   - 调整JVM堆内存设置
   - 优化缓存配置
   - 检查是否存在内存泄漏

### 日志分析

应用日志位置：`logs/performance-optimization.log`

关键日志关键字：
- `性能监控` - 监控相关日志
- `性能告警` - 告警相关日志
- `缓存` - 缓存操作日志
- `ERROR` - 错误日志

## 扩展功能

### 1. 自定义性能指标

```java
@Component
public class CustomMetricsCollector {
    
    @Autowired
    private PerformanceMonitoringService monitoringService;
    
    @EventListener
    public void handleCustomEvent(CustomEvent event) {
        // 收集自定义指标
        monitoringService.recordCustomMetric(event.getType(), event.getValue());
    }
}
```

### 2. 自定义告警规则

```java
@Component
public class CustomAlertRule {
    
    @Scheduled(fixedDelay = 60000)
    public void checkCustomAlert() {
        // 实现自定义告警逻辑
        if (customCondition()) {
            sendAlert("CUSTOM_ALERT", "自定义告警消息");
        }
    }
}
```

### 3. 性能优化建议

应用可以根据收集的性能数据自动生成优化建议：

- 线程池大小调整建议
- 缓存配置优化建议
- Kafka参数调优建议
- 系统资源配置建议

## 注意事项

1. **生产环境部署**
   - 根据实际负载调整配置参数
   - 设置合适的JVM参数
   - 配置日志轮转和清理策略
   - 建立监控和告警体系

2. **安全考虑**
   - 配置Kafka认证和授权
   - 设置Redis密码和访问控制
   - 限制管理接口的访问权限
   - 定期更新依赖库版本

3. **性能调优**
   - 定期进行性能测试
   - 监控关键性能指标
   - 根据业务增长调整配置
   - 建立性能优化的持续改进流程

---

更多信息请参考 [D2K官方文档](https://github.com/ThierrySquirrel/d2k-spring-boot-starter)