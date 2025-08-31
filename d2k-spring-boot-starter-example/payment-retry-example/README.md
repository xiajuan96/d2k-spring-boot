# 支付结果异步通知重试示例

本示例演示了如何使用 D2K 框架实现支付结果的异步通知重试机制，适用于支付系统中需要向商户系统发送支付结果通知的场景。

## 功能特性

### 核心功能
- **支付通知管理** - 创建、更新、查询支付通知记录
- **自动重试机制** - 支持多种重试策略和间隔配置
- **HTTP回调** - 向商户系统发送支付结果通知
- **状态管理** - 完整的支付和通知状态跟踪
- **延迟消息** - 基于D2K的延迟消息实现重试调度

### 技术特性
- **数据持久化** - 使用JPA和H2数据库存储通知记录
- **RESTful API** - 提供完整的API接口进行管理
- **异步处理** - 支持同步和异步消息处理
- **事务管理** - 确保数据一致性
- **监控支持** - 集成健康检查和指标监控

## 快速开始

### 前置条件
1. Java 8+
2. Maven 3.6+
3. Kafka 2.8+

### 启动 Kafka
```bash
# 使用 Docker 启动 Kafka
docker run -d --name kafka -p 9092:9092 apache/kafka:latest

# 或者使用本地 Kafka
bin/kafka-server-start.sh config/server.properties
```

### 运行应用
```bash
# 在 payment-retry-example 目录下执行
mvn spring-boot:run

# 或者先编译再运行
mvn clean package
java -jar target/payment-retry-example-1.0.0.jar
```

应用启动后访问：http://localhost:8082/payment-retry

## API 接口

### 支付通知管理

#### 1. 创建支付通知
```bash
POST /api/payment/notification
Content-Type: application/x-www-form-urlencoded

paymentId=PAY001&orderNo=ORDER001&amount=100.00&status=SUCCESS&callbackUrl=http://localhost:8082/payment-retry/api/payment/callback/mock&paymentMethod=ALIPAY&transactionId=TXN001
```

#### 2. 更新支付状态
```bash
PUT /api/payment/status
Content-Type: application/x-www-form-urlencoded

paymentId=PAY001&status=SUCCESS&transactionId=TXN001
```

#### 3. 查询支付通知详情
```bash
GET /api/payment/notification/PAY001
```

#### 4. 查询订单的所有支付通知
```bash
GET /api/payment/notifications/order/ORDER001
```

### 重试管理

#### 1. 手动触发重试
```bash
POST /api/payment/retry/PAY001
```

#### 2. 查询需要重试的通知
```bash
GET /api/payment/notifications/retry
```

#### 3. 查询失败的通知
```bash
GET /api/payment/notifications/failed
```

#### 4. 放弃支付通知
```bash
POST /api/payment/abandon/PAY001
Content-Type: application/x-www-form-urlencoded

reason=商户系统维护，暂停通知
```

### 系统接口

#### 1. 健康检查
```bash
GET /api/payment/health
```

#### 2. 获取枚举值
```bash
# 支付状态枚举
GET /api/payment/status/enum

# 通知状态枚举
GET /api/payment/notification/status/enum
```

## 业务流程

### 支付通知重试流程

1. **创建支付通知**
   - 支付系统创建支付通知记录
   - 如果支付状态需要通知，立即发送第一次通知

2. **HTTP回调执行**
   - 向商户回调URL发送POST请求
   - 包含支付ID、订单号、金额、状态等信息

3. **回调结果处理**
   - 成功：更新通知状态为SUCCESS
   - 失败：根据重试策略安排下次重试

4. **自动重试机制**
   - 使用D2K延迟消息实现重试调度
   - 重试间隔：30s → 1m → 5m → 15m → 30m
   - 最大重试次数：5次

5. **最终处理**
   - 重试成功：标记为SUCCESS
   - 达到最大重试次数：标记为RETRY_FAILED
   - 手动放弃：标记为ABANDONED

### 支付状态说明

| 状态 | 描述 | 是否需要通知 |
|------|------|-------------|
| PROCESSING | 支付中 | 否 |
| SUCCESS | 支付成功 | 是 |
| FAILED | 支付失败 | 是 |
| CANCELLED | 支付取消 | 是 |
| TIMEOUT | 支付超时 | 是 |
| REFUNDING | 退款中 | 否 |
| REFUNDED | 已退款 | 是 |

### 通知状态说明

| 状态 | 描述 | 是否可重试 |
|------|------|------------|
| PENDING | 待通知 | 是 |
| NOTIFYING | 通知中 | 否 |
| SUCCESS | 通知成功 | 否 |
| FAILED | 通知失败 | 是 |
| RETRYING | 重试中 | 否 |
| RETRY_FAILED | 重试失败 | 否 |
| ABANDONED | 已放弃 | 否 |

## 代码结构

```
src/main/java/example/d2k/payment/retry/
├── entity/                          # 实体类
│   ├── PaymentNotification.java     # 支付通知实体
│   ├── PaymentStatus.java           # 支付状态枚举
│   └── NotificationStatus.java      # 通知状态枚举
├── repository/                      # 数据访问层
│   └── PaymentNotificationRepository.java
├── service/                         # 业务服务层
│   └── PaymentNotificationService.java
├── consumer/                        # 消息消费者
│   └── PaymentRetryConsumer.java
├── controller/                      # 控制器
│   └── PaymentController.java
└── PaymentRetryExampleApplication.java  # 主应用类
```

## 配置说明

### D2K 配置

```yaml
d2k:
  producer:
    bootstrap-servers: localhost:9092
    enable-delay: true              # 启用延迟消息
    delay-topic-prefix: "d2k-delay" # 延迟消息主题前缀
  
  consumer:
    bootstrap-servers: localhost:9092
    concurrency: 3                  # 并发消费者数量
    async: true                     # 异步处理
    enable-retry: true              # 启用重试
    max-retry-attempts: 3           # 最大重试次数
```

### 数据库配置

```yaml
spring:
  datasource:
    url: jdbc:h2:mem:payment_retry_db
    driver-class-name: org.h2.Driver
  
  jpa:
    hibernate:
      ddl-auto: create-drop
    show-sql: true
```

### 自定义配置

```yaml
payment:
  retry:
    intervals: [30, 60, 300, 900, 1800]  # 重试间隔（秒）
    max-attempts: 5                      # 最大重试次数
    callback-timeout: 30                 # 回调超时时间（秒）
```

## 监控和调试

### H2 数据库控制台
- 访问地址：http://localhost:8082/payment-retry/h2-console
- JDBC URL：`jdbc:h2:mem:payment_retry_db`
- 用户名：`sa`
- 密码：（空）

### 健康检查
- 应用健康：http://localhost:8082/payment-retry/actuator/health
- 业务健康：http://localhost:8082/payment-retry/api/payment/health

### 日志配置
- 应用日志：`logs/payment-retry-example.log`
- 日志级别：DEBUG（开发环境）
- SQL日志：已启用

## 测试场景

### 场景1：正常支付通知
1. 创建支付通知（状态为SUCCESS）
2. 系统自动发送通知到商户回调URL
3. 商户返回成功响应
4. 通知状态更新为SUCCESS

### 场景2：支付通知重试
1. 创建支付通知（状态为SUCCESS）
2. 商户回调URL返回失败响应
3. 系统按重试策略自动重试
4. 查看重试次数和状态变化

### 场景3：手动重试
1. 查询失败的通知记录
2. 手动触发重试
3. 观察重试结果

### 场景4：放弃通知
1. 对于多次重试失败的通知
2. 手动放弃通知
3. 记录放弃原因

## 扩展功能

### 自定义重试策略
可以通过修改 `PaymentNotificationService` 中的 `RETRY_INTERVALS` 数组来自定义重试间隔：

```java
// 自定义重试间隔（秒）
private static final int[] RETRY_INTERVALS = {10, 30, 120, 600, 3600}; // 10s, 30s, 2m, 10m, 1h
```

### 回调签名验证
可以在 `buildCallbackData` 方法中添加签名生成逻辑：

```java
private Map<String, Object> buildCallbackData(PaymentNotification notification) {
    Map<String, Object> data = new HashMap<>();
    // ... 设置数据
    
    // 添加签名
    String signature = generateSignature(data, secretKey);
    data.put("signature", signature);
    
    return data;
}
```

### 通知模板定制
可以根据不同的支付方式或商户配置不同的通知模板和回调格式。

## 注意事项

1. **Kafka 依赖**：确保 Kafka 服务正常运行
2. **回调URL**：商户回调URL必须可访问
3. **重试策略**：根据业务需求调整重试间隔和次数
4. **数据清理**：定期清理过期的通知记录
5. **监控告警**：建议对重试失败的通知设置告警

## 故障排除

### 常见问题

1. **Kafka 连接失败**
   - 检查 Kafka 服务状态
   - 确认端口配置正确

2. **回调请求失败**
   - 检查商户回调URL可访问性
   - 确认网络连接正常

3. **数据库连接问题**
   - 检查 H2 数据库配置
   - 查看数据库连接日志

4. **消息消费异常**
   - 查看消费者日志
   - 检查消息格式是否正确

### 日志分析

关键日志关键字：
- `支付通知创建成功`：通知创建
- `支付通知重试成功/失败`：重试结果
- `HTTP回调执行完成`：回调执行
- `支付通知消息发送成功`：延迟消息发送

通过这些日志可以追踪整个支付通知的生命周期。