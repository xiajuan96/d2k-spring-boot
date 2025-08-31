# D2K Spring Boot Starter

[![License: LGPL v3](https://img.shields.io/badge/License-LGPL%20v3-blue.svg)](https://www.gnu.org/licenses/lgpl-3.0)
[![Java Version](https://img.shields.io/badge/Java-8%2B-orange.svg)](https://www.oracle.com/java/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-2.7%2B-green.svg)](https://spring.io/projects/spring-boot)

## 📖 项目概述

D2K Spring Boot Starter 是基于 [d2k-client](https://github.com/xiajuan96/d2k-client) 实现的 Spring Boot 集成套件，为 Spring Boot 应用程序提供快速集成 D2K 延迟消息系统的完整解决方案。

### 🎯 核心特性

- **🚀 自动配置**: 基于 Spring Boot 自动配置机制，零配置启动
- **📦 模块化设计**: Producer 和 Consumer 配置完全分离，支持独立使用
- **🎯 注解驱动**: 提供 `@D2kListener` 注解，简化消费者开发
- **🛠️ 模板支持**: 内置 `D2kTemplate`，简化消息发送
- **⚙️ 配置管理**: 支持通过 `application.yml` 统一管理所有配置参数

### 🚀 适用场景

- **订单超时处理**: 电商订单超时自动取消、支付超时提醒
- **定时任务调度**: 替代传统定时任务，支持动态调度和精确控制
- **业务流程编排**: 多步骤业务流程的延迟执行和状态管理
- **系统解耦**: 通过延迟消息实现系统间的异步通信和解耦

## 🚀 快速开始

### 📋 环境要求

- **Java**: 8 或更高版本
- **Spring Boot**: 2.7.x 或更高版本
- **Apache Kafka**: 2.8.x 或更高版本

### 1️⃣ 添加依赖

```xml
<dependency>
    <groupId>io.github.xiajuan96</groupId>
    <artifactId>d2k-spring-boot-starter</artifactId>
    <version>1.0.0-SNAPSHOT</version>
</dependency>
```

### 2️⃣ 启用 D2K

```java
@SpringBootApplication
@EnableD2k
public class DemoApplication {
    public static void main(String[] args) {
        SpringApplication.run(DemoApplication.class, args);
    }
}
```

### 3️⃣ 基础配置

```yaml
d2k:
  producer:
    bootstrap-servers: localhost:9092
    client-id: demo-producer
  consumer:
    bootstrap-servers: localhost:9092
    group-id: demo-consumer-group
    client-id: demo-consumer
```

### 4️⃣ 发送延迟消息

```java
@Service
public class MessageProducer {
    @Autowired
    private D2kTemplate<String, String> d2kTemplate;
    
    public void sendDelayMessage(String topic, String key, String message) {
        d2kTemplate.send(topic, key, message);
    }
}
```

### 5️⃣ 消费延迟消息

```java
@Component
public class MessageConsumer {
    
    @D2kListener(topic = "order-timeout", groupId = "order-service")
    public void handleOrderTimeout(ConsumerRecord<String, String> record) {
        String orderId = record.key();
        // 处理订单超时逻辑
    }
    
    @D2kListener(topic = "payment-reminder", concurrency = 3)
    public void handlePaymentReminder(String message) {
        // 处理支付提醒逻辑
    }
}
```

## 📚 示例项目

示例项目位于 `d2k-spring-boot-starter-example` 目录下：

- **basic-usage-example**: 基础使用示例
- **order-timeout-example**: 订单超时处理示例
- **payment-retry-example**: 支付重试示例
- **performance-optimization-example**: 性能优化示例
- **monitoring-example**: 监控示例

```bash
# 运行示例
cd d2k-spring-boot-starter-example/basic-usage-example
mvn spring-boot:run
```

## ⚙️ 高级配置

### 模块化配置

```java
// 只使用 Producer
@EnableAutoConfiguration(exclude = {D2kConsumerAutoConfiguration.class})

// 只使用 Consumer  
@EnableAutoConfiguration(exclude = {D2kProducerAutoConfiguration.class})
```

### 性能优化配置

```yaml
d2k:
  producer:
    compression-type: lz4
    linger-ms: 5
    batch-size: 16384
  consumer:
    concurrency: 3
    async:
      enabled: true
      core-pool-size: 5
      max-pool-size: 10
```

## 📖 API 参考

### D2kTemplate 核心方法

```java
// 同步发送
RecordMetadata send(String topic, V value);
RecordMetadata send(String topic, K key, V value);

// 异步发送
CompletableFuture<RecordMetadata> sendAsync(String topic, V value);
CompletableFuture<RecordMetadata> sendAsync(String topic, K key, V value);
```

### @D2kListener 注解参数

```java
@D2kListener(
    topic = "topic-name",           // 必需：主题名称
    groupId = "consumer-group",     // 可选：消费者组ID
    concurrency = 3,                // 可选：并发消费者数量
    asyncProcessing = true          // 可选：是否异步处理
)
```

## ⚠️ 注意事项

- **环境要求**: 确保 Kafka 服务正常运行，网络连接正常
- **配置要点**: 消费者组ID、序列化器配置需匹配
- **性能优化**: 合理设置批处理、并发参数
- **安全建议**: 生产环境启用认证，避免传输敏感信息

## 📄 许可证

本项目采用 [GNU Lesser General Public License v3.0 (LGPL-3.0)](https://www.gnu.org/licenses/lgpl-3.0.html) 开源许可证。

## 🤝 贡献

欢迎贡献代码！请通过以下方式参与：

1. **报告问题**: [提交 Issue](https://github.com/xiajuan96/d2k-spring-boot-starter/issues)
2. **功能请求**: 详细描述使用场景和期望的行为
3. **代码贡献**: 提交 Pull Request，确保代码风格一致并添加测试

## 📞 联系方式

- **GitHub Issues**: [提交问题](https://github.com/xiajuan96/d2k-spring-boot-starter/issues)
- **讨论区**: [GitHub Discussions](https://github.com/xiajuan96/d2k-spring-boot-starter/discussions)

---

**如果这个项目对您有帮助，请给我们一个 ⭐ Star！**