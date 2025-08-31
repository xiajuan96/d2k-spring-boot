# 用户行为分析延迟处理示例

本示例演示如何使用 D2K 框架实现用户行为事件的收集、分析和延迟处理。通过延迟处理机制，可以有效减少实时处理压力，提高系统性能和稳定性。

## 功能特性

### 1. 用户行为事件收集
- 支持多种用户行为事件类型（页面访问、点击、搜索、购买等）
- 自动记录事件时间戳、用户信息、设备信息等
- 支持自定义事件属性和扩展字段
- 提供事件验证和数据清洗功能

### 2. 延迟处理机制
- 对特定类型事件进行延迟处理，避免实时处理压力
- 可配置延迟时间和处理策略
- 支持动态调整延迟参数
- 提供延迟事件监控和管理

### 3. 批量处理优化
- 将相似事件进行批量处理，提高处理效率
- 可配置批量大小和处理间隔
- 支持按事件类型、用户、时间等维度进行批量分组
- 提供批量处理进度监控

### 4. 智能重试机制
- 对处理失败的事件进行自动重试
- 支持指数退避重试策略
- 可配置最大重试次数和重试间隔
- 提供重试状态跟踪和失败分析

### 5. 完整状态管理
- 支持多种处理状态（待处理、处理中、已处理、失败等）
- 提供状态转换规则和验证
- 支持状态查询和统计分析
- 提供状态变更历史记录

### 6. Redis缓存支持
- 使用Redis缓存用户会话信息
- 缓存实时统计数据，提高查询性能
- 支持缓存过期策略和数据同步
- 提供缓存监控和管理功能

### 7. 数据持久化
- 使用H2内存数据库存储事件数据（开发环境）
- 支持MySQL、PostgreSQL等生产数据库
- 提供数据备份和恢复功能
- 支持数据归档和清理策略

### 8. RESTful API
- 提供完整的事件管理API接口
- 支持事件创建、查询、更新、删除操作
- 提供批量操作和高级查询功能
- 支持API版本管理和文档生成

### 9. 异步处理
- 支持异步事件处理，提高系统响应性能
- 使用线程池管理异步任务
- 提供异步处理监控和调优
- 支持异步处理结果回调

### 10. 监控支持
- 集成Spring Boot Actuator进行健康检查
- 提供事件处理性能监控
- 支持自定义监控指标和告警
- 提供可视化监控面板

## 快速开始

### 前置条件

1. **Java 17+**
2. **Maven 3.6+**
3. **Kafka 2.8+**
4. **Redis 6.0+**

### 启动依赖服务

#### 启动 Kafka
```bash
# 使用 Docker 启动 Kafka
docker run -d --name kafka \
  -p 9092:9092 \
  -e KAFKA_ZOOKEEPER_CONNECT=zookeeper:2181 \
  -e KAFKA_ADVERTISED_LISTENERS=PLAINTEXT://localhost:9092 \
  -e KAFKA_OFFSETS_TOPIC_REPLICATION_FACTOR=1 \
  apache/kafka:latest
```

#### 启动 Redis
```bash
# 使用 Docker 启动 Redis
docker run -d --name redis \
  -p 6379:6379 \
  redis:latest
```

### 运行应用

```bash
# 在项目根目录下运行
mvn spring-boot:run
```

应用启动后，访问 http://localhost:8083/user-behavior/api/user-behavior/health 检查服务状态。

## API 接口

### 事件管理

#### 创建用户行为事件
```http
POST /api/user-behavior/events
Content-Type: application/json

{
  "userId": "user123",
  "eventType": "PAGE_VIEW",
  "eventName": "访问首页",
  "pagePath": "/home",
  "ipAddress": "192.168.1.100",
  "deviceType": "PC",
  "eventProperties": {
    "sessionId": "session123",
    "userAgent": "Mozilla/5.0...",
    "referrer": "https://google.com"
  }
}
```

#### 批量创建事件
```http
POST /api/user-behavior/events/batch
Content-Type: application/json

{
  "events": [
    {
      "userId": "user123",
      "eventType": "PRODUCT_VIEW",
      "eventName": "查看商品",
      "pagePath": "/product/123",
      "eventProperties": {
        "productId": "123",
        "productName": "iPhone 15",
        "price": 7999.00
      }
    }
  ]
}
```

#### 查询用户事件
```http
GET /api/user-behavior/users/{userId}/events?page=0&size=20
```

#### 查询事件详情
```http
GET /api/user-behavior/events/{eventId}
```

### 事件处理

#### 手动重试事件
```http
POST /api/user-behavior/events/{eventId}/retry
```

#### 取消事件处理
```http
POST /api/user-behavior/events/{eventId}/cancel
```

#### 更新事件状态
```http
PUT /api/user-behavior/events/{eventId}/status
Content-Type: application/json

{
  "status": "PROCESSED"
}
```

### 批量操作

#### 触发批量处理
```http
POST /api/user-behavior/events/batch-process
```

#### 触发延迟事件处理
```http
POST /api/user-behavior/events/process-delayed
```

#### 触发重试事件处理
```http
POST /api/user-behavior/events/process-retry
```

#### 清理过期事件
```http
POST /api/user-behavior/events/cleanup
```

### 查询和统计

#### 获取事件统计
```http
GET /api/user-behavior/statistics
```

#### 获取用户统计
```http
GET /api/user-behavior/users/{userId}/statistics
```

#### 查询特定状态事件
```http
GET /api/user-behavior/events/status/{status}?page=0&size=20
```

#### 查询时间范围事件
```http
GET /api/user-behavior/events/time-range?startTime=2024-01-01T00:00:00&endTime=2024-01-31T23:59:59&page=0&size=20
```

### 测试和模拟

#### 模拟用户行为
```http
POST /api/user-behavior/simulate
Content-Type: application/json

{
  "userId": "test_user",
  "eventCount": 50
}
```

## 业务流程

### 用户行为事件处理流程

1. **事件收集**：用户在前端进行操作，触发行为事件
2. **事件创建**：通过API创建用户行为事件记录
3. **延迟判断**：根据事件类型判断是否需要延迟处理
4. **消息发送**：将事件发送到Kafka消息队列
5. **事件消费**：消费者接收并处理事件消息
6. **业务处理**：执行具体的业务逻辑（分析、推荐等）
7. **状态更新**：更新事件处理状态
8. **重试机制**：处理失败时进行自动重试
9. **结果缓存**：将处理结果缓存到Redis
10. **数据归档**：定期清理和归档历史数据

### 事件类型说明

| 事件类型 | 描述 | 延迟处理 | 优先级 |
|---------|------|---------|--------|
| PAGE_VIEW | 页面访问 | 是 | 低 |
| CLICK | 点击事件 | 是 | 低 |
| SEARCH | 搜索行为 | 否 | 中 |
| PRODUCT_VIEW | 商品查看 | 是 | 中 |
| ADD_TO_CART | 加入购物车 | 否 | 高 |
| PURCHASE | 购买行为 | 否 | 高 |
| REGISTER | 用户注册 | 否 | 高 |
| LOGIN | 用户登录 | 否 | 中 |
| LOGOUT | 用户登出 | 是 | 低 |
| ERROR_OCCURRED | 错误事件 | 否 | 高 |

### 处理状态说明

| 状态 | 描述 | 可重试 | 最终状态 |
|------|------|--------|----------|
| PENDING | 待处理 | 是 | 否 |
| PROCESSING | 处理中 | 否 | 否 |
| PROCESSED | 已处理 | 否 | 是 |
| FAILED | 处理失败 | 是 | 否 |
| RETRYING | 重试中 | 否 | 否 |
| RETRY_FAILED | 重试失败 | 否 | 是 |
| DELAYED | 延迟处理 | 是 | 否 |
| CANCELLED | 已取消 | 否 | 是 |
| SKIPPED | 已跳过 | 否 | 是 |
| ARCHIVED | 已归档 | 否 | 是 |

## 代码结构

```
user-behavior-example/
├── src/main/java/example/d2k/user/behavior/
│   ├── entity/
│   │   ├── UserBehaviorEvent.java          # 用户行为事件实体
│   │   ├── BehaviorEventType.java          # 行为事件类型枚举
│   │   └── ProcessStatus.java              # 处理状态枚举
│   ├── repository/
│   │   └── UserBehaviorEventRepository.java # 事件数据访问层
│   ├── service/
│   │   └── UserBehaviorEventService.java   # 事件业务逻辑层
│   ├── consumer/
│   │   └── UserBehaviorConsumer.java       # 事件消费者
│   ├── controller/
│   │   └── UserBehaviorController.java     # REST API控制器
│   └── UserBehaviorExampleApplication.java # 主应用程序类
├── src/main/resources/
│   └── application.yml                     # 应用配置文件
├── pom.xml                                 # Maven依赖配置
└── README.md                               # 项目说明文档
```

## 配置说明

### D2K 配置

```yaml
d2k:
  producer:
    bootstrap-servers: localhost:9092
    key-serializer: org.apache.kafka.common.serialization.StringSerializer
    value-serializer: org.apache.kafka.common.serialization.StringSerializer
    acks: 1
    retries: 3
    enable-idempotence: true
  
  consumer:
    bootstrap-servers: localhost:9092
    group-id: user-behavior-group
    client-id: user-behavior-client
    concurrency: 3
    async: true
    max-retry-attempts: 3
    retry-interval: 1000
```

### 数据库配置

```yaml
spring:
  datasource:
    url: jdbc:h2:mem:user_behavior_db
    driver-class-name: org.h2.Driver
    username: sa
    password:
  
  jpa:
    hibernate:
      ddl-auto: create-drop
    show-sql: true
```

### Redis 配置

```yaml
spring:
  data:
    redis:
      host: localhost
      port: 6379
      database: 0
      timeout: 2000ms
```

### 自定义配置

```yaml
user-behavior:
  delay:
    default-minutes: 5
    max-minutes: 60
    check-interval-seconds: 30
  
  retry:
    max-attempts: 3
    interval-seconds: 60
    backoff-multiplier: 2.0
  
  batch:
    size: 100
    interval-seconds: 60
    timeout-seconds: 300
```

## 监控和调试

### H2 控制台

访问 http://localhost:8083/user-behavior/h2-console

- **JDBC URL**: `jdbc:h2:mem:user_behavior_db`
- **用户名**: `sa`
- **密码**: 空

### 健康检查

```http
GET /actuator/health
```

### 应用信息

```http
GET /actuator/info
```

### 日志配置

日志文件位置：`logs/user-behavior-example.log`

可以通过修改 `application.yml` 中的日志级别来调整日志输出：

```yaml
logging:
  level:
    example.d2k.user.behavior: DEBUG
    com.d2k: DEBUG
```

## 测试场景

### 1. 正常事件处理

```bash
# 创建页面访问事件
curl -X POST http://localhost:8083/user-behavior/api/user-behavior/events \
  -H "Content-Type: application/json" \
  -d '{
    "userId": "user123",
    "eventType": "PAGE_VIEW",
    "eventName": "访问首页",
    "pagePath": "/home",
    "ipAddress": "192.168.1.100",
    "deviceType": "PC"
  }'
```

### 2. 延迟处理测试

```bash
# 创建需要延迟处理的事件
curl -X POST http://localhost:8083/user-behavior/api/user-behavior/events \
  -H "Content-Type: application/json" \
  -d '{
    "userId": "user123",
    "eventType": "USER_BEHAVIOR_ANALYSIS",
    "eventName": "用户行为分析"
  }'

# 手动触发延迟事件处理
curl -X POST http://localhost:8083/user-behavior/api/user-behavior/events/process-delayed
```

### 3. 批量处理测试

```bash
# 模拟大量用户行为事件
curl -X POST http://localhost:8083/user-behavior/api/user-behavior/simulate \
  -H "Content-Type: application/json" \
  -d '{
    "userId": "test_user",
    "eventCount": 100
  }'

# 触发批量处理
curl -X POST http://localhost:8083/user-behavior/api/user-behavior/events/batch-process
```

### 4. 重试机制测试

```bash
# 查询失败的事件
curl http://localhost:8083/user-behavior/api/user-behavior/events/status/FAILED

# 手动重试特定事件
curl -X POST http://localhost:8083/user-behavior/api/user-behavior/events/{eventId}/retry

# 触发所有重试事件处理
curl -X POST http://localhost:8083/user-behavior/api/user-behavior/events/process-retry
```

### 5. 统计查询测试

```bash
# 获取整体统计信息
curl http://localhost:8083/user-behavior/api/user-behavior/statistics

# 获取用户统计信息
curl http://localhost:8083/user-behavior/api/user-behavior/users/user123/statistics

# 查询时间范围内的事件
curl "http://localhost:8083/user-behavior/api/user-behavior/events/time-range?startTime=2024-01-01T00:00:00&endTime=2024-12-31T23:59:59"
```

## 扩展功能

### 1. 自定义事件处理器

可以通过实现自定义的事件处理器来扩展业务逻辑：

```java
@Component
public class CustomEventProcessor {
    
    public void processPageViewEvent(UserBehaviorEvent event) {
        // 自定义页面访问事件处理逻辑
    }
    
    public void processSearchEvent(UserBehaviorEvent event) {
        // 自定义搜索事件处理逻辑
    }
}
```

### 2. 自定义延迟策略

可以根据业务需求实现自定义的延迟处理策略：

```java
@Component
public class CustomDelayStrategy {
    
    public LocalDateTime calculateDelayTime(UserBehaviorEvent event) {
        // 根据事件类型、用户等级等因素计算延迟时间
        return LocalDateTime.now().plusMinutes(getDelayMinutes(event));
    }
}
```

### 3. 事件过滤器

可以添加事件过滤器来过滤不需要处理的事件：

```java
@Component
public class EventFilter {
    
    public boolean shouldProcess(UserBehaviorEvent event) {
        // 根据业务规则判断是否需要处理该事件
        return !isSpamEvent(event) && isValidUser(event.getUserId());
    }
}
```

### 4. 实时推荐系统

基于用户行为事件实现实时推荐：

```java
@Component
public class RecommendationEngine {
    
    public List<String> generateRecommendations(String userId) {
        // 基于用户行为历史生成推荐内容
        List<UserBehaviorEvent> userEvents = getUserRecentEvents(userId);
        return analyzeAndRecommend(userEvents);
    }
}
```

## 注意事项

1. **内存数据库**：本示例使用H2内存数据库，应用重启后数据会丢失。生产环境建议使用MySQL或PostgreSQL。

2. **Redis依赖**：应用依赖Redis进行缓存，请确保Redis服务正常运行。

3. **Kafka配置**：请根据实际环境调整Kafka连接配置。

4. **性能调优**：可以根据实际负载调整批量处理大小、并发数等参数。

5. **监控告警**：生产环境建议配置完善的监控和告警机制。

## 故障排除

### 常见问题

1. **Kafka连接失败**
   - 检查Kafka服务是否启动
   - 确认Kafka地址和端口配置正确
   - 检查网络连接和防火墙设置

2. **Redis连接失败**
   - 检查Redis服务是否启动
   - 确认Redis地址和端口配置正确
   - 检查Redis密码配置

3. **事件处理失败**
   - 查看应用日志了解具体错误信息
   - 检查事件数据格式是否正确
   - 确认业务处理逻辑是否有异常

4. **性能问题**
   - 调整批量处理大小和并发数
   - 优化数据库查询和索引
   - 增加缓存使用

### 日志分析

查看关键日志信息：

```bash
# 查看应用启动日志
tail -f logs/user-behavior-example.log | grep "Started UserBehaviorExampleApplication"

# 查看事件处理日志
tail -f logs/user-behavior-example.log | grep "UserBehaviorEventService"

# 查看错误日志
tail -f logs/user-behavior-example.log | grep "ERROR"
```

### 性能监控

使用JVM监控工具：

```bash
# 查看JVM内存使用情况
jstat -gc <pid>

# 查看线程状态
jstack <pid>

# 生成堆转储
jmap -dump:format=b,file=heap.hprof <pid>
```