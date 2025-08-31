package example.d2k.monitoring.aspect;

import example.d2k.monitoring.service.MonitoringService;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
// Kafka相关导入已移除，使用虚假实现
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Method;

/**
 * 监控切面
 * 自动监控方法执行时间、HTTP请求、Kafka消息处理等
 * 
 * @author xiajuan96
 */
@Aspect
@Component
public class MonitoringAspect {

    private static final Logger logger = LoggerFactory.getLogger(MonitoringAspect.class);

    @Autowired
    private MonitoringService monitoringService;

    /**
     * 监控注解
     * 用于标记需要监控的方法
     */
    @Target(ElementType.METHOD)
    @Retention(RetentionPolicy.RUNTIME)
    public @interface Monitor {
        /**
         * 监控名称
         */
        String value() default "";
        
        /**
         * 是否记录参数
         */
        boolean logArgs() default false;
        
        /**
         * 是否记录返回值
         */
        boolean logResult() default false;
        
        /**
         * 慢方法阈值（毫秒）
         */
        long slowThreshold() default 1000;
    }

    /**
     * 监控HTTP请求
     */
    @Pointcut("@within(org.springframework.web.bind.annotation.RestController) || " +
              "@within(org.springframework.stereotype.Controller)")
    public void controllerMethods() {}

    // Kafka监听器切点已移除

    /**
     * 监控D2K消费者
     */
    @Pointcut("@annotation(com.d2k.spring.boot.autoconfigure.annotation.D2kListener)")
    public void d2kConsumerMethods() {}

    /**
     * 监控标记了@Monitor注解的方法
     */
    @Pointcut("@annotation(example.d2k.monitoring.aspect.MonitoringAspect.Monitor)")
    public void monitorAnnotatedMethods() {}

    /**
     * 监控HTTP请求
     */
    @Around("controllerMethods()")
    public Object monitorHttpRequest(ProceedingJoinPoint joinPoint) throws Throwable {
        long startTime = System.currentTimeMillis();
        String methodName = joinPoint.getSignature().getName();
        String className = joinPoint.getTarget().getClass().getSimpleName();
        
        // 获取HTTP请求信息
        HttpServletRequest request = null;
        HttpServletResponse response = null;
        
        try {
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attributes != null) {
                request = attributes.getRequest();
                response = attributes.getResponse();
            }
        } catch (Exception e) {
            logger.debug("无法获取HTTP请求信息", e);
        }

        String method = request != null ? request.getMethod() : "UNKNOWN";
        String uri = request != null ? request.getRequestURI() : "/unknown";
        
        logger.debug("开始处理HTTP请求: {} {} - {}.{}", method, uri, className, methodName);

        try {
            Object result = joinPoint.proceed();
            
            long duration = System.currentTimeMillis() - startTime;
            int statusCode = response != null ? response.getStatus() : 200;
            
            // 记录HTTP请求指标
            monitoringService.recordHttpRequest(method, uri, statusCode, duration);
            
            if (duration > 1000) {
                logger.warn("慢HTTP请求: {} {} - {}.{} 耗时: {}ms", method, uri, className, methodName, duration);
            } else {
                logger.debug("HTTP请求完成: {} {} - {}.{} 耗时: {}ms 状态码: {}", 
                           method, uri, className, methodName, duration, statusCode);
            }
            
            return result;
            
        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            int statusCode = 500;
            
            // 记录HTTP错误请求指标
            monitoringService.recordHttpRequest(method, uri, statusCode, duration);
            
            logger.error("HTTP请求异常: {} {} - {}.{} 耗时: {}ms", method, uri, className, methodName, duration, e);
            throw e;
        }
    }

    // Kafka消息消费监控方法已移除

    /**
     * 监控标记了@Monitor注解的方法
     */
    @Around("monitorAnnotatedMethods()")
    public Object monitorAnnotatedMethod(ProceedingJoinPoint joinPoint) throws Throwable {
        long startTime = System.currentTimeMillis();
        String methodName = joinPoint.getSignature().getName();
        String className = joinPoint.getTarget().getClass().getSimpleName();
        
        // 获取Monitor注解
        Monitor monitor = getMonitorAnnotation(joinPoint);
        String monitorName = monitor != null && !monitor.value().isEmpty() ? 
                           monitor.value() : className + "." + methodName;
        long slowThreshold = monitor != null ? monitor.slowThreshold() : 1000;
        boolean logArgs = monitor != null && monitor.logArgs();
        boolean logResult = monitor != null && monitor.logResult();
        
        if (logArgs) {
            logger.debug("开始执行监控方法: {} 参数: {}", monitorName, joinPoint.getArgs());
        } else {
            logger.debug("开始执行监控方法: {}", monitorName);
        }

        try {
            Object result = joinPoint.proceed();
            
            long duration = System.currentTimeMillis() - startTime;
            
            if (duration > slowThreshold) {
                logger.warn("慢方法执行: {} 耗时: {}ms (阈值: {}ms)", monitorName, duration, slowThreshold);
            } else {
                if (logResult) {
                    logger.debug("监控方法执行完成: {} 耗时: {}ms 返回值: {}", monitorName, duration, result);
                } else {
                    logger.debug("监控方法执行完成: {} 耗时: {}ms", monitorName, duration);
                }
            }
            
            return result;
            
        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            logger.error("监控方法执行异常: {} 耗时: {}ms", monitorName, duration, e);
            throw e;
        }
    }

    // Kafka相关的Topic提取方法已移除

    /**
     * 获取Monitor注解
     */
    private Monitor getMonitorAnnotation(ProceedingJoinPoint joinPoint) {
        try {
            Method method = ((org.aspectj.lang.reflect.MethodSignature) joinPoint.getSignature()).getMethod();
            return method.getAnnotation(Monitor.class);
        } catch (Exception e) {
            logger.debug("获取Monitor注解失败", e);
            return null;
        }
    }
}