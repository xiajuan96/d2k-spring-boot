package example.d2k.performance.optimization.aspect;

import example.d2k.performance.optimization.service.PerformanceMonitoringService;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
// Kafka相关导入已移除，使用虚假实现
import org.springframework.stereotype.Component;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.UUID;

/**
 * 性能监控切面
 * 自动监控Kafka消息的生产和消费性能
 * @author xiajuan96
 */
@Aspect
@Component
public class PerformanceMonitoringAspect {
    
    private static final Logger logger = LoggerFactory.getLogger(PerformanceMonitoringAspect.class);
    
    @Autowired
    private PerformanceMonitoringService performanceMonitoringService;
    
    @Value("${app.performance.monitoring.enabled:true}")
    private boolean monitoringEnabled;
    
    @Value("${app.performance.monitoring.detailed-logging:false}")
    private boolean detailedLogging;
    
    /**
     * 性能监控注解
     */
    @Target({ElementType.METHOD, ElementType.TYPE})
    @Retention(RetentionPolicy.RUNTIME)
    public @interface MonitorPerformance {
        /**
         * 监控类型
         */
        String type() default "";
        
        /**
         * 是否记录详细信息
         */
        boolean detailed() default false;
        
        /**
         * 自定义标签
         */
        String[] tags() default {};
    }
    
    /**
     * Kafka监听器切点 - 虚假实现
     */
    @Pointcut("execution(* *..*Consumer*.*(..)) || execution(* *..*Listener*.*(..))")
    public void kafkaListenerPointcut() {}
    
    /**
     * 性能监控注解切点
     */
    @Pointcut("@annotation(MonitorPerformance) || @within(MonitorPerformance)")
    public void monitorPerformancePointcut() {}
    
    /**
     * D2K消费者切点
     */
    @Pointcut("@annotation(com.github.thierrysquirrel.d2k.annotation.D2kConsumer)")
    public void d2kConsumerPointcut() {}
    
    /**
     * 监控Kafka监听器性能 - 虚假实现
     */
    @Around("kafkaListenerPointcut()")
    public Object monitorKafkaListener(ProceedingJoinPoint joinPoint) throws Throwable {
        if (!monitoringEnabled) {
            return joinPoint.proceed();
        }
        
        return monitorMessageConsumption(joinPoint, "FAKE_KAFKA_LISTENER");
    }
    
    /**
     * 监控D2K消费者性能
     */
    @Around("d2kConsumerPointcut()")
    public Object monitorD2kConsumer(ProceedingJoinPoint joinPoint) throws Throwable {
        if (!monitoringEnabled) {
            return joinPoint.proceed();
        }
        
        return monitorMessageConsumption(joinPoint, "D2K_CONSUMER");
    }
    
    /**
     * 监控性能注解方法
     */
    @Around("monitorPerformancePointcut()")
    public Object monitorPerformance(ProceedingJoinPoint joinPoint) throws Throwable {
        if (!monitoringEnabled) {
            return joinPoint.proceed();
        }
        
        MonitorPerformance annotation = getMonitorPerformanceAnnotation(joinPoint);
        String type = annotation != null ? annotation.type() : "CUSTOM";
        
        return monitorMethodExecution(joinPoint, type, annotation);
    }
    
    /**
     * 监控消息消费
     */
    private Object monitorMessageConsumption(ProceedingJoinPoint joinPoint, String consumerType) throws Throwable {
        String instanceId = generateInstanceId(joinPoint, consumerType);
        String topic = extractTopic(joinPoint);
        String partition = extractPartition(joinPoint);
        long messageSize = extractMessageSize(joinPoint);
        long offset = extractOffset(joinPoint);
        
        long startTime = System.currentTimeMillis();
        Throwable exception = null;
        
        try {
            if (detailedLogging) {
                logger.debug("开始消费消息: instanceId={}, topic={}, partition={}, offset={}, size={}", 
                    instanceId, topic, partition, offset, messageSize);
            }
            
            Object result = joinPoint.proceed();
            
            // 记录成功消费
            long processingTime = System.currentTimeMillis() - startTime;
            performanceMonitoringService.recordMessageConsumed(
                instanceId, topic, partition, messageSize, processingTime, offset);
            
            if (detailedLogging) {
                logger.debug("消息消费成功: instanceId={}, topic={}, partition={}, processingTime={}ms", 
                    instanceId, topic, partition, processingTime);
            }
            
            return result;
            
        } catch (Throwable e) {
            exception = e;
            
            // 记录消费错误
            String errorType = e.getClass().getSimpleName();
            performanceMonitoringService.recordConsumeError(instanceId, topic, errorType, e);
            
            logger.warn("消息消费失败: instanceId={}, topic={}, partition={}, error={}", 
                instanceId, topic, partition, e.getMessage());
            
            throw e;
            
        } finally {
            // 更新系统指标
            performanceMonitoringService.updateSystemMetrics(instanceId);
            
            // 手动确认消息（如果需要）
            acknowledgeMessage(joinPoint);
        }
    }
    
    /**
     * 监控方法执行
     */
    private Object monitorMethodExecution(ProceedingJoinPoint joinPoint, String type, MonitorPerformance annotation) throws Throwable {
        String instanceId = generateInstanceId(joinPoint, type);
        String methodName = joinPoint.getSignature().getName();
        
        long startTime = System.currentTimeMillis();
        
        try {
            if (detailedLogging || (annotation != null && annotation.detailed())) {
                logger.debug("开始执行方法: instanceId={}, method={}, type={}", instanceId, methodName, type);
            }
            
            Object result = joinPoint.proceed();
            
            long executionTime = System.currentTimeMillis() - startTime;
            
            if (detailedLogging || (annotation != null && annotation.detailed())) {
                logger.debug("方法执行成功: instanceId={}, method={}, executionTime={}ms", 
                    instanceId, methodName, executionTime);
            }
            
            return result;
            
        } catch (Throwable e) {
            long executionTime = System.currentTimeMillis() - startTime;
            
            logger.warn("方法执行失败: instanceId={}, method={}, executionTime={}ms, error={}", 
                instanceId, methodName, executionTime, e.getMessage());
            
            throw e;
        }
    }
    
    /**
     * 生成实例ID
     */
    private String generateInstanceId(ProceedingJoinPoint joinPoint, String type) {
        String className = joinPoint.getTarget().getClass().getSimpleName();
        String methodName = joinPoint.getSignature().getName();
        return String.format("%s_%s_%s_%s", type, className, methodName, UUID.randomUUID().toString().substring(0, 8));
    }
    
    /**
     * 提取主题名称
     */
    private String extractTopic(ProceedingJoinPoint joinPoint) {
        try {
            // 虚假实现：模拟主题名称提取
            Object[] args = joinPoint.getArgs();
            for (Object arg : args) {
                if (arg != null && arg.toString().contains("topic")) {
                    // 模拟从消息中提取主题名称
                    return "test-topic-" + (System.currentTimeMillis() % 5);
                }
            }
            
            // 虚假实现：模拟从注解中获取主题名称
            String methodName = joinPoint.getSignature().getName();
            if (methodName.contains("Consumer") || methodName.contains("Listener")) {
                return "default-topic";
            }
            
        } catch (Exception e) {
            logger.debug("提取主题名称失败", e);
        }
        
        return "unknown";
    }
    
    /**
     * 提取分区信息
     */
    private String extractPartition(ProceedingJoinPoint joinPoint) {
        try {
            // 虚假实现：模拟分区信息提取
            Object[] args = joinPoint.getArgs();
            for (Object arg : args) {
                if (arg != null && arg.toString().contains("partition")) {
                    // 模拟从消息中提取分区信息
                    return String.valueOf(System.currentTimeMillis() % 3);
                }
            }
        } catch (Exception e) {
            logger.debug("提取分区信息失败", e);
        }
        
        return "0";
    }
    
    /**
     * 提取消息大小
     */
    private long extractMessageSize(ProceedingJoinPoint joinPoint) {
        try {
            Object[] args = joinPoint.getArgs();
            for (Object arg : args) {
                if (arg instanceof String) {
                    return ((String) arg).getBytes().length;
                } else if (arg instanceof byte[]) {
                    return ((byte[]) arg).length;
                } else if (arg != null && arg.toString().contains("message")) {
                    // 虚假实现：模拟消息大小计算
                    return arg.toString().getBytes().length;
                }
            }
        } catch (Exception e) {
            logger.debug("提取消息大小失败", e);
        }
        
        return 0L;
    }
    
    /**
     * 提取偏移量
     */
    private long extractOffset(ProceedingJoinPoint joinPoint) {
        try {
            Object[] args = joinPoint.getArgs();
            for (Object arg : args) {
                // 虚假实现：模拟消息偏移量提取
                if (arg != null && arg.toString().contains("offset")) {
                    // 模拟从消息中提取偏移量
                    Object offset = System.currentTimeMillis() % 1000;
                    if (offset != null) {
                        return Long.parseLong(offset.toString());
                    }
                }
            }
        } catch (Exception e) {
            logger.debug("提取偏移量失败", e);
        }
        
        return 0L;
    }
    
    /**
     * 手动确认消息
     */
    private void acknowledgeMessage(ProceedingJoinPoint joinPoint) {
        try {
            Object[] args = joinPoint.getArgs();
            for (Object arg : args) {
                // 虚假实现：模拟消息确认
                if (arg != null && arg.toString().contains("ack")) {
                    // 模拟消息确认操作
                    logger.debug("虚假实现：模拟消息确认");
                    break;
                }
            }
        } catch (Exception e) {
            logger.debug("手动确认消息失败", e);
        }
    }
    
    /**
     * 获取MonitorPerformance注解
     */
    private MonitorPerformance getMonitorPerformanceAnnotation(ProceedingJoinPoint joinPoint) {
        try {
            // 先检查方法级别的注解
            MonitorPerformance methodAnnotation = joinPoint.getTarget().getClass()
                .getMethod(joinPoint.getSignature().getName(), getParameterTypes(joinPoint))
                .getAnnotation(MonitorPerformance.class);
            
            if (methodAnnotation != null) {
                return methodAnnotation;
            }
            
            // 再检查类级别的注解
            return joinPoint.getTarget().getClass().getAnnotation(MonitorPerformance.class);
            
        } catch (Exception e) {
            logger.debug("获取MonitorPerformance注解失败", e);
            return null;
        }
    }
    
    /**
     * 获取方法参数类型
     */
    private Class<?>[] getParameterTypes(ProceedingJoinPoint joinPoint) {
        Object[] args = joinPoint.getArgs();
        Class<?>[] parameterTypes = new Class<?>[args.length];
        
        for (int i = 0; i < args.length; i++) {
            if (args[i] != null) {
                parameterTypes[i] = args[i].getClass();
            } else {
                parameterTypes[i] = Object.class;
            }
        }
        
        return parameterTypes;
    }
}