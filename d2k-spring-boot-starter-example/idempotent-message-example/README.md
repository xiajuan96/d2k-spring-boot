# 消息幂等性处理示例 (Idempotent Message Example)

本示例演示了如何使用D2K框架实现消息的幂等性处理，确保相同的消息不会被重复处理，提供完整的消息生命周期管理。

## 功能特性

### 核心功能
- **消息幂等性检查** - 基于messageId防止重复消息处理
- **消息状态管理** - 完整的消息处理状态跟踪
- **智能重试机制** - 失败消息自动重试，支持指数退避
- **超时处理** - 自动检测和处理超时消息
- **Redis缓存** - 高性能的幂等性检查缓存
- **数据持久化** - H2内存数据库存储消息记录

### 管理功能
- **RESTful API** - 完整的消息管理接口
- **批量操作** - 支持批量消息发送和处理
- **统计监控** - 实时处理统计和监控指标
- **健康检查** - 服务健康状态检查
- **消息归档** - 自动归档和清理过期消息

### 高级特性
- **异步处理** - 支持高并发异步消息处理
- **多消息类型** - 支持订单、支付、用户、库存等多种消息类型
- **优先级处理** - 支持消息优先级和紧急处理
- **业务规则** - 可配置的业务处理规则

## 快速开始

### 前置条件
1. Java 17+
2. Maven 3.6+
3. Docker (用于运行Kafka和Redis)

### 启动依赖服务

```bash
# 启动Kafka
docker run -d --name kafka -p 9092:9092 apache/kafka:latest

# 启动Redis
docker run -d --name redis -p 6379:6379 redis:latest
```

### 运行应用

```bash
# 进入项目目录
cd idempotent-message-example

# 编译和运行
mvn clean spring-boot:run
```

应用启动后访问: http://localhost:8084

## API 接口

### 消息管理

#### 检查消息是否已处理
```http
GET /api/message-idempotent/check/{messageId}
```

#### 获取消息记录详情
```http
GET /api/message-idempotent/record/{messageId}
```

#### 根据业务键获取消息
```http
GET /api/message-idempotent/records/business/{businessKey}
```

#### 分页查询消息记录
```http
GET /api/message-idempotent/records?messageType=ORDER_CREATED&status=SUCCESS&page=0&size=20
```

### 消息处理

#### 手动重试消息
```http
POST /api/message-idempotent/retry/{messageId}
```

#### 取消消息处理
```http
POST /api/message-idempotent/cancel/{messageId}?reason=手动取消
```

#### 处理超时消息
```http
POST /api/message-idempotent/handle-timeout?timeoutMinutes=30
```

### 批量操作

#### 获取重试队列
```http
GET /api/message-idempotent/retry-queue?limit=50
```

#### 清理过期消息
```http
POST /api/message-idempotent/cleanup?retentionDays=7
```

#### 归档旧消息
```http
POST /api/message-idempotent/archive?archiveDays=3
```

### 查询统计

#### 获取处理统计
```http
GET /api/message-idempotent/statistics?startTime=2024-01-01T00:00:00&endTime=2024-01-02T00:00:00
```

#### 获取今日统计
```http
GET /api/message-idempotent/statistics/today
```

#### 获取消息类型枚举
```http
GET /api/message-idempotent/message-types
```

#### 获取处理状态枚举
```http
GET /api/message-idempotent/processing-statuses
```

### 测试模拟

#### 发送单个测试消息
```http
POST /api/message-idempotent/test/send?businessKey=test123&messageType=TEST_MESSAGE&content=测试消息&shouldFail=false
```

#### 批量发送测试消息
```http
POST /api/message-idempotent/test/batch-send?count=100&failureRate=0.1
```

#### 健康检查
```http
GET /api/message-idempotent/health
```

## 业务流程

### 消息处理流程

1. **消息接收** - 消费者接收Kafka消息
2. **幂等性检查** - 检查消息是否已处理
3. **创建记录** - 创建消息处理记录
4. **开始处理** - 标记消息为处理中状态
5. **业务逻辑** - 执行具体的业务处理
6. **结果标记** - 标记处理成功或失败
7. **重试处理** - 失败消息进入重试队列
8. **状态更新** - 更新最终处理状态

### 消息类型

- **ORDER_CREATED** - 订单创建消息
- **PAYMENT_SUCCESS** - 支付成功消息
- **USER_REGISTRATION** - 用户注册消息
- **INVENTORY_RESERVED** - 库存预留消息
- **NOTIFICATION** - 通知消息
- **SYSTEM_ALERT** - 系统告警消息
- **BUSINESS_EVENT** - 业务事件消息
- **AUDIT_LOG** - 审计日志消息
- **TEST_MESSAGE** - 测试消息

### 处理状态

- **PENDING** - 待处理
- **PROCESSING** - 处理中
- **SUCCESS** - 处理成功
- **FAILED** - 处理失败
- **RETRYING** - 重试中
- **RETRY_FAILED** - 重试失败
- **RETRY_EXHAUSTED** - 重试耗尽
- **SKIPPED** - 已跳过
- **CANCELLED** - 已取消
- **TIMEOUT** - 处理超时
- **ARCHIVED** - 已归档
- **INVALID_MESSAGE** - 无效消息
- **DUPLICATE_MESSAGE** - 重复消息

## 代码结构

```
src/main/java/example/d2k/idempotent/message/
├── IdempotentMessageExampleApplication.java  # 主应用程序类
├── entity/
│   ├── MessageRecord.java                    # 消息记录实体
│   ├── MessageType.java                      # 消息类型枚举
│   ├── MessagePriority.java                  # 消息优先级枚举
│   └── ProcessingStatus.java                 # 处理状态枚举
├── repository/
│   └── MessageRecordRepository.java          # 消息记录仓库
├── service/
│   └── MessageIdempotentService.java         # 消息幂等性服务
├── consumer/
│   └── MessageIdempotentConsumer.java        # 消息消费者
└── controller/
    └── MessageIdempotentController.java       # REST控制器
```

## 配置说明

### D2K配置

```yaml
d2k:
  producer:
    bootstrap-servers: localhost:9092
    enable-idempotence: true  # 启用生产者幂等性
  consumer:
    bootstrap-servers: localhost:9092
    group-id: idempotent-message-group
    enable-auto-commit: false  # 禁用自动提交
    concurrency: 3  # 并发消费者数量
```

### 数据库配置

```yaml
spring:
  datasource:
    url: jdbc:h2:mem:testdb
    driver-class-name: org.h2.Driver
  jpa:
    hibernate:
      ddl-auto: create-drop
    show-sql: true
```

### Redis配置

```yaml
spring:
  data:
    redis:
      host: localhost
      port: 6379
      database: 0
```

### 自定义配置

```yaml
app:
  message:
    idempotent:
      cache:
        enabled: true
        ttl: 3600  # 缓存过期时间
      retry:
        enabled: true
        max-attempts: 5  # 最大重试次数
      timeout:
        enabled: true
        default-timeout: 300000  # 默认超时时间
      cleanup:
        enabled: true
        retention-days: 7  # 消息保留天数
```

## 监控和调试

### H2控制台
- 访问地址: http://localhost:8084/h2-console
- JDBC URL: `jdbc:h2:mem:testdb`
- 用户名: `sa`
- 密码: (空)

### 健康检查
- 应用健康: http://localhost:8084/actuator/health
- 服务健康: http://localhost:8084/api/message-idempotent/health

### 日志配置
- 应用日志: `logs/idempotent-message-example.log`
- 日志级别: DEBUG (消息处理相关)
- SQL日志: 已启用

## 测试场景

### 1. 正常消息处理
```bash
# 发送测试消息
curl -X POST "http://localhost:8084/api/message-idempotent/test/send?businessKey=order123&messageType=ORDER_CREATED"

# 查看处理结果
curl "http://localhost:8084/api/message-idempotent/records/business/order123"
```

### 2. 幂等性验证
```bash
# 发送相同messageId的消息多次
curl -X POST "http://localhost:8084/api/message-idempotent/test/send?businessKey=order123&messageType=ORDER_CREATED"
curl -X POST "http://localhost:8084/api/message-idempotent/test/send?businessKey=order123&messageType=ORDER_CREATED"

# 验证只处理一次
curl "http://localhost:8084/api/message-idempotent/records/business/order123"
```

### 3. 失败重试测试
```bash
# 发送会失败的消息
curl -X POST "http://localhost:8084/api/message-idempotent/test/send?businessKey=fail123&shouldFail=true"

# 查看重试队列
curl "http://localhost:8084/api/message-idempotent/retry-queue"

# 手动重试
curl -X POST "http://localhost:8084/api/message-idempotent/retry/{messageId}"
```

### 4. 批量处理测试
```bash
# 批量发送消息
curl -X POST "http://localhost:8084/api/message-idempotent/test/batch-send?count=100&failureRate=0.1"

# 查看统计信息
curl "http://localhost:8084/api/message-idempotent/statistics/today"
```

### 5. 超时处理测试
```bash
# 查看超时消息
curl "http://localhost:8084/api/message-idempotent/timeout-messages?timeoutMinutes=5"

# 处理超时消息
curl -X POST "http://localhost:8084/api/message-idempotent/handle-timeout?timeoutMinutes=5"
```

### 6. 统计查询测试
```bash
# 查看今日统计
curl "http://localhost:8084/api/message-idempotent/statistics/today"

# 查看指定时间范围统计
curl "http://localhost:8084/api/message-idempotent/statistics?startTime=2024-01-01T00:00:00&endTime=2024-01-02T00:00:00"

# 查看消息类型分布
curl "http://localhost:8084/api/message-idempotent/message-types"
```

## 扩展功能

### 1. 自定义消息处理器
```java
@Component
public class CustomMessageProcessor {
    
    @D2kConsumer(topic = "custom-topic")
    public void processCustomMessage(String message, MessageHeaders headers) {
        String messageId = extractMessageId(headers);
        
        // 幂等性检查
        if (messageIdempotentService.isMessageProcessed(messageId)) {
            return;
        }
        
        // 创建消息记录
        MessageRecord record = messageIdempotentService.createMessageRecord(
            messageId, "custom-business-key", MessageType.BUSINESS_EVENT, message, headers
        );
        
        try {
            // 开始处理
            messageIdempotentService.startProcessing(messageId);
            
            // 自定义业务逻辑
            processCustomBusinessLogic(message);
            
            // 标记成功
            messageIdempotentService.markSuccess(messageId, "处理成功");
        } catch (Exception e) {
            // 标记失败
            messageIdempotentService.markFailure(messageId, "处理失败: " + e.getMessage());
        }
    }
}
```

### 2. 自定义重试策略
```java
@Component
public class CustomRetryStrategy {
    
    public boolean shouldRetry(MessageRecord record) {
        // 自定义重试逻辑
        if (record.getMessageType() == MessageType.PAYMENT_SUCCESS) {
            return record.getRetryCount() < 10; // 支付消息最多重试10次
        }
        return record.getRetryCount() < 3; // 其他消息最多重试3次
    }
    
    public long getRetryDelay(MessageRecord record) {
        // 自定义重试延迟
        return Math.min(1000 * Math.pow(2, record.getRetryCount()), 60000);
    }
}
```

### 3. 消息过滤器
```java
@Component
public class MessageFilter {
    
    public boolean shouldProcess(String messageId, String content, MessageHeaders headers) {
        // 自定义过滤逻辑
        String messageType = headers.get("messageType", String.class);
        
        // 过滤测试消息
        if ("TEST_MESSAGE".equals(messageType) && !isTestEnvironment()) {
            return false;
        }
        
        // 过滤重复业务键
        String businessKey = headers.get("businessKey", String.class);
        if (isDuplicateBusinessKey(businessKey)) {
            return false;
        }
        
        return true;
    }
}
```

### 4. 实时监控告警
```java
@Component
public class MessageMonitor {
    
    @Scheduled(fixedDelay = 60000) // 每分钟检查一次
    public void checkMessageHealth() {
        Map<String, Object> stats = messageIdempotentService.getTodayStatistics();
        
        double failureRate = (Double) stats.get("failureRate");
        if (failureRate > 0.1) { // 失败率超过10%
            sendAlert("消息处理失败率过高: " + failureRate);
        }
        
        long pendingCount = (Long) stats.get("pendingCount");
        if (pendingCount > 1000) { // 待处理消息超过1000条
            sendAlert("待处理消息积压: " + pendingCount);
        }
    }
}
```

## 注意事项

1. **消息ID唯一性**: 确保messageId在全局范围内唯一
2. **缓存一致性**: Redis缓存和数据库数据保持一致
3. **重试策略**: 合理设置重试次数和间隔，避免无限重试
4. **超时设置**: 根据业务特点设置合适的超时时间
5. **资源清理**: 定期清理过期消息，避免数据积累
6. **监控告警**: 设置合适的监控指标和告警阈值
7. **性能优化**: 根据消息量调整并发数和批处理大小
8. **错误处理**: 完善的异常处理和错误恢复机制

## 故障排除

### 常见问题

1. **消息重复处理**
   - 检查messageId是否唯一
   - 验证幂等性检查逻辑
   - 确认Redis连接正常

2. **消息处理失败**
   - 查看错误日志
   - 检查业务逻辑异常
   - 验证数据库连接

3. **重试不生效**
   - 检查重试配置
   - 验证消息状态
   - 确认重试条件

4. **性能问题**
   - 调整并发数配置
   - 优化数据库查询
   - 检查Redis性能

### 调试技巧

1. **启用详细日志**
```yaml
logging:
  level:
    example.d2k.idempotent.message: DEBUG
    org.hibernate.SQL: DEBUG
```

2. **使用H2控制台查看数据**
```sql
-- 查看所有消息记录
SELECT * FROM message_record ORDER BY created_at DESC;

-- 查看失败消息
SELECT * FROM message_record WHERE processing_status = 'FAILED';

-- 查看重试统计
SELECT processing_status, COUNT(*) FROM message_record GROUP BY processing_status;
```

3. **监控关键指标**
   - 消息处理成功率
   - 平均处理时间
   - 重试次数分布
   - 超时消息数量

4. **压力测试**
```bash
# 批量发送大量消息
for i in {1..1000}; do
  curl -X POST "http://localhost:8084/api/message-idempotent/test/send?businessKey=stress-test-$i" &
done
wait

# 查看处理结果
curl "http://localhost:8084/api/message-idempotent/statistics/today"
```