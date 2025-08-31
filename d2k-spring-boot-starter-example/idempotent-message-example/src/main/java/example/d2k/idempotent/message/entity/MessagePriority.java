package example.d2k.idempotent.message.entity;

/**
 * 消息优先级枚举
 * 定义消息处理的优先级别
 * @author xiajuan96
 */
public enum MessagePriority {
    
    /**
     * 高优先级 - 需要立即处理的重要消息
     * 如：支付成功、订单创建、库存预留等
     */
    HIGH("高优先级", "需要立即处理的重要消息", 1, 30, 5),
    
    /**
     * 普通优先级 - 正常业务流程消息
     * 如：订单状态更新、用户信息变更等
     */
    NORMAL("普通优先级", "正常业务流程消息", 2, 60, 3),
    
    /**
     * 低优先级 - 非关键业务消息
     * 如：日志记录、统计数据、通知消息等
     */
    LOW("低优先级", "非关键业务消息", 3, 300, 2);
    
    private final String displayName;
    private final String description;
    private final int level;
    private final int defaultTimeoutSeconds;
    private final int defaultMaxRetries;
    
    MessagePriority(String displayName, String description, int level, 
                   int defaultTimeoutSeconds, int defaultMaxRetries) {
        this.displayName = displayName;
        this.description = description;
        this.level = level;
        this.defaultTimeoutSeconds = defaultTimeoutSeconds;
        this.defaultMaxRetries = defaultMaxRetries;
    }
    
    public String getDisplayName() {
        return displayName;
    }
    
    public String getDescription() {
        return description;
    }
    
    public int getLevel() {
        return level;
    }
    
    public int getDefaultTimeoutSeconds() {
        return defaultTimeoutSeconds;
    }
    
    public int getDefaultMaxRetries() {
        return defaultMaxRetries;
    }
    
    /**
     * 判断是否为高优先级
     */
    public boolean isHigh() {
        return this == HIGH;
    }
    
    /**
     * 判断是否为普通优先级
     */
    public boolean isNormal() {
        return this == NORMAL;
    }
    
    /**
     * 判断是否为低优先级
     */
    public boolean isLow() {
        return this == LOW;
    }
    
    /**
     * 比较优先级高低
     * @param other 另一个优先级
     * @return 如果当前优先级更高返回true
     */
    public boolean isHigherThan(MessagePriority other) {
        return this.level < other.level;
    }
    
    /**
     * 比较优先级高低
     * @param other 另一个优先级
     * @return 如果当前优先级更低返回true
     */
    public boolean isLowerThan(MessagePriority other) {
        return this.level > other.level;
    }
    
    /**
     * 获取推荐的处理线程池大小
     */
    public int getRecommendedThreadPoolSize() {
        switch (this) {
            case HIGH:
                return 10;
            case NORMAL:
                return 5;
            case LOW:
                return 2;
            default:
                return 3;
        }
    }
    
    /**
     * 获取推荐的批处理大小
     */
    public int getRecommendedBatchSize() {
        switch (this) {
            case HIGH:
                return 1; // 高优先级消息单独处理
            case NORMAL:
                return 10;
            case LOW:
                return 50;
            default:
                return 10;
        }
    }
    
    /**
     * 获取推荐的重试间隔（秒）
     */
    public int getRecommendedRetryIntervalSeconds() {
        switch (this) {
            case HIGH:
                return 30;
            case NORMAL:
                return 60;
            case LOW:
                return 300;
            default:
                return 60;
        }
    }
    
    /**
     * 获取推荐的消息保留时间（小时）
     */
    public int getRecommendedRetentionHours() {
        switch (this) {
            case HIGH:
                return 72; // 3天
            case NORMAL:
                return 48; // 2天
            case LOW:
                return 24; // 1天
            default:
                return 48;
        }
    }
    
    /**
     * 是否需要立即处理
     */
    public boolean requiresImmediateProcessing() {
        return this == HIGH;
    }
    
    /**
     * 是否可以批量处理
     */
    public boolean allowsBatchProcessing() {
        return this != HIGH;
    }
    
    /**
     * 是否需要详细日志
     */
    public boolean requiresDetailedLogging() {
        return this == HIGH;
    }
    
    /**
     * 是否需要监控告警
     */
    public boolean requiresMonitoring() {
        return this == HIGH;
    }
    
    /**
     * 根据名称查找优先级
     */
    public static MessagePriority findByName(String name) {
        if (name == null || name.trim().isEmpty()) {
            return NORMAL; // 默认返回普通优先级
        }
        
        try {
            return MessagePriority.valueOf(name.toUpperCase());
        } catch (IllegalArgumentException e) {
            return NORMAL;
        }
    }
    
    /**
     * 根据显示名称查找优先级
     */
    public static MessagePriority findByDisplayName(String displayName) {
        if (displayName == null || displayName.trim().isEmpty()) {
            return NORMAL;
        }
        
        for (MessagePriority priority : MessagePriority.values()) {
            if (priority.getDisplayName().equals(displayName)) {
                return priority;
            }
        }
        return NORMAL;
    }
    
    /**
     * 根据级别查找优先级
     */
    public static MessagePriority findByLevel(int level) {
        for (MessagePriority priority : MessagePriority.values()) {
            if (priority.getLevel() == level) {
                return priority;
            }
        }
        return NORMAL;
    }
    
    /**
     * 获取最高优先级
     */
    public static MessagePriority getHighest() {
        return HIGH;
    }
    
    /**
     * 获取最低优先级
     */
    public static MessagePriority getLowest() {
        return LOW;
    }
    
    /**
     * 获取默认优先级
     */
    public static MessagePriority getDefault() {
        return NORMAL;
    }
    
    /**
     * 获取所有优先级，按级别排序
     */
    public static MessagePriority[] getAllSortedByLevel() {
        return new MessagePriority[]{HIGH, NORMAL, LOW};
    }
    
    /**
     * 根据消息内容智能推断优先级
     */
    public static MessagePriority inferFromContent(String content) {
        if (content == null || content.trim().isEmpty()) {
            return NORMAL;
        }
        
        String lowerContent = content.toLowerCase();
        
        // 高优先级关键词
        if (lowerContent.contains("支付") || lowerContent.contains("payment") ||
            lowerContent.contains("订单") || lowerContent.contains("order") ||
            lowerContent.contains("库存") || lowerContent.contains("inventory") ||
            lowerContent.contains("紧急") || lowerContent.contains("urgent") ||
            lowerContent.contains("错误") || lowerContent.contains("error") ||
            lowerContent.contains("失败") || lowerContent.contains("failed")) {
            return HIGH;
        }
        
        // 低优先级关键词
        if (lowerContent.contains("日志") || lowerContent.contains("log") ||
            lowerContent.contains("统计") || lowerContent.contains("statistics") ||
            lowerContent.contains("测试") || lowerContent.contains("test") ||
            lowerContent.contains("调试") || lowerContent.contains("debug")) {
            return LOW;
        }
        
        return NORMAL;
    }
}