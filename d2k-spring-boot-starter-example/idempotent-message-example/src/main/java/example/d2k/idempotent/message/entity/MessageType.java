package example.d2k.idempotent.message.entity;

/**
 * 消息类型枚举
 * 定义系统中支持的各种消息类型
 * @author xiajuan96
 */
public enum MessageType {
    
    // 订单相关消息
    ORDER_CREATED("订单创建", "订单创建消息", true, MessagePriority.HIGH),
    ORDER_PAID("订单支付", "订单支付成功消息", true, MessagePriority.HIGH),
    ORDER_CANCELLED("订单取消", "订单取消消息", true, MessagePriority.NORMAL),
    ORDER_SHIPPED("订单发货", "订单发货消息", true, MessagePriority.NORMAL),
    ORDER_DELIVERED("订单送达", "订单送达消息", false, MessagePriority.NORMAL),
    ORDER_REFUNDED("订单退款", "订单退款消息", true, MessagePriority.HIGH),
    
    // 支付相关消息
    PAYMENT_CREATED("支付创建", "支付订单创建消息", true, MessagePriority.HIGH),
    PAYMENT_SUCCESS("支付成功", "支付成功通知消息", true, MessagePriority.HIGH),
    PAYMENT_FAILED("支付失败", "支付失败通知消息", true, MessagePriority.HIGH),
    PAYMENT_REFUND("支付退款", "支付退款消息", true, MessagePriority.HIGH),
    
    // 用户相关消息
    USER_REGISTERED("用户注册", "用户注册成功消息", false, MessagePriority.NORMAL),
    USER_LOGIN("用户登录", "用户登录消息", false, MessagePriority.LOW),
    USER_LOGOUT("用户登出", "用户登出消息", false, MessagePriority.LOW),
    USER_PROFILE_UPDATED("用户信息更新", "用户信息更新消息", false, MessagePriority.NORMAL),
    USER_PASSWORD_CHANGED("密码修改", "用户密码修改消息", true, MessagePriority.HIGH),
    
    // 库存相关消息
    INVENTORY_RESERVED("库存预留", "库存预留消息", true, MessagePriority.HIGH),
    INVENTORY_RELEASED("库存释放", "库存释放消息", true, MessagePriority.HIGH),
    INVENTORY_UPDATED("库存更新", "库存数量更新消息", true, MessagePriority.NORMAL),
    INVENTORY_WARNING("库存告警", "库存不足告警消息", false, MessagePriority.HIGH),
    
    // 通知相关消息
    EMAIL_NOTIFICATION("邮件通知", "邮件通知消息", false, MessagePriority.NORMAL),
    SMS_NOTIFICATION("短信通知", "短信通知消息", false, MessagePriority.NORMAL),
    PUSH_NOTIFICATION("推送通知", "APP推送通知消息", false, MessagePriority.NORMAL),
    
    // 系统相关消息
    SYSTEM_ALERT("系统告警", "系统告警消息", false, MessagePriority.HIGH),
    SYSTEM_MAINTENANCE("系统维护", "系统维护通知消息", false, MessagePriority.NORMAL),
    DATA_SYNC("数据同步", "数据同步消息", true, MessagePriority.NORMAL),
    
    // 业务事件消息
    BUSINESS_EVENT("业务事件", "通用业务事件消息", false, MessagePriority.NORMAL),
    AUDIT_LOG("审计日志", "审计日志消息", false, MessagePriority.LOW),
    
    // 测试消息
    TEST_MESSAGE("测试消息", "用于测试的消息类型", false, MessagePriority.LOW);
    
    private final String displayName;
    private final String description;
    private final boolean requiresIdempotency;
    private final MessagePriority defaultPriority;
    
    MessageType(String displayName, String description, boolean requiresIdempotency, MessagePriority defaultPriority) {
        this.displayName = displayName;
        this.description = description;
        this.requiresIdempotency = requiresIdempotency;
        this.defaultPriority = defaultPriority;
    }
    
    public String getDisplayName() {
        return displayName;
    }
    
    public String getDescription() {
        return description;
    }
    
    public boolean isRequiresIdempotency() {
        return requiresIdempotency;
    }
    
    public MessagePriority getDefaultPriority() {
        return defaultPriority;
    }
    
    /**
     * 判断是否为订单相关消息
     */
    public boolean isOrderRelated() {
        return this.name().startsWith("ORDER_");
    }
    
    /**
     * 判断是否为支付相关消息
     */
    public boolean isPaymentRelated() {
        return this.name().startsWith("PAYMENT_");
    }
    
    /**
     * 判断是否为用户相关消息
     */
    public boolean isUserRelated() {
        return this.name().startsWith("USER_");
    }
    
    /**
     * 判断是否为库存相关消息
     */
    public boolean isInventoryRelated() {
        return this.name().startsWith("INVENTORY_");
    }
    
    /**
     * 判断是否为通知相关消息
     */
    public boolean isNotificationRelated() {
        return this.name().contains("NOTIFICATION");
    }
    
    /**
     * 判断是否为系统相关消息
     */
    public boolean isSystemRelated() {
        return this.name().startsWith("SYSTEM_");
    }
    
    /**
     * 判断是否为高优先级消息
     */
    public boolean isHighPriority() {
        return this.defaultPriority == MessagePriority.HIGH;
    }
    
    /**
     * 判断是否为低优先级消息
     */
    public boolean isLowPriority() {
        return this.defaultPriority == MessagePriority.LOW;
    }
    
    /**
     * 判断是否为业务关键消息
     */
    public boolean isBusinessCritical() {
        return isOrderRelated() || isPaymentRelated() || isInventoryRelated();
    }
    
    /**
     * 获取推荐的重试次数
     */
    public int getRecommendedRetryCount() {
        if (isBusinessCritical()) {
            return 5;
        } else if (isNotificationRelated()) {
            return 3;
        } else {
            return 2;
        }
    }
    
    /**
     * 获取推荐的重试间隔（秒）
     */
    public int getRecommendedRetryIntervalSeconds() {
        switch (this.defaultPriority) {
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
     * 获取推荐的处理超时时间（秒）
     */
    public int getRecommendedTimeoutSeconds() {
        if (isBusinessCritical()) {
            return 30;
        } else if (isNotificationRelated()) {
            return 60;
        } else {
            return 120;
        }
    }
    
    /**
     * 根据名称查找消息类型
     */
    public static MessageType findByName(String name) {
        if (name == null || name.trim().isEmpty()) {
            return null;
        }
        
        try {
            return MessageType.valueOf(name.toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
    
    /**
     * 根据显示名称查找消息类型
     */
    public static MessageType findByDisplayName(String displayName) {
        if (displayName == null || displayName.trim().isEmpty()) {
            return null;
        }
        
        for (MessageType type : MessageType.values()) {
            if (type.getDisplayName().equals(displayName)) {
                return type;
            }
        }
        return null;
    }
    
    /**
     * 获取所有订单相关的消息类型
     */
    public static MessageType[] getOrderRelatedTypes() {
        return new MessageType[]{
            ORDER_CREATED, ORDER_PAID, ORDER_CANCELLED, 
            ORDER_SHIPPED, ORDER_DELIVERED, ORDER_REFUNDED
        };
    }
    
    /**
     * 获取所有支付相关的消息类型
     */
    public static MessageType[] getPaymentRelatedTypes() {
        return new MessageType[]{
            PAYMENT_CREATED, PAYMENT_SUCCESS, PAYMENT_FAILED, PAYMENT_REFUND
        };
    }
    
    /**
     * 获取所有需要幂等性处理的消息类型
     */
    public static MessageType[] getIdempotentTypes() {
        return java.util.Arrays.stream(MessageType.values())
                .filter(MessageType::isRequiresIdempotency)
                .toArray(MessageType[]::new);
    }
    
    /**
     * 获取所有高优先级的消息类型
     */
    public static MessageType[] getHighPriorityTypes() {
        return java.util.Arrays.stream(MessageType.values())
                .filter(MessageType::isHighPriority)
                .toArray(MessageType[]::new);
    }
    
    /**
     * 获取所有业务关键的消息类型
     */
    public static MessageType[] getBusinessCriticalTypes() {
        return java.util.Arrays.stream(MessageType.values())
                .filter(MessageType::isBusinessCritical)
                .toArray(MessageType[]::new);
    }
}