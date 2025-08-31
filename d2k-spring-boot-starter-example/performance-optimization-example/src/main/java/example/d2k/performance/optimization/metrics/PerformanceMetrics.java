package example.d2k.performance.optimization.metrics;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.LongAdder;

/**
 * 性能指标数据类
 * 用于收集和统计Kafka生产者和消费者的性能指标
 * @author xiajuan96
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PerformanceMetrics {
    
    // 基本信息
    private String instanceId;
    private String nodeId;
    private String clientType; // PRODUCER 或 CONSUMER
    
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime timestamp;
    
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime startTime;
    
    // 生产者指标
    private final LongAdder totalMessagesSent = new LongAdder();
    private final LongAdder totalBytesSent = new LongAdder();
    private final LongAdder totalSendErrors = new LongAdder();
    private final AtomicLong avgSendLatency = new AtomicLong(0);
    private final AtomicLong maxSendLatency = new AtomicLong(0);
    private final AtomicLong minSendLatency = new AtomicLong(Long.MAX_VALUE);
    
    // 消费者指标
    private final LongAdder totalMessagesConsumed = new LongAdder();
    private final LongAdder totalBytesConsumed = new LongAdder();
    private final LongAdder totalConsumeErrors = new LongAdder();
    private final AtomicLong avgProcessingTime = new AtomicLong(0);
    private final AtomicLong maxProcessingTime = new AtomicLong(0);
    private final AtomicLong minProcessingTime = new AtomicLong(Long.MAX_VALUE);
    
    // 连接指标
    private final AtomicLong connectionCount = new AtomicLong(0);
    private final AtomicLong connectionErrors = new AtomicLong(0);
    private final AtomicLong reconnectCount = new AtomicLong(0);
    
    // 批处理指标
    private final LongAdder totalBatches = new LongAdder();
    private final AtomicLong avgBatchSize = new AtomicLong(0);
    private final AtomicLong maxBatchSize = new AtomicLong(0);
    private final AtomicLong avgBatchLatency = new AtomicLong(0);
    
    // 内存指标
    private final AtomicLong usedMemory = new AtomicLong(0);
    private final AtomicLong maxMemory = new AtomicLong(0);
    private final AtomicLong bufferPoolSize = new AtomicLong(0);
    private final AtomicLong bufferPoolUsed = new AtomicLong(0);
    
    // 线程池指标
    private final AtomicLong activeThreads = new AtomicLong(0);
    private final AtomicLong maxThreads = new AtomicLong(0);
    private final AtomicLong queueSize = new AtomicLong(0);
    private final AtomicLong completedTasks = new AtomicLong(0);
    
    // Topic指标
    private final Map<String, TopicMetrics> topicMetrics = new ConcurrentHashMap<>();
    
    // 分区指标
    private final Map<String, PartitionMetrics> partitionMetrics = new ConcurrentHashMap<>();
    
    // 错误统计
    private final Map<String, AtomicLong> errorCounts = new ConcurrentHashMap<>();
    
    // 性能等级
    private PerformanceLevel performanceLevel = PerformanceLevel.UNKNOWN;
    
    public PerformanceMetrics() {
        this.timestamp = LocalDateTime.now();
        this.startTime = LocalDateTime.now();
    }
    
    public PerformanceMetrics(String instanceId, String clientType) {
        this();
        this.instanceId = instanceId;
        this.clientType = clientType;
    }
    
    /**
     * 记录发送消息
     */
    public void recordMessageSent(long bytes, long latency) {
        totalMessagesSent.increment();
        totalBytesSent.add(bytes);
        updateSendLatency(latency);
    }
    
    /**
     * 记录发送错误
     */
    public void recordSendError(String errorType) {
        totalSendErrors.increment();
        errorCounts.computeIfAbsent(errorType, k -> new AtomicLong(0)).incrementAndGet();
    }
    
    /**
     * 记录消费消息
     */
    public void recordMessageConsumed(long bytes, long processingTime) {
        totalMessagesConsumed.increment();
        totalBytesConsumed.add(bytes);
        updateProcessingTime(processingTime);
    }
    
    /**
     * 记录消费错误
     */
    public void recordConsumeError(String errorType) {
        totalConsumeErrors.increment();
        errorCounts.computeIfAbsent(errorType, k -> new AtomicLong(0)).incrementAndGet();
    }
    
    /**
     * 记录批处理
     */
    public void recordBatch(int batchSize, long batchLatency) {
        totalBatches.increment();
        updateBatchSize(batchSize);
        updateBatchLatency(batchLatency);
    }
    
    /**
     * 更新发送延迟
     */
    private void updateSendLatency(long latency) {
        // 更新平均延迟（简化计算）
        long currentAvg = avgSendLatency.get();
        long newAvg = (currentAvg + latency) / 2;
        avgSendLatency.set(newAvg);
        
        // 更新最大延迟
        long currentMax = maxSendLatency.get();
        if (latency > currentMax) {
            maxSendLatency.compareAndSet(currentMax, latency);
        }
        
        // 更新最小延迟
        long currentMin = minSendLatency.get();
        if (latency < currentMin) {
            minSendLatency.compareAndSet(currentMin, latency);
        }
    }
    
    /**
     * 更新处理时间
     */
    private void updateProcessingTime(long processingTime) {
        // 更新平均处理时间
        long currentAvg = avgProcessingTime.get();
        long newAvg = (currentAvg + processingTime) / 2;
        avgProcessingTime.set(newAvg);
        
        // 更新最大处理时间
        long currentMax = maxProcessingTime.get();
        if (processingTime > currentMax) {
            maxProcessingTime.compareAndSet(currentMax, processingTime);
        }
        
        // 更新最小处理时间
        long currentMin = minProcessingTime.get();
        if (processingTime < currentMin) {
            minProcessingTime.compareAndSet(currentMin, processingTime);
        }
    }
    
    /**
     * 更新批处理大小
     */
    private void updateBatchSize(int batchSize) {
        // 更新平均批处理大小
        long currentAvg = avgBatchSize.get();
        long newAvg = (currentAvg + batchSize) / 2;
        avgBatchSize.set(newAvg);
        
        // 更新最大批处理大小
        long currentMax = maxBatchSize.get();
        if (batchSize > currentMax) {
            maxBatchSize.compareAndSet(currentMax, batchSize);
        }
    }
    
    /**
     * 更新批处理延迟
     */
    private void updateBatchLatency(long batchLatency) {
        long currentAvg = avgBatchLatency.get();
        long newAvg = (currentAvg + batchLatency) / 2;
        avgBatchLatency.set(newAvg);
    }
    
    /**
     * 获取Topic指标
     */
    public TopicMetrics getTopicMetrics(String topic) {
        return topicMetrics.computeIfAbsent(topic, k -> new TopicMetrics(topic));
    }
    
    /**
     * 获取分区指标
     */
    public PartitionMetrics getPartitionMetrics(String topicPartition) {
        return partitionMetrics.computeIfAbsent(topicPartition, k -> new PartitionMetrics(topicPartition));
    }
    
    /**
     * 计算吞吐量（消息/秒）
     */
    public double getMessageThroughput() {
        long duration = java.time.Duration.between(startTime, timestamp).getSeconds();
        if (duration == 0) return 0.0;
        
        long totalMessages = "PRODUCER".equals(clientType) ? 
            totalMessagesSent.sum() : totalMessagesConsumed.sum();
        return (double) totalMessages / duration;
    }
    
    /**
     * 计算字节吞吐量（字节/秒）
     */
    public double getByteThroughput() {
        long duration = java.time.Duration.between(startTime, timestamp).getSeconds();
        if (duration == 0) return 0.0;
        
        long totalBytes = "PRODUCER".equals(clientType) ? 
            totalBytesSent.sum() : totalBytesConsumed.sum();
        return (double) totalBytes / duration;
    }
    
    /**
     * 计算错误率
     */
    public double getErrorRate() {
        long totalMessages = "PRODUCER".equals(clientType) ? 
            totalMessagesSent.sum() : totalMessagesConsumed.sum();
        long totalErrors = "PRODUCER".equals(clientType) ? 
            totalSendErrors.sum() : totalConsumeErrors.sum();
        
        if (totalMessages == 0) return 0.0;
        return (double) totalErrors / totalMessages;
    }
    
    /**
     * 计算内存使用率
     */
    public double getMemoryUsageRate() {
        long max = maxMemory.get();
        if (max == 0) return 0.0;
        return (double) usedMemory.get() / max;
    }
    
    /**
     * 计算缓冲池使用率
     */
    public double getBufferPoolUsageRate() {
        long poolSize = bufferPoolSize.get();
        if (poolSize == 0) return 0.0;
        return (double) bufferPoolUsed.get() / poolSize;
    }
    
    /**
     * 评估性能等级
     */
    public PerformanceLevel evaluatePerformanceLevel() {
        double throughput = getMessageThroughput();
        double errorRate = getErrorRate();
        long avgLatency = "PRODUCER".equals(clientType) ? 
            avgSendLatency.get() : avgProcessingTime.get();
        
        // 性能评估逻辑
        if (errorRate > 0.05) { // 错误率超过5%
            performanceLevel = PerformanceLevel.POOR;
        } else if (avgLatency > 1000) { // 平均延迟超过1秒
            performanceLevel = PerformanceLevel.POOR;
        } else if (throughput < 100) { // 吞吐量低于100消息/秒
            performanceLevel = PerformanceLevel.FAIR;
        } else if (throughput < 1000 && avgLatency < 100) {
            performanceLevel = PerformanceLevel.GOOD;
        } else if (throughput >= 1000 && avgLatency < 50 && errorRate < 0.01) {
            performanceLevel = PerformanceLevel.EXCELLENT;
        } else {
            performanceLevel = PerformanceLevel.GOOD;
        }
        
        return performanceLevel;
    }
    
    /**
     * 重置指标
     */
    public void reset() {
        totalMessagesSent.reset();
        totalBytesSent.reset();
        totalSendErrors.reset();
        totalMessagesConsumed.reset();
        totalBytesConsumed.reset();
        totalConsumeErrors.reset();
        totalBatches.reset();
        
        avgSendLatency.set(0);
        maxSendLatency.set(0);
        minSendLatency.set(Long.MAX_VALUE);
        avgProcessingTime.set(0);
        maxProcessingTime.set(0);
        minProcessingTime.set(Long.MAX_VALUE);
        avgBatchSize.set(0);
        maxBatchSize.set(0);
        avgBatchLatency.set(0);
        
        connectionCount.set(0);
        connectionErrors.set(0);
        reconnectCount.set(0);
        
        usedMemory.set(0);
        maxMemory.set(0);
        bufferPoolSize.set(0);
        bufferPoolUsed.set(0);
        
        activeThreads.set(0);
        maxThreads.set(0);
        queueSize.set(0);
        completedTasks.set(0);
        
        topicMetrics.clear();
        partitionMetrics.clear();
        errorCounts.clear();
        
        startTime = LocalDateTime.now();
        timestamp = LocalDateTime.now();
        performanceLevel = PerformanceLevel.UNKNOWN;
    }
    
    // Getters and Setters
    public String getInstanceId() { return instanceId; }
    public void setInstanceId(String instanceId) { this.instanceId = instanceId; }
    
    public String getNodeId() { return nodeId; }
    public void setNodeId(String nodeId) { this.nodeId = nodeId; }
    
    public String getClientType() { return clientType; }
    public void setClientType(String clientType) { this.clientType = clientType; }
    
    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }
    
    public LocalDateTime getStartTime() { return startTime; }
    public void setStartTime(LocalDateTime startTime) { this.startTime = startTime; }
    
    public long getTotalMessagesSent() { return totalMessagesSent.sum(); }
    public long getTotalBytesSent() { return totalBytesSent.sum(); }
    public long getTotalSendErrors() { return totalSendErrors.sum(); }
    public long getAvgSendLatency() { return avgSendLatency.get(); }
    public long getMaxSendLatency() { return maxSendLatency.get(); }
    public long getMinSendLatency() { return minSendLatency.get() == Long.MAX_VALUE ? 0 : minSendLatency.get(); }
    
    public long getTotalMessagesConsumed() { return totalMessagesConsumed.sum(); }
    public long getTotalBytesConsumed() { return totalBytesConsumed.sum(); }
    public long getTotalConsumeErrors() { return totalConsumeErrors.sum(); }
    public long getAvgProcessingTime() { return avgProcessingTime.get(); }
    public long getMaxProcessingTime() { return maxProcessingTime.get(); }
    public long getMinProcessingTime() { return minProcessingTime.get() == Long.MAX_VALUE ? 0 : minProcessingTime.get(); }
    
    public long getConnectionCount() { return connectionCount.get(); }
    public void setConnectionCount(long connectionCount) { this.connectionCount.set(connectionCount); }
    
    public long getConnectionErrors() { return connectionErrors.get(); }
    public void setConnectionErrors(long connectionErrors) { this.connectionErrors.set(connectionErrors); }
    
    public long getReconnectCount() { return reconnectCount.get(); }
    public void setReconnectCount(long reconnectCount) { this.reconnectCount.set(reconnectCount); }
    
    public long getTotalBatches() { return totalBatches.sum(); }
    public long getAvgBatchSize() { return avgBatchSize.get(); }
    public long getMaxBatchSize() { return maxBatchSize.get(); }
    public long getAvgBatchLatency() { return avgBatchLatency.get(); }
    
    public long getUsedMemory() { return usedMemory.get(); }
    public void setUsedMemory(long usedMemory) { this.usedMemory.set(usedMemory); }
    
    public long getMaxMemory() { return maxMemory.get(); }
    public void setMaxMemory(long maxMemory) { this.maxMemory.set(maxMemory); }
    
    public long getBufferPoolSize() { return bufferPoolSize.get(); }
    public void setBufferPoolSize(long bufferPoolSize) { this.bufferPoolSize.set(bufferPoolSize); }
    
    public long getBufferPoolUsed() { return bufferPoolUsed.get(); }
    public void setBufferPoolUsed(long bufferPoolUsed) { this.bufferPoolUsed.set(bufferPoolUsed); }
    
    public long getActiveThreads() { return activeThreads.get(); }
    public void setActiveThreads(long activeThreads) { this.activeThreads.set(activeThreads); }
    
    public long getMaxThreads() { return maxThreads.get(); }
    public void setMaxThreads(long maxThreads) { this.maxThreads.set(maxThreads); }
    
    public long getQueueSize() { return queueSize.get(); }
    public void setQueueSize(long queueSize) { this.queueSize.set(queueSize); }
    
    public long getCompletedTasks() { return completedTasks.get(); }
    public void setCompletedTasks(long completedTasks) { this.completedTasks.set(completedTasks); }
    
    public Map<String, TopicMetrics> getTopicMetrics() { return topicMetrics; }
    public Map<String, PartitionMetrics> getPartitionMetrics() { return partitionMetrics; }
    public Map<String, AtomicLong> getErrorCounts() { return errorCounts; }
    
    public PerformanceLevel getPerformanceLevel() { return performanceLevel; }
    public void setPerformanceLevel(PerformanceLevel performanceLevel) { this.performanceLevel = performanceLevel; }
    
    /**
     * 性能等级枚举
     */
    public enum PerformanceLevel {
        EXCELLENT("优秀", "性能表现优秀，各项指标均达到最佳水平"),
        GOOD("良好", "性能表现良好，大部分指标正常"),
        FAIR("一般", "性能表现一般，部分指标需要优化"),
        POOR("较差", "性能表现较差，需要立即优化"),
        UNKNOWN("未知", "性能状态未知，需要收集更多数据");
        
        private final String displayName;
        private final String description;
        
        PerformanceLevel(String displayName, String description) {
            this.displayName = displayName;
            this.description = description;
        }
        
        public String getDisplayName() { return displayName; }
        public String getDescription() { return description; }
    }
    
    /**
     * Topic指标
     */
    public static class TopicMetrics {
        private final String topic;
        private final LongAdder messageCount = new LongAdder();
        private final LongAdder byteCount = new LongAdder();
        private final LongAdder errorCount = new LongAdder();
        private final AtomicLong avgLatency = new AtomicLong(0);
        
        public TopicMetrics(String topic) {
            this.topic = topic;
        }
        
        public void recordMessage(long bytes, long latency) {
            messageCount.increment();
            byteCount.add(bytes);
            
            // 更新平均延迟
            long currentAvg = avgLatency.get();
            long newAvg = (currentAvg + latency) / 2;
            avgLatency.set(newAvg);
        }
        
        public void recordError() {
            errorCount.increment();
        }
        
        // Getters
        public String getTopic() { return topic; }
        public long getMessageCount() { return messageCount.sum(); }
        public long getByteCount() { return byteCount.sum(); }
        public long getErrorCount() { return errorCount.sum(); }
        public long getAvgLatency() { return avgLatency.get(); }
        
        public double getErrorRate() {
            long total = messageCount.sum();
            if (total == 0) return 0.0;
            return (double) errorCount.sum() / total;
        }
    }
    
    /**
     * 分区指标
     */
    public static class PartitionMetrics {
        private final String topicPartition;
        private final LongAdder messageCount = new LongAdder();
        private final LongAdder byteCount = new LongAdder();
        private final AtomicLong currentOffset = new AtomicLong(0);
        private final AtomicLong lag = new AtomicLong(0);
        
        public PartitionMetrics(String topicPartition) {
            this.topicPartition = topicPartition;
        }
        
        public void recordMessage(long bytes, long offset) {
            messageCount.increment();
            byteCount.add(bytes);
            currentOffset.set(offset);
        }
        
        // Getters
        public String getTopicPartition() { return topicPartition; }
        public long getMessageCount() { return messageCount.sum(); }
        public long getByteCount() { return byteCount.sum(); }
        public long getCurrentOffset() { return currentOffset.get(); }
        public long getLag() { return lag.get(); }
        public void setLag(long lag) { this.lag.set(lag); }
    }
}