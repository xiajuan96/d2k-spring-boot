package example.d2k.monitoring.config;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.springframework.beans.factory.annotation.Value;
// import org.springframework.boot.actuator.health.HealthIndicator; // 未使用的导入
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
// Redis相关导入已移除，使用虚假实现
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.util.Properties;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;

/**
 * 监控配置类
 * 配置监控相关的Bean和参数
 * 
 * @author xiajuan96
 */
@Configuration
@EnableAspectJAutoProxy
@EnableAsync
@EnableScheduling
public class MonitoringConfig {

    @Value("${app.monitoring.cache.max-size:10000}")
    private long cacheMaxSize;

    @Value("${app.monitoring.cache.expire-after-write:300}")
    private long cacheExpireAfterWrite;

    @Value("${app.monitoring.thread-pool.core-size:5}")
    private int threadPoolCoreSize;

    @Value("${app.monitoring.thread-pool.max-size:20}")
    private int threadPoolMaxSize;

    @Value("${app.monitoring.thread-pool.queue-capacity:100}")
    private int threadPoolQueueCapacity;

    @Value("${app.monitoring.thread-pool.keep-alive:60}")
    private int threadPoolKeepAlive;

    // 邮件配置
    @Value("${app.monitoring.notification.email.host:smtp.gmail.com}")
    private String emailHost;

    @Value("${app.monitoring.notification.email.port:587}")
    private int emailPort;

    @Value("${app.monitoring.notification.email.username:}")
    private String emailUsername;

    @Value("${app.monitoring.notification.email.password:}")
    private String emailPassword;

    @Value("${app.monitoring.notification.email.enabled:false}")
    private boolean emailEnabled;

    // Redis模板配置已移除，使用虚假实现

    /**
     * 配置指标注册表
     */
    @Bean
    @ConditionalOnMissingBean
    public MeterRegistry meterRegistry() {
        return new SimpleMeterRegistry();
    }

    /**
     * 配置监控缓存
     */
    @Bean("monitoringCache")
    public Cache<String, Object> monitoringCache() {
        return Caffeine.newBuilder()
                .maximumSize(cacheMaxSize)
                .expireAfterWrite(cacheExpireAfterWrite, TimeUnit.SECONDS)
                .recordStats()
                .build();
    }

    /**
     * 配置指标缓存
     */
    @Bean("metricsCache")
    public Cache<String, Object> metricsCache() {
        return Caffeine.newBuilder()
                .maximumSize(cacheMaxSize / 2)
                .expireAfterWrite(Duration.ofMinutes(5))
                .recordStats()
                .build();
    }

    /**
     * 配置告警缓存
     */
    @Bean("alertCache")
    public Cache<String, Object> alertCache() {
        return Caffeine.newBuilder()
                .maximumSize(1000)
                .expireAfterWrite(Duration.ofHours(1))
                .recordStats()
                .build();
    }

    /**
     * 配置健康检查缓存
     */
    @Bean("healthCheckCache")
    public Cache<String, Object> healthCheckCache() {
        return Caffeine.newBuilder()
                .maximumSize(100)
                .expireAfterWrite(Duration.ofSeconds(30))
                .recordStats()
                .build();
    }

    /**
     * 配置异步任务执行器
     */
    @Bean("monitoringTaskExecutor")
    public Executor monitoringTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(threadPoolCoreSize);
        executor.setMaxPoolSize(threadPoolMaxSize);
        executor.setQueueCapacity(threadPoolQueueCapacity);
        executor.setKeepAliveSeconds(threadPoolKeepAlive);
        executor.setThreadNamePrefix("monitoring-");
        executor.setRejectedExecutionHandler(new java.util.concurrent.ThreadPoolExecutor.CallerRunsPolicy());
        executor.initialize();
        return executor;
    }

    /**
     * 配置告警任务执行器
     */
    @Bean("alertTaskExecutor")
    public Executor alertTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(5);
        executor.setQueueCapacity(50);
        executor.setKeepAliveSeconds(60);
        executor.setThreadNamePrefix("alert-");
        executor.setRejectedExecutionHandler(new java.util.concurrent.ThreadPoolExecutor.CallerRunsPolicy());
        executor.initialize();
        return executor;
    }

    /**
     * 配置RestTemplate
     */
    @Bean
    @ConditionalOnMissingBean
    public RestTemplate restTemplate() {
        RestTemplate restTemplate = new RestTemplate();
        
        // 设置连接超时和读取超时
        restTemplate.getRequestFactory();
        
        return restTemplate;
    }

    /**
     * 配置邮件发送器
     */
    @Bean
    @ConditionalOnMissingBean
    public JavaMailSender javaMailSender() {
        if (!emailEnabled || emailUsername.isEmpty()) {
            // 返回一个空的实现，避免启动失败
            return new JavaMailSenderImpl();
        }
        
        JavaMailSenderImpl mailSender = new JavaMailSenderImpl();
        mailSender.setHost(emailHost);
        mailSender.setPort(emailPort);
        mailSender.setUsername(emailUsername);
        mailSender.setPassword(emailPassword);
        
        Properties props = mailSender.getJavaMailProperties();
        props.put("mail.transport.protocol", "smtp");
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.debug", "false");
        
        return mailSender;
    }

    /**
     * 监控配置属性
     */
    @Bean
    public MonitoringProperties monitoringProperties() {
        return new MonitoringProperties();
    }

    /**
     * 监控配置属性类
     */
    public static class MonitoringProperties {
        
        // 监控配置
        private Metrics metrics = new Metrics();
        private Alert alert = new Alert();
        private Cache cache = new Cache();
        private ThreadPool threadPool = new ThreadPool();
        private Notification notification = new Notification();
        
        // Getters and Setters
        public Metrics getMetrics() {
            return metrics;
        }
        
        public void setMetrics(Metrics metrics) {
            this.metrics = metrics;
        }
        
        public Alert getAlert() {
            return alert;
        }
        
        public void setAlert(Alert alert) {
            this.alert = alert;
        }
        
        public Cache getCache() {
            return cache;
        }
        
        public void setCache(Cache cache) {
            this.cache = cache;
        }
        
        public ThreadPool getThreadPool() {
            return threadPool;
        }
        
        public void setThreadPool(ThreadPool threadPool) {
            this.threadPool = threadPool;
        }
        
        public Notification getNotification() {
            return notification;
        }
        
        public void setNotification(Notification notification) {
            this.notification = notification;
        }
        
        // 内部配置类
        public static class Metrics {
            private long collectionInterval = 60; // 秒
            private int retentionHours = 24;
            private boolean enabled = true;
            
            // Getters and Setters
            public long getCollectionInterval() {
                return collectionInterval;
            }
            
            public void setCollectionInterval(long collectionInterval) {
                this.collectionInterval = collectionInterval;
            }
            
            public int getRetentionHours() {
                return retentionHours;
            }
            
            public void setRetentionHours(int retentionHours) {
                this.retentionHours = retentionHours;
            }
            
            public boolean isEnabled() {
                return enabled;
            }
            
            public void setEnabled(boolean enabled) {
                this.enabled = enabled;
            }
        }
        
        public static class Alert {
            private boolean enabled = true;
            private Thresholds thresholds = new Thresholds();
            
            public boolean isEnabled() {
                return enabled;
            }
            
            public void setEnabled(boolean enabled) {
                this.enabled = enabled;
            }
            
            public Thresholds getThresholds() {
                return thresholds;
            }
            
            public void setThresholds(Thresholds thresholds) {
                this.thresholds = thresholds;
            }
            
            public static class Thresholds {
                private double cpuUsage = 0.8;
                private double memoryUsage = 0.8;
                private double errorRate = 0.05;
                private long responseTime = 5000;
                private int threadCount = 200;
                
                // Getters and Setters
                public double getCpuUsage() {
                    return cpuUsage;
                }
                
                public void setCpuUsage(double cpuUsage) {
                    this.cpuUsage = cpuUsage;
                }
                
                public double getMemoryUsage() {
                    return memoryUsage;
                }
                
                public void setMemoryUsage(double memoryUsage) {
                    this.memoryUsage = memoryUsage;
                }
                
                public double getErrorRate() {
                    return errorRate;
                }
                
                public void setErrorRate(double errorRate) {
                    this.errorRate = errorRate;
                }
                
                public long getResponseTime() {
                    return responseTime;
                }
                
                public void setResponseTime(long responseTime) {
                    this.responseTime = responseTime;
                }
                
                public int getThreadCount() {
                    return threadCount;
                }
                
                public void setThreadCount(int threadCount) {
                    this.threadCount = threadCount;
                }
            }
        }
        
        public static class Cache {
            private long maxSize = 10000;
            private long expireAfterWrite = 300; // 秒
            private boolean recordStats = true;
            
            // Getters and Setters
            public long getMaxSize() {
                return maxSize;
            }
            
            public void setMaxSize(long maxSize) {
                this.maxSize = maxSize;
            }
            
            public long getExpireAfterWrite() {
                return expireAfterWrite;
            }
            
            public void setExpireAfterWrite(long expireAfterWrite) {
                this.expireAfterWrite = expireAfterWrite;
            }
            
            public boolean isRecordStats() {
                return recordStats;
            }
            
            public void setRecordStats(boolean recordStats) {
                this.recordStats = recordStats;
            }
        }
        
        public static class ThreadPool {
            private int coreSize = 5;
            private int maxSize = 20;
            private int queueCapacity = 100;
            private int keepAlive = 60; // 秒
            
            // Getters and Setters
            public int getCoreSize() {
                return coreSize;
            }
            
            public void setCoreSize(int coreSize) {
                this.coreSize = coreSize;
            }
            
            public int getMaxSize() {
                return maxSize;
            }
            
            public void setMaxSize(int maxSize) {
                this.maxSize = maxSize;
            }
            
            public int getQueueCapacity() {
                return queueCapacity;
            }
            
            public void setQueueCapacity(int queueCapacity) {
                this.queueCapacity = queueCapacity;
            }
            
            public int getKeepAlive() {
                return keepAlive;
            }
            
            public void setKeepAlive(int keepAlive) {
                this.keepAlive = keepAlive;
            }
        }
        
        public static class Notification {
            private Email email = new Email();
            private Webhook webhook = new Webhook();
            
            public Email getEmail() {
                return email;
            }
            
            public void setEmail(Email email) {
                this.email = email;
            }
            
            public Webhook getWebhook() {
                return webhook;
            }
            
            public void setWebhook(Webhook webhook) {
                this.webhook = webhook;
            }
            
            public static class Email {
                private boolean enabled = false;
                private String host = "smtp.gmail.com";
                private int port = 587;
                private String username = "";
                private String password = "";
                private String from = "";
                private String[] to = {};
                
                // Getters and Setters
                public boolean isEnabled() {
                    return enabled;
                }
                
                public void setEnabled(boolean enabled) {
                    this.enabled = enabled;
                }
                
                public String getHost() {
                    return host;
                }
                
                public void setHost(String host) {
                    this.host = host;
                }
                
                public int getPort() {
                    return port;
                }
                
                public void setPort(int port) {
                    this.port = port;
                }
                
                public String getUsername() {
                    return username;
                }
                
                public void setUsername(String username) {
                    this.username = username;
                }
                
                public String getPassword() {
                    return password;
                }
                
                public void setPassword(String password) {
                    this.password = password;
                }
                
                public String getFrom() {
                    return from;
                }
                
                public void setFrom(String from) {
                    this.from = from;
                }
                
                public String[] getTo() {
                    return to;
                }
                
                public void setTo(String[] to) {
                    this.to = to;
                }
            }
            
            public static class Webhook {
                private boolean enabled = false;
                private String url = "";
                private int timeout = 5000; // 毫秒
                private int retries = 3;
                
                // Getters and Setters
                public boolean isEnabled() {
                    return enabled;
                }
                
                public void setEnabled(boolean enabled) {
                    this.enabled = enabled;
                }
                
                public String getUrl() {
                    return url;
                }
                
                public void setUrl(String url) {
                    this.url = url;
                }
                
                public int getTimeout() {
                    return timeout;
                }
                
                public void setTimeout(int timeout) {
                    this.timeout = timeout;
                }
                
                public int getRetries() {
                    return retries;
                }
                
                public void setRetries(int retries) {
                    this.retries = retries;
                }
            }
        }
    }
}