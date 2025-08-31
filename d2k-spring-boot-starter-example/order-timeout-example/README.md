# 订单超时处理示例

本示例演示如何使用D2K Spring Boot Starter处理电商订单超时场景，实现订单的自动超时取消机制。

## 功能特性

### 核心功能
- **订单管理**：创建、支付、取消订单
- **自动超时**：订单创建后自动发送延迟消息，超时未支付自动取消
- **灵活配置**：支持自定义订单超时时间
- **状态管理**：完整的订单状态流转（待支付→已支付/已取消/超时取消）

### 技术特性
- **延迟消息**：基于D2K的延迟消息机制
- **数据持久化**：使用JPA + H2数据库
- **RESTful API**：完整的订单管理接口
- **异步处理**：支持高并发消息处理
- **事务管理**：保证数据一致性

## 快速开始

### 前置条件
- Java 8+
- Apache Kafka 2.8+
- Maven 3.6+

### 启动Kafka
```bash
# 启动Zookeeper
bin/zookeeper-server-start.sh config/zookeeper.properties

# 启动Kafka
bin/kafka-server-start.sh config/server.properties
```

### 运行应用
```bash
# 编译项目
mvn clean compile

# 运行应用
mvn spring-boot:run
```

应用启动后访问：http://localhost:8080

## API接口

### 1. 创建订单
```bash
POST /api/orders
参数：
- userId: 用户ID
- productName: 商品名称
- amount: 订单金额
- timeoutMinutes: 超时时间（分钟，默认30）

示例：
curl -X POST "http://localhost:8080/api/orders?userId=1&productName=iPhone&amount=8999&timeoutMinutes=2"
```

### 2. 支付订单
```bash
POST /api/orders/{orderNo}/pay

示例：
curl -X POST "http://localhost:8080/api/orders/ORD1234567890ABCDEF/pay"
```

### 3. 取消订单
```bash
POST /api/orders/{orderNo}/cancel

示例：
curl -X POST "http://localhost:8080/api/orders/ORD1234567890ABCDEF/cancel"
```

### 4. 查询订单详情
```bash
GET /api/orders/{orderNo}

示例：
curl "http://localhost:8080/api/orders/ORD1234567890ABCDEF"
```

### 5. 查询用户订单列表
```bash
GET /api/orders?userId={userId}

示例：
curl "http://localhost:8080/api/orders?userId=1"
```

### 6. 查询待支付订单
```bash
GET /api/orders/pending

示例：
curl "http://localhost:8080/api/orders/pending"
```

### 7. 手动触发超时处理（测试用）
```bash
POST /api/orders/{orderNo}/timeout

示例：
curl -X POST "http://localhost:8080/api/orders/ORD1234567890ABCDEF/timeout"
```

## 业务流程

### 订单超时处理流程
1. **创建订单**：用户创建订单，状态为`PENDING`
2. **发送延迟消息**：系统自动发送延迟消息到`order-timeout` topic
3. **等待支付**：用户可以在超时时间内支付订单
4. **超时处理**：
   - 如果用户已支付，消息处理时跳过
   - 如果订单仍为待支付状态，自动取消订单
5. **状态更新**：订单状态更新为`TIMEOUT_CANCELLED`

### 订单状态说明
- `PENDING`：待支付
- `PAID`：已支付
- `CANCELLED`：手动取消
- `TIMEOUT_CANCELLED`：超时取消
- `COMPLETED`：已完成

## 代码结构

```
src/main/java/example/d2k/order/timeout/
├── OrderTimeoutExampleApplication.java    # 主应用程序
├── entity/
│   ├── Order.java                        # 订单实体
│   └── OrderStatus.java                  # 订单状态枚举
├── repository/
│   └── OrderRepository.java              # 订单数据访问层
├── service/
│   └── OrderService.java                 # 订单业务逻辑
├── consumer/
│   └── OrderTimeoutConsumer.java         # 超时消息消费者
└── controller/
    └── OrderController.java               # 订单REST控制器
```

## 配置说明

### 关键配置项
```yaml
d2k:
  producer:
    bootstrap-servers: localhost:9092
    delay-topic-suffix: "-delay"          # 延迟消息topic后缀
  consumer:
    bootstrap-servers: localhost:9092
    group-id: order-timeout-consumer-group
    concurrency: 3                         # 消费者并发数
    async-processing: true                  # 启用异步处理
```

### 数据库配置
```yaml
spring:
  datasource:
    url: jdbc:h2:mem:order_timeout_db      # H2内存数据库
  jpa:
    hibernate:
      ddl-auto: create-drop                # 自动创建表结构
```

## 监控和调试

### H2数据库控制台
访问：http://localhost:8080/h2-console
- JDBC URL: `jdbc:h2:mem:order_timeout_db`
- 用户名: `sa`
- 密码: （空）

### 健康检查
访问：http://localhost:8080/actuator/health

### 日志配置
- 应用日志：`logs/order-timeout-example.log`
- 日志级别：订单相关为DEBUG，其他为INFO

## 测试场景

### 场景1：正常支付流程
1. 创建订单（超时时间设为5分钟）
2. 在超时前支付订单
3. 观察延迟消息到达时的处理（应该跳过）

### 场景2：订单超时取消
1. 创建订单（超时时间设为1分钟）
2. 不进行支付操作
3. 等待1分钟后观察订单状态变化

### 场景3：手动取消订单
1. 创建订单
2. 手动取消订单
3. 观察延迟消息到达时的处理（应该跳过）

## 扩展功能

### 自定义超时策略
可以根据商品类型、用户等级等因素设置不同的超时时间：

```java
// 在OrderService中扩展
public Order createOrderWithCustomTimeout(Long userId, String productName, 
                                        BigDecimal amount, String productType) {
    Integer timeoutMinutes = calculateTimeoutByProductType(productType);
    return createOrder(userId, productName, amount, timeoutMinutes);
}
```

### 超时提醒功能
可以在订单即将超时前发送提醒消息：

```java
// 发送超时前提醒（如超时前5分钟）
long reminderDelayMs = (order.getTimeoutMinutes() - 5) * 60 * 1000L;
if (reminderDelayMs > 0) {
    d2kTemplate.sendDelayMessage("order-reminder", orderNo, orderNo, reminderDelayMs);
}
```

## 注意事项

1. **时钟同步**：确保应用服务器和Kafka集群时钟同步
2. **消息幂等**：消费者处理逻辑已考虑幂等性
3. **异常处理**：消息处理失败会重试，可配置重试策略
4. **性能调优**：可根据业务量调整消费者并发数和异步线程池大小
5. **数据备份**：生产环境建议使用持久化数据库

## 故障排除

### 常见问题

1. **消息没有延迟投递**
   - 检查Kafka集群是否正常运行
   - 确认D2K配置是否正确

2. **订单状态没有更新**
   - 检查数据库连接
   - 查看应用日志中的异常信息

3. **消费者无法启动**
   - 检查Kafka连接配置
   - 确认topic是否存在

4. **H2控制台无法访问**
   - 确认`spring.h2.console.enabled=true`
   - 检查应用是否正常启动