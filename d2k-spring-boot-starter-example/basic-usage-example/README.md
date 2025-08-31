# D2K Spring Boot Starter - Basic Usage Example

这是一个展示如何使用 D2K Spring Boot Starter 的基础示例应用。

## 功能特性

- **延迟消息发送**: 演示如何发送延迟消息
- **消息消费**: 展示不同类型的消息消费方式
- **自动配置**: 使用 Spring Boot 自动配置功能
- **资源监控**: 实时监控应用资源使用情况
- **自动关闭**: 应用运行60秒后自动关闭

## 前置条件

1. **Java 8+**: 确保安装了 Java 8 或更高版本
2. **Apache Kafka**: 需要运行 Kafka 服务器
   ```bash
   # 启动 Zookeeper
   bin/zookeeper-server-start.sh config/zookeeper.properties
   
   # 启动 Kafka
   bin/kafka-server-start.sh config/server.properties
   ```

## 快速开始

### 1. 构建项目

```bash
# 在项目根目录执行
mvn clean install
```

### 2. 运行示例

```bash
# 进入示例目录
cd basic-usage-example

# 运行应用
mvn spring-boot:run
```

### 3. 观察输出

应用启动后会自动执行以下操作：

1. **初始化阶段**: 启动 Spring 容器，初始化 D2K 组件
2. **消息发送阶段**: 发送多种类型的延迟消息
3. **消息消费阶段**: 消费并处理接收到的消息
4. **监控阶段**: 定期输出系统资源状态
5. **关闭阶段**: 60秒后自动关闭应用

## 配置说明

### application.yml

```yaml
d2k:
  producer:
    bootstrap-servers: localhost:9092
    topic-delays:
      test-topic: 5000      # 5秒延迟
      another-topic: 3000   # 3秒延迟
      notification-topic: 1000  # 1秒延迟
  
  consumer:
    bootstrap-servers: localhost:9092
    group-id: d2k-spring-consumer-group
    concurrency: 2
```

### 关键配置项

- `bootstrap-servers`: Kafka 服务器地址
- `topic-delays`: 各主题的延迟时间配置（毫秒）
- `group-id`: 消费者组ID
- `concurrency`: 消费者并发数

## 代码结构

```
src/main/java/example/d2k/spring/boot/autoconfigure/
└── D2kSpringExampleApplication.java    # 主应用类
    ├── ApplicationShutdownScheduler    # 自动关闭调度器
    ├── MessageProducer                 # 消息生产者
    └── MessageConsumer                 # 消息消费者
```

### 核心组件

#### 1. MessageProducer（消息生产者）

```java
@Component
public static class MessageProducer implements CommandLineRunner {
    @Autowired
    private D2kTemplate<String, String> d2kTemplate;
    
    // 发送延迟消息
    d2kTemplate.send("test-topic", "key1", "这是一条延迟消息");
    
    // 异步发送
    d2kTemplate.sendAsync("test-topic", "key2", "异步消息");
    
    // 同步发送
    d2kTemplate.sendSync("test-topic", "key3", "同步消息");
}
```

#### 2. MessageConsumer（消息消费者）

```java
@Component
public static class MessageConsumer {
    // 处理完整消息记录
    @D2kListener(topic = "test-topic", groupId = "test-group-1")
    public void handleDelayMessage(ConsumerRecord<String, String> record) {
        // 处理逻辑
    }
    
    // 处理简单消息内容
    @D2kListener(topic = "another-topic", groupId = "test-group-2", concurrency = 2)
    public void handleSimpleMessage(String message) {
        // 处理逻辑
    }
    
    // 无参数处理方法
    @D2kListener(topic = "notification-topic", groupId = "notification-group")
    public void handleNotification() {
        // 处理逻辑
    }
}
```

## 监控功能

应用内置了资源监控功能，会定期输出：

- **内存使用情况**: 总内存、已用内存、空闲内存
- **线程状态**: 活跃线程数
- **处理器信息**: 可用处理器数量
- **Spring容器状态**: Bean数量、容器状态
- **消息处理统计**: 处理消息数量、处理耗时

## 故障排除

### 1. Kafka 连接失败

```
错误: Failed to send message
解决: 检查 Kafka 服务是否启动，确认 bootstrap-servers 配置正确
```

### 2. 主题不存在

```
错误: Topic 'test-topic' does not exist
解决: Kafka 会自动创建主题，或手动创建：
bin/kafka-topics.sh --create --topic test-topic --bootstrap-server localhost:9092
```

### 3. 端口冲突

```
错误: Port 8080 is already in use
解决: 修改 application.yml 中的 server.port 配置
```

## 扩展示例

基于此示例，你可以：

1. **添加更多消息类型**: 支持 JSON、对象序列化等
2. **集成数据库**: 添加消息持久化功能
3. **添加监控**: 集成 Micrometer、Prometheus 等
4. **错误处理**: 添加重试、死信队列等机制
5. **性能优化**: 调整批处理、并发参数等

## 相关文档

- [D2K Spring Boot Starter 文档](../..//README.md)
- [Apache Kafka 官方文档](https://kafka.apache.org/documentation/)
- [Spring Boot 官方文档](https://spring.io/projects/spring-boot)