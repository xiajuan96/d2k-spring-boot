package example.d2k.user.behavior.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import example.d2k.user.behavior.entity.BehaviorEventType;
import example.d2k.user.behavior.entity.ProcessStatus;
import example.d2k.user.behavior.entity.UserBehaviorEvent;
import example.d2k.user.behavior.repository.UserBehaviorEventRepository;
import com.d2k.spring.boot.autoconfigure.template.D2kTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 用户行为事件服务类
 * @author xiajuan96
 */
@Service
@Transactional
public class UserBehaviorEventService {
    
    private static final Logger logger = LoggerFactory.getLogger(UserBehaviorEventService.class);
    
    @Autowired
    private UserBehaviorEventRepository eventRepository;
    
    @Autowired
    private D2kTemplate d2kTemplate;
    
    @Autowired
    private RedisTemplate<String, Object> redisTemplate;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    @Value("${user-behavior.processing.batch-size:100}")
    private int batchSize;
    
    @Value("${user-behavior.processing.max-retry-count:3}")
    private int maxRetryCount;
    
    @Value("${user-behavior.processing.retry-delay-minutes:5}")
    private int retryDelayMinutes;
    
    @Value("${user-behavior.processing.delay-processing-minutes:10}")
    private int delayProcessingMinutes;
    
    @Value("${user-behavior.cache.user-session-ttl:1800}")
    private int userSessionTtl;
    
    @Value("${user-behavior.cleanup.processed-events-retention-days:30}")
    private int processedEventsRetentionDays;
    
    @Value("${user-behavior.cleanup.failed-events-retention-days:7}")
    private int failedEventsRetentionDays;
    
    private static final String REDIS_KEY_PREFIX = "user_behavior:";
    private static final String USER_SESSION_KEY = REDIS_KEY_PREFIX + "session:";
    private static final String EVENT_CACHE_KEY = REDIS_KEY_PREFIX + "event:";
    private static final String USER_STATS_KEY = REDIS_KEY_PREFIX + "stats:";
    
    /**
     * 创建用户行为事件
     */
    public UserBehaviorEvent createEvent(String userId, BehaviorEventType eventType, String eventName, 
                                        String pagePath, String ipAddress, String deviceType, 
                                        Map<String, Object> eventProperties) {
        try {
            UserBehaviorEvent event = new UserBehaviorEvent();
            event.setUserId(userId);
            event.setEventType(eventType);
            event.setEventName(eventName);
            event.setPagePath(pagePath);
            event.setIpAddress(ipAddress);
            event.setDeviceType(deviceType);
            event.setPropertiesMap(eventProperties);
            event.setEventTimestamp(LocalDateTime.now());
            event.setProcessStatus(ProcessStatus.PENDING);
            event.setMaxRetryCount(maxRetryCount);
            
            // 根据事件类型决定是否需要延迟处理
            if (eventType.needsDelayedProcessing()) {
                event.setProcessStatus(ProcessStatus.DELAYED);
            }
            
            UserBehaviorEvent savedEvent = eventRepository.save(event);
            logger.info("创建用户行为事件: eventId={}, userId={}, eventType={}, delayed={}", 
                       savedEvent.getId(), userId, eventType, (event.getProcessStatus() == ProcessStatus.DELAYED));
            
            // 缓存事件信息
            cacheEvent(savedEvent);
            
            // 更新用户会话信息
            updateUserSession(userId, eventType);
            
            // 发送处理消息
            if (event.getProcessStatus() != ProcessStatus.DELAYED) {
                sendProcessingMessage(savedEvent);
            } else {
                sendDelayedProcessingMessage(savedEvent);
            }
            
            return savedEvent;
            
        } catch (Exception e) {
            logger.error("创建用户行为事件失败: userId={}, eventType={}", userId, eventType, e);
            throw new RuntimeException("创建用户行为事件失败", e);
        }
    }
    
    /**
     * 批量创建用户行为事件
     */
    public List<UserBehaviorEvent> createEvents(List<UserBehaviorEvent> events) {
        try {
            LocalDateTime now = LocalDateTime.now();
            
            for (UserBehaviorEvent event : events) {
                if (event.getEventTimestamp() == null) {
                    event.setEventTimestamp(now);
                }
                if (event.getProcessStatus() == null) {
                    event.setProcessStatus(ProcessStatus.PENDING);
                }
                if (event.getMaxRetryCount() == 0) {
                    event.setMaxRetryCount(maxRetryCount);
                }
                
                // 根据事件类型决定是否需要延迟处理
                if (event.getEventType().needsDelayedProcessing()) {
                    event.setProcessStatus(ProcessStatus.DELAYED);
                }
            }
            
            List<UserBehaviorEvent> savedEvents = eventRepository.saveAll(events);
            logger.info("批量创建用户行为事件: count={}", savedEvents.size());
            
            // 批量缓存和发送消息
            for (UserBehaviorEvent event : savedEvents) {
                cacheEvent(event);
                updateUserSession(event.getUserId(), event.getEventType());
                
                if (event.getProcessStatus() != ProcessStatus.DELAYED) {
                    sendProcessingMessage(event);
                } else {
                    sendDelayedProcessingMessage(event);
                }
            }
            
            return savedEvents;
            
        } catch (Exception e) {
            logger.error("批量创建用户行为事件失败: count={}", events.size(), e);
            throw new RuntimeException("批量创建用户行为事件失败", e);
        }
    }
    
    /**
     * 处理用户行为事件
     */
    public void processEvent(String eventId) {
        try {
            Optional<UserBehaviorEvent> eventOpt = eventRepository.findById(eventId);
            if (!eventOpt.isPresent()) {
                logger.warn("事件不存在: eventId={}", eventId);
                return;
            }
            
            UserBehaviorEvent event = eventOpt.get();
            
            // 检查事件状态
            if (!event.getProcessStatus().canRetry()) {
                logger.warn("事件状态不允许处理: eventId={}, status={}", eventId, event.getProcessStatus());
                return;
            }
            
            // 更新状态为处理中
            event.setProcessStatus(ProcessStatus.PROCESSING);
            event.setActualProcessTime(LocalDateTime.now());
            eventRepository.save(event);
            
            logger.info("开始处理用户行为事件: eventId={}, userId={}, eventType={}", 
                       eventId, event.getUserId(), event.getEventType());
            
            // 执行具体的业务处理逻辑
            boolean success = executeBusinessLogic(event);
            
            if (success) {
                // 处理成功
                event.setProcessStatus(ProcessStatus.PROCESSED);
                event.setActualProcessTime(LocalDateTime.now());
                eventRepository.save(event);
                
                logger.info("用户行为事件处理成功: eventId={}", eventId);
                
                // 更新缓存和统计信息
                updateEventCache(event);
                updateUserStats(event.getUserId(), event.getEventType(), true);
                
            } else {
                // 处理失败，安排重试
                handleProcessingFailure(event, "业务逻辑处理失败");
            }
            
        } catch (Exception e) {
            logger.error("处理用户行为事件异常: eventId={}", eventId, e);
            
            // 处理异常，安排重试
            try {
                Optional<UserBehaviorEvent> eventOpt = eventRepository.findById(eventId);
                if (eventOpt.isPresent()) {
                    handleProcessingFailure(eventOpt.get(), "处理异常: " + e.getMessage());
                }
            } catch (Exception retryException) {
                logger.error("处理失败重试安排异常: eventId={}", eventId, retryException);
            }
        }
    }
    
    /**
     * 执行业务逻辑处理
     */
    private boolean executeBusinessLogic(UserBehaviorEvent event) {
        try {
            BehaviorEventType eventType = event.getEventType();
            
            switch (eventType) {
                case PAGE_VIEW:
                    return processPageView(event);
                case CLICK:
                    return processClick(event);
                case FORM_SUBMIT:
                    return processFormSubmit(event);
                case SEARCH:
                    return processSearch(event);
                case PRODUCT_VIEW:
                    return processProductView(event);
                case ADD_TO_CART:
                    return processAddToCart(event);
                case PURCHASE:
                    return processPurchase(event);
                case REGISTER:
                    return processUserRegister(event);
                case LOGIN:
                    return processUserLogin(event);
                case LOGOUT:
                    return processUserLogout(event);
                case CONTENT_LIKE:
                    return processContentLike(event);
                case CONTENT_SHARE:
                    return processContentShare(event);
                case COMMENT_POST:
                    return processContentComment(event);
                case FILE_DOWNLOAD:
                    return processFileDownload(event);
                case FILE_UPLOAD:
                    return processFileUpload(event);
                case VIDEO_PLAY:
                    return processVideoPlay(event);
                case VIDEO_PAUSE:
                    return processVideoPause(event);
                case VIDEO_COMPLETE:
                    return processVideoComplete(event);
                case ERROR_OCCURRED:
                    return processError(event);
                case CUSTOM:
                    return processCustomEvent(event);
                default:
                    logger.warn("未知的事件类型: eventType={}", eventType);
                    return false;
            }
            
        } catch (Exception e) {
            logger.error("执行业务逻辑失败: eventId={}, eventType={}", event.getId(), event.getEventType(), e);
            return false;
        }
    }
    
    /**
     * 处理页面访问事件
     */
    private boolean processPageView(UserBehaviorEvent event) {
        logger.info("处理页面访问事件: userId={}, pagePath={}", event.getUserId(), event.getPagePath());
        
        // 更新页面访问统计
        String pageStatsKey = USER_STATS_KEY + "page:" + event.getPagePath();
        redisTemplate.opsForHash().increment(pageStatsKey, "view_count", 1);
        redisTemplate.opsForHash().increment(pageStatsKey, "unique_users", 1);
        redisTemplate.expire(pageStatsKey, 24, TimeUnit.HOURS);
        
        // 更新用户访问路径
        String userPathKey = USER_STATS_KEY + "path:" + event.getUserId();
        redisTemplate.opsForList().rightPush(userPathKey, event.getPagePath());
        redisTemplate.expire(userPathKey, userSessionTtl, TimeUnit.SECONDS);
        
        return true;
    }
    
    /**
     * 处理点击事件
     */
    private boolean processClick(UserBehaviorEvent event) {
        logger.info("处理点击事件: userId={}, eventName={}", event.getUserId(), event.getEventName());
        
        // 更新点击统计
        String clickStatsKey = USER_STATS_KEY + "click:" + event.getEventName();
        redisTemplate.opsForHash().increment(clickStatsKey, "click_count", 1);
        redisTemplate.expire(clickStatsKey, 24, TimeUnit.HOURS);
        
        return true;
    }
    
    /**
     * 处理表单提交事件
     */
    private boolean processFormSubmit(UserBehaviorEvent event) {
        logger.info("处理表单提交事件: userId={}, eventName={}", event.getUserId(), event.getEventName());
        
        // 更新表单提交统计
        String formStatsKey = USER_STATS_KEY + "form:" + event.getEventName();
        redisTemplate.opsForHash().increment(formStatsKey, "submit_count", 1);
        redisTemplate.expire(formStatsKey, 24, TimeUnit.HOURS);
        
        return true;
    }
    
    /**
     * 处理搜索事件
     */
    private boolean processSearch(UserBehaviorEvent event) {
        logger.info("处理搜索事件: userId={}, eventName={}", event.getUserId(), event.getEventName());
        
        // 提取搜索关键词
        Map<String, Object> properties = event.getPropertiesMap();
        if (properties != null && properties.containsKey("keyword")) {
            String keyword = properties.get("keyword").toString();
            
            // 更新搜索关键词统计
            String searchStatsKey = USER_STATS_KEY + "search:keywords";
            redisTemplate.opsForZSet().incrementScore(searchStatsKey, keyword, 1);
            redisTemplate.expire(searchStatsKey, 24, TimeUnit.HOURS);
        }
        
        return true;
    }
    
    /**
     * 处理商品查看事件
     */
    private boolean processProductView(UserBehaviorEvent event) {
        logger.info("处理商品查看事件: userId={}, eventName={}", event.getUserId(), event.getEventName());
        
        // 提取商品ID
        Map<String, Object> properties = event.getPropertiesMap();
        if (properties != null && properties.containsKey("productId")) {
            String productId = properties.get("productId").toString();
            
            // 更新商品查看统计
            String productStatsKey = USER_STATS_KEY + "product:" + productId;
            redisTemplate.opsForHash().increment(productStatsKey, "view_count", 1);
            redisTemplate.expire(productStatsKey, 24, TimeUnit.HOURS);
            
            // 更新用户浏览历史
            String userHistoryKey = USER_STATS_KEY + "history:" + event.getUserId();
            redisTemplate.opsForList().leftPush(userHistoryKey, productId);
            redisTemplate.opsForList().trim(userHistoryKey, 0, 99); // 保留最近100个
            redisTemplate.expire(userHistoryKey, 30, TimeUnit.DAYS);
        }
        
        return true;
    }
    
    /**
     * 处理加入购物车事件
     */
    private boolean processAddToCart(UserBehaviorEvent event) {
        logger.info("处理加入购物车事件: userId={}, eventName={}", event.getUserId(), event.getEventName());
        
        // 提取商品信息
        Map<String, Object> properties = event.getPropertiesMap();
        if (properties != null && properties.containsKey("productId")) {
            String productId = properties.get("productId").toString();
            
            // 更新购物车统计
            String cartStatsKey = USER_STATS_KEY + "cart:" + productId;
            redisTemplate.opsForHash().increment(cartStatsKey, "add_count", 1);
            redisTemplate.expire(cartStatsKey, 24, TimeUnit.HOURS);
        }
        
        return true;
    }
    
    /**
     * 处理购买事件
     */
    private boolean processPurchase(UserBehaviorEvent event) {
        logger.info("处理购买事件: userId={}, eventName={}", event.getUserId(), event.getEventName());
        
        // 提取订单信息
        Map<String, Object> properties = event.getPropertiesMap();
        if (properties != null) {
            // 更新购买统计
            String purchaseStatsKey = USER_STATS_KEY + "purchase:daily";
            String today = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
            redisTemplate.opsForHash().increment(purchaseStatsKey, today, 1);
            redisTemplate.expire(purchaseStatsKey, 30, TimeUnit.DAYS);
            
            // 更新用户购买历史
            if (properties.containsKey("orderId")) {
                String orderId = properties.get("orderId").toString();
                String userPurchaseKey = USER_STATS_KEY + "purchase:" + event.getUserId();
                redisTemplate.opsForList().leftPush(userPurchaseKey, orderId);
                redisTemplate.expire(userPurchaseKey, 365, TimeUnit.DAYS);
            }
        }
        
        return true;
    }
    
    /**
     * 处理用户注册事件
     */
    private boolean processUserRegister(UserBehaviorEvent event) {
        logger.info("处理用户注册事件: userId={}", event.getUserId());
        
        // 更新注册统计
        String registerStatsKey = USER_STATS_KEY + "register:daily";
        String today = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        redisTemplate.opsForHash().increment(registerStatsKey, today, 1);
        redisTemplate.expire(registerStatsKey, 30, TimeUnit.DAYS);
        
        return true;
    }
    
    /**
     * 处理用户登录事件
     */
    private boolean processUserLogin(UserBehaviorEvent event) {
        logger.info("处理用户登录事件: userId={}", event.getUserId());
        
        // 更新登录统计
        String loginStatsKey = USER_STATS_KEY + "login:daily";
        String today = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        redisTemplate.opsForHash().increment(loginStatsKey, today, 1);
        redisTemplate.expire(loginStatsKey, 30, TimeUnit.DAYS);
        
        // 更新用户最后登录时间
        String userLoginKey = USER_STATS_KEY + "last_login:" + event.getUserId();
        redisTemplate.opsForValue().set(userLoginKey, LocalDateTime.now().toString());
        redisTemplate.expire(userLoginKey, 365, TimeUnit.DAYS);
        
        return true;
    }
    
    /**
     * 处理用户登出事件
     */
    private boolean processUserLogout(UserBehaviorEvent event) {
        logger.info("处理用户登出事件: userId={}", event.getUserId());
        
        // 清理用户会话缓存
        String userSessionKey = USER_SESSION_KEY + event.getUserId();
        redisTemplate.delete(userSessionKey);
        
        return true;
    }
    
    /**
     * 处理内容点赞事件
     */
    private boolean processContentLike(UserBehaviorEvent event) {
        logger.info("处理内容点赞事件: userId={}, eventName={}", event.getUserId(), event.getEventName());
        
        // 提取内容ID
        Map<String, Object> properties = event.getPropertiesMap();
        if (properties != null && properties.containsKey("contentId")) {
            String contentId = properties.get("contentId").toString();
            
            // 更新内容点赞统计
            String likeStatsKey = USER_STATS_KEY + "like:" + contentId;
            redisTemplate.opsForHash().increment(likeStatsKey, "like_count", 1);
            redisTemplate.expire(likeStatsKey, 30, TimeUnit.DAYS);
        }
        
        return true;
    }
    
    /**
     * 处理内容分享事件
     */
    private boolean processContentShare(UserBehaviorEvent event) {
        logger.info("处理内容分享事件: userId={}, eventName={}", event.getUserId(), event.getEventName());
        
        // 提取内容ID
        Map<String, Object> properties = event.getPropertiesMap();
        if (properties != null && properties.containsKey("contentId")) {
            String contentId = properties.get("contentId").toString();
            
            // 更新内容分享统计
            String shareStatsKey = USER_STATS_KEY + "share:" + contentId;
            redisTemplate.opsForHash().increment(shareStatsKey, "share_count", 1);
            redisTemplate.expire(shareStatsKey, 30, TimeUnit.DAYS);
        }
        
        return true;
    }
    
    /**
     * 处理内容评论事件
     */
    private boolean processContentComment(UserBehaviorEvent event) {
        logger.info("处理内容评论事件: userId={}, eventName={}", event.getUserId(), event.getEventName());
        
        // 提取内容ID
        Map<String, Object> properties = event.getPropertiesMap();
        if (properties != null && properties.containsKey("contentId")) {
            String contentId = properties.get("contentId").toString();
            
            // 更新内容评论统计
            String commentStatsKey = USER_STATS_KEY + "comment:" + contentId;
            redisTemplate.opsForHash().increment(commentStatsKey, "comment_count", 1);
            redisTemplate.expire(commentStatsKey, 30, TimeUnit.DAYS);
        }
        
        return true;
    }
    
    /**
     * 处理文件下载事件
     */
    private boolean processFileDownload(UserBehaviorEvent event) {
        logger.info("处理文件下载事件: userId={}, eventName={}", event.getUserId(), event.getEventName());
        
        // 提取文件信息
        Map<String, Object> properties = event.getPropertiesMap();
        if (properties != null && properties.containsKey("fileId")) {
            String fileId = properties.get("fileId").toString();
            
            // 更新文件下载统计
            String downloadStatsKey = USER_STATS_KEY + "download:" + fileId;
            redisTemplate.opsForHash().increment(downloadStatsKey, "download_count", 1);
            redisTemplate.expire(downloadStatsKey, 30, TimeUnit.DAYS);
        }
        
        return true;
    }
    
    /**
     * 处理文件上传事件
     */
    private boolean processFileUpload(UserBehaviorEvent event) {
        logger.info("处理文件上传事件: userId={}, eventName={}", event.getUserId(), event.getEventName());
        
        // 更新文件上传统计
        String uploadStatsKey = USER_STATS_KEY + "upload:daily";
        String today = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        redisTemplate.opsForHash().increment(uploadStatsKey, today, 1);
        redisTemplate.expire(uploadStatsKey, 30, TimeUnit.DAYS);
        
        return true;
    }
    
    /**
     * 处理视频播放事件
     */
    private boolean processVideoPlay(UserBehaviorEvent event) {
        logger.info("处理视频播放事件: userId={}, eventName={}", event.getUserId(), event.getEventName());
        
        // 提取视频信息
        Map<String, Object> properties = event.getPropertiesMap();
        if (properties != null && properties.containsKey("videoId")) {
            String videoId = properties.get("videoId").toString();
            
            // 更新视频播放统计
            String videoStatsKey = USER_STATS_KEY + "video:" + videoId;
            redisTemplate.opsForHash().increment(videoStatsKey, "play_count", 1);
            redisTemplate.expire(videoStatsKey, 30, TimeUnit.DAYS);
        }
        
        return true;
    }
    
    /**
     * 处理视频暂停事件
     */
    private boolean processVideoPause(UserBehaviorEvent event) {
        logger.info("处理视频暂停事件: userId={}, eventName={}", event.getUserId(), event.getEventName());
        return true;
    }
    
    /**
     * 处理视频完成事件
     */
    private boolean processVideoComplete(UserBehaviorEvent event) {
        logger.info("处理视频完成事件: userId={}, eventName={}", event.getUserId(), event.getEventName());
        
        // 提取视频信息
        Map<String, Object> properties = event.getPropertiesMap();
        if (properties != null && properties.containsKey("videoId")) {
            String videoId = properties.get("videoId").toString();
            
            // 更新视频完成统计
            String videoStatsKey = USER_STATS_KEY + "video:" + videoId;
            redisTemplate.opsForHash().increment(videoStatsKey, "complete_count", 1);
            redisTemplate.expire(videoStatsKey, 30, TimeUnit.DAYS);
        }
        
        return true;
    }
    
    /**
     * 处理错误事件
     */
    private boolean processError(UserBehaviorEvent event) {
        logger.info("处理错误事件: userId={}, eventName={}", event.getUserId(), event.getEventName());
        
        // 提取错误信息
        Map<String, Object> properties = event.getPropertiesMap();
        if (properties != null && properties.containsKey("errorType")) {
            String errorType = properties.get("errorType").toString();
            
            // 更新错误统计
            String errorStatsKey = USER_STATS_KEY + "error:" + errorType;
            redisTemplate.opsForHash().increment(errorStatsKey, "error_count", 1);
            redisTemplate.expire(errorStatsKey, 7, TimeUnit.DAYS);
        }
        
        return true;
    }
    
    /**
     * 处理自定义事件
     */
    private boolean processCustomEvent(UserBehaviorEvent event) {
        logger.info("处理自定义事件: userId={}, eventName={}", event.getUserId(), event.getEventName());
        
        // 自定义事件的处理逻辑可以根据具体需求实现
        // 这里只是简单记录
        
        return true;
    }
    
    /**
     * 处理失败处理
     */
    private void handleProcessingFailure(UserBehaviorEvent event, String errorMessage) {
        try {
            event.incrementRetryCount();
            event.setErrorMessage(errorMessage);
            event.setActualProcessTime(LocalDateTime.now());
            
            if (event.canRetry()) {
                // 可以重试，安排下次重试
                event.setProcessStatus(ProcessStatus.FAILED);
                // 计算下次重试时间（仅用于日志记录）
                LocalDateTime nextRetryTime = LocalDateTime.now().plusMinutes(retryDelayMinutes * event.getRetryCount());
                
                logger.warn("事件处理失败，安排重试: eventId={}, retryCount={}, nextRetryTime={}", 
                           event.getId(), event.getRetryCount(), nextRetryTime);
                
                // 发送重试消息
                sendRetryMessage(event);
                
            } else {
                // 达到最大重试次数，标记为重试失败
                event.setProcessStatus(ProcessStatus.RETRY_FAILED);
                
                logger.error("事件处理重试失败，已达到最大重试次数: eventId={}, retryCount={}", 
                            event.getId(), event.getRetryCount());
            }
            
            eventRepository.save(event);
            updateUserStats(event.getUserId(), event.getEventType(), false);
            
        } catch (Exception e) {
            logger.error("处理失败处理异常: eventId={}", event.getId(), e);
        }
    }
    
    /**
     * 发送处理消息
     */
    private void sendProcessingMessage(UserBehaviorEvent event) {
        try {
            Map<String, Object> message = new HashMap<>();
            message.put("eventId", event.getId());
            message.put("userId", event.getUserId());
            message.put("eventType", event.getEventType().name());
            message.put("action", "process");
            message.put("timestamp", LocalDateTime.now().toString());
            
            d2kTemplate.send("user-behavior-processing", event.getId(), message);
            logger.debug("发送处理消息: eventId={}", event.getId());
            
        } catch (Exception e) {
            logger.error("发送处理消息失败: eventId={}", event.getId(), e);
        }
    }
    
    /**
     * 发送延迟处理消息
     */
    private void sendDelayedProcessingMessage(UserBehaviorEvent event) {
        try {
            Map<String, Object> message = new HashMap<>();
            message.put("eventId", event.getId());
            message.put("userId", event.getUserId());
            message.put("eventType", event.getEventType().name());
            message.put("action", "delayed_process");
            // 计算延迟时间
            LocalDateTime delayUntil = LocalDateTime.now().plusMinutes(delayProcessingMinutes);
            message.put("delayUntil", delayUntil.toString());
            message.put("timestamp", LocalDateTime.now().toString());
            
            // 计算延迟时间（毫秒）
            long delayMs = java.time.Duration.between(LocalDateTime.now(), delayUntil).toMillis();
            
            d2kTemplate.send("user-behavior-delayed-processing", event.getId().toString(), objectMapper.writeValueAsString(message));
            logger.debug("发送延迟处理消息: eventId={}, delayMs={}", event.getId(), delayMs);
            
        } catch (Exception e) {
            logger.error("发送延迟处理消息失败: eventId={}", event.getId(), e);
        }
    }
    
    /**
     * 发送重试消息
     */
    private void sendRetryMessage(UserBehaviorEvent event) {
        try {
            Map<String, Object> message = new HashMap<>();
            message.put("eventId", event.getId());
            message.put("userId", event.getUserId());
            message.put("eventType", event.getEventType().name());
            message.put("action", "retry");
            message.put("retryCount", event.getRetryCount());
            // 计算下次重试时间
            LocalDateTime nextRetryTime = LocalDateTime.now().plusMinutes(retryDelayMinutes * event.getRetryCount());
            message.put("nextRetryTime", nextRetryTime.toString());
            message.put("timestamp", LocalDateTime.now().toString());
            
            // 计算延迟时间（毫秒）
            long delayMs = java.time.Duration.between(LocalDateTime.now(), nextRetryTime).toMillis();
            
            d2kTemplate.send("user-behavior-retry", event.getId().toString(), objectMapper.writeValueAsString(message));
            logger.debug("发送重试消息: eventId={}, retryCount={}, delayMs={}", 
                        event.getId(), event.getRetryCount(), delayMs);
            
        } catch (Exception e) {
            logger.error("发送重试消息失败: eventId={}", event.getId(), e);
        }
    }
    
    /**
     * 缓存事件信息
     */
    private void cacheEvent(UserBehaviorEvent event) {
        try {
            String eventKey = EVENT_CACHE_KEY + event.getId();
            String eventJson = objectMapper.writeValueAsString(event);
            redisTemplate.opsForValue().set(eventKey, eventJson, 24, TimeUnit.HOURS);
            
        } catch (JsonProcessingException e) {
            logger.error("缓存事件信息失败: eventId={}", event.getId(), e);
        }
    }
    
    /**
     * 更新事件缓存
     */
    private void updateEventCache(UserBehaviorEvent event) {
        try {
            String eventKey = EVENT_CACHE_KEY + event.getId();
            String eventJson = objectMapper.writeValueAsString(event);
            redisTemplate.opsForValue().set(eventKey, eventJson, 24, TimeUnit.HOURS);
            
        } catch (JsonProcessingException e) {
            logger.error("更新事件缓存失败: eventId={}", event.getId(), e);
        }
    }
    
    /**
     * 更新用户会话信息
     */
    private void updateUserSession(String userId, BehaviorEventType eventType) {
        try {
            String userSessionKey = USER_SESSION_KEY + userId;
            Map<String, Object> sessionInfo = new HashMap<>();
            sessionInfo.put("lastEventType", eventType.name());
            sessionInfo.put("lastEventTime", LocalDateTime.now().toString());
            sessionInfo.put("eventCount", 1);
            
            redisTemplate.opsForHash().putAll(userSessionKey, sessionInfo);
            redisTemplate.opsForHash().increment(userSessionKey, "eventCount", 1);
            redisTemplate.expire(userSessionKey, userSessionTtl, TimeUnit.SECONDS);
            
        } catch (Exception e) {
            logger.error("更新用户会话信息失败: userId={}", userId, e);
        }
    }
    
    /**
     * 更新用户统计信息
     */
    private void updateUserStats(String userId, BehaviorEventType eventType, boolean success) {
        try {
            String userStatsKey = USER_STATS_KEY + "user:" + userId;
            
            redisTemplate.opsForHash().increment(userStatsKey, "total_events", 1);
            redisTemplate.opsForHash().increment(userStatsKey, eventType.name() + "_count", 1);
            
            if (success) {
                redisTemplate.opsForHash().increment(userStatsKey, "success_events", 1);
            } else {
                redisTemplate.opsForHash().increment(userStatsKey, "failed_events", 1);
            }
            
            redisTemplate.expire(userStatsKey, 30, TimeUnit.DAYS);
            
        } catch (Exception e) {
            logger.error("更新用户统计信息失败: userId={}", userId, e);
        }
    }
    
    /**
     * 更新事件状态
     */
    public boolean updateEventStatus(String eventId, ProcessStatus newStatus) {
        try {
            Optional<UserBehaviorEvent> eventOpt = eventRepository.findById(eventId);
            if (!eventOpt.isPresent()) {
                logger.warn("事件不存在: eventId={}", eventId);
                return false;
            }
            
            UserBehaviorEvent event = eventOpt.get();
            
            // 检查状态转换是否合法
            if (!event.getProcessStatus().canTransitionTo(newStatus)) {
                logger.warn("非法的状态转换: eventId={}, from={}, to={}", 
                           eventId, event.getProcessStatus(), newStatus);
                return false;
            }
            
            event.setProcessStatus(newStatus);
            event.setActualProcessTime(LocalDateTime.now());
            eventRepository.save(event);
            
            logger.info("更新事件状态: eventId={}, status={}", eventId, newStatus);
            updateEventCache(event);
            
            return true;
            
        } catch (Exception e) {
            logger.error("更新事件状态失败: eventId={}, status={}", eventId, newStatus, e);
            return false;
        }
    }
    
    /**
     * 手动重试事件
     */
    public boolean retryEvent(String eventId) {
        try {
            Optional<UserBehaviorEvent> eventOpt = eventRepository.findById(eventId);
            if (!eventOpt.isPresent()) {
                logger.warn("事件不存在: eventId={}", eventId);
                return false;
            }
            
            UserBehaviorEvent event = eventOpt.get();
            
            // 检查是否可以重试
            if (!event.getProcessStatus().canRetry()) {
                logger.warn("事件状态不允许重试: eventId={}, status={}", eventId, event.getProcessStatus());
                return false;
            }
            
            // 重置状态为待处理
            event.setProcessStatus(ProcessStatus.PENDING);
            event.setErrorMessage(null);
            event.setActualProcessTime(LocalDateTime.now());
            eventRepository.save(event);
            
            // 发送处理消息
            sendProcessingMessage(event);
            
            logger.info("手动重试事件: eventId={}", eventId);
            return true;
            
        } catch (Exception e) {
            logger.error("手动重试事件失败: eventId={}", eventId, e);
            return false;
        }
    }
    
    /**
     * 取消事件处理
     */
    public boolean cancelEvent(String eventId) {
        try {
            Optional<UserBehaviorEvent> eventOpt = eventRepository.findById(eventId);
            if (!eventOpt.isPresent()) {
                logger.warn("事件不存在: eventId={}", eventId);
                return false;
            }
            
            UserBehaviorEvent event = eventOpt.get();
            
            // 检查是否可以取消
            if (!event.getProcessStatus().canCancel()) {
                logger.warn("事件状态不允许取消: eventId={}, status={}", eventId, event.getProcessStatus());
                return false;
            }
            
            event.setProcessStatus(ProcessStatus.CANCELLED);
            event.setActualProcessTime(LocalDateTime.now());
            eventRepository.save(event);
            
            logger.info("取消事件处理: eventId={}", eventId);
            updateEventCache(event);
            
            return true;
            
        } catch (Exception e) {
            logger.error("取消事件处理失败: eventId={}", eventId, e);
            return false;
        }
    }
    
    /**
     * 批量处理事件
     */
    public void batchProcessEvents() {
        try {
            // 查找需要处理的事件
            List<ProcessStatus> processableStatuses = Arrays.asList(
                ProcessStatus.PENDING, ProcessStatus.FAILED
            );
            
            Pageable pageable = PageRequest.of(0, batchSize, Sort.by("eventTimestamp").ascending());
            Page<UserBehaviorEvent> eventsPage = eventRepository.findEventsToProcess(processableStatuses, pageable);
            
            if (eventsPage.hasContent()) {
                List<UserBehaviorEvent> events = eventsPage.getContent();
                logger.info("开始批量处理事件: count={}", events.size());
                
                for (UserBehaviorEvent event : events) {
                    try {
                        processEvent(event.getId().toString());
                    } catch (Exception e) {
                        logger.error("批量处理事件失败: eventId={}", event.getId(), e);
                    }
                }
                
                logger.info("批量处理事件完成: count={}", events.size());
            }
            
        } catch (Exception e) {
            logger.error("批量处理事件异常", e);
        }
    }
    
    /**
     * 处理延迟事件
     */
    public void processDelayedEvents() {
        try {
            LocalDateTime now = LocalDateTime.now();
            Pageable pageable = PageRequest.of(0, batchSize, Sort.by("delayUntil").ascending());
            Page<UserBehaviorEvent> eventsPage = eventRepository.findDelayedEventsReadyToProcess(now, pageable);
            
            if (eventsPage.hasContent()) {
                List<UserBehaviorEvent> events = eventsPage.getContent();
                logger.info("开始处理延迟事件: count={}", events.size());
                
                for (UserBehaviorEvent event : events) {
                    try {
                        // 更新状态为待处理
                        event.setProcessStatus(ProcessStatus.PENDING);
                        eventRepository.save(event);
                        
                        // 发送处理消息
                        sendProcessingMessage(event);
                        
                    } catch (Exception e) {
                        logger.error("处理延迟事件失败: eventId={}", event.getId(), e);
                    }
                }
                
                logger.info("处理延迟事件完成: count={}", events.size());
            }
            
        } catch (Exception e) {
            logger.error("处理延迟事件异常", e);
        }
    }
    
    /**
     * 处理重试事件
     */
    public void processRetryEvents() {
        try {
            LocalDateTime now = LocalDateTime.now();
            Pageable pageable = PageRequest.of(0, batchSize, Sort.by("nextRetryTime").ascending());
            Page<UserBehaviorEvent> eventsPage = eventRepository.findEventsToRetry(now, pageable);
            
            if (eventsPage.hasContent()) {
                List<UserBehaviorEvent> events = eventsPage.getContent();
                logger.info("开始处理重试事件: count={}", events.size());
                
                for (UserBehaviorEvent event : events) {
                    try {
                        processEvent(event.getId().toString());
                    } catch (Exception e) {
                        logger.error("处理重试事件失败: eventId={}", event.getId(), e);
                    }
                }
                
                logger.info("处理重试事件完成: count={}", events.size());
            }
            
        } catch (Exception e) {
            logger.error("处理重试事件异常", e);
        }
    }
    
    /**
     * 清理过期事件
     */
    @Transactional
    public void cleanupExpiredEvents() {
        try {
            LocalDateTime processedExpireTime = LocalDateTime.now().minusDays(processedEventsRetentionDays);
            LocalDateTime failedExpireTime = LocalDateTime.now().minusDays(failedEventsRetentionDays);
            
            // 删除过期的已处理事件
            int processedDeleted = eventRepository.deleteExpiredProcessedEvents(processedExpireTime);
            logger.info("删除过期的已处理事件: count={}", processedDeleted);
            
            // 删除过期的失败事件
            int failedDeleted = eventRepository.deleteExpiredFailedEvents(failedExpireTime);
            logger.info("删除过期的失败事件: count={}", failedDeleted);
            
            // 归档旧事件
            LocalDateTime archiveTime = LocalDateTime.now().minusDays(processedEventsRetentionDays / 2);
            int archived = eventRepository.archiveOldEvents(archiveTime);
            logger.info("归档旧事件: count={}", archived);
            
        } catch (Exception e) {
            logger.error("清理过期事件异常", e);
        }
    }
    
    /**
     * 查询事件
     */
    @Transactional(readOnly = true)
    public UserBehaviorEvent findEventById(String eventId) {
        return eventRepository.findById(eventId).orElse(null);
    }
    
    /**
     * 查询用户事件
     */
    @Transactional(readOnly = true)
    public List<UserBehaviorEvent> findEventsByUser(String userId) {
        return eventRepository.findByUserId(userId);
    }
    
    /**
     * 分页查询用户事件
     */
    @Transactional(readOnly = true)
    public Page<UserBehaviorEvent> findEventsByUser(String userId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("eventTimestamp").descending());
        List<UserBehaviorEvent> allEvents = eventRepository.findByUserId(userId);
        // 手动分页
        int start = page * size;
        int end = Math.min(start + size, allEvents.size());
        List<UserBehaviorEvent> pageContent = start < allEvents.size() ? allEvents.subList(start, end) : new ArrayList<>();
        return new org.springframework.data.domain.PageImpl<>(pageContent, pageable, allEvents.size());
    }
    
    /**
     * 查询用户特定类型事件
     */
    @Transactional(readOnly = true)
    public List<UserBehaviorEvent> findEventsByUserAndType(String userId, BehaviorEventType eventType) {
        return eventRepository.findByUserIdAndEventType(userId, eventType);
    }
    
    /**
     * 查询特定状态的事件
     */
    @Transactional(readOnly = true)
    public Page<UserBehaviorEvent> findEventsByStatus(ProcessStatus status, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("eventTimestamp").descending());
        return eventRepository.findByProcessStatus(status, pageable);
    }
    
    /**
     * 查询时间范围内的事件
     */
    @Transactional(readOnly = true)
    public Page<UserBehaviorEvent> findEventsByTimeRange(LocalDateTime startTime, LocalDateTime endTime, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("eventTimestamp").descending());
        return eventRepository.findEventsByTimeRange(startTime, endTime, pageable);
    }
    
    /**
     * 获取事件统计信息
     */
    @Transactional(readOnly = true)
    public Map<String, Object> getEventStatistics() {
        Map<String, Object> stats = new HashMap<>();
        
        // 总事件数
        long totalEvents = eventRepository.count();
        stats.put("totalEvents", totalEvents);
        
        // 各状态事件数
        for (ProcessStatus status : ProcessStatus.values()) {
            long count = eventRepository.countByProcessStatus(status);
            stats.put(status.name().toLowerCase() + "Count", count);
        }
        
        // 处理成功率
        double successRate = eventRepository.getProcessSuccessRate();
        stats.put("successRate", successRate);
        
        // 今日事件数
        LocalDateTime todayStart = LocalDateTime.now().withHour(0).withMinute(0).withSecond(0).withNano(0);
        LocalDateTime todayEnd = todayStart.plusDays(1);
        long todayEvents = eventRepository.countByTimeRange(todayStart, todayEnd);
        stats.put("todayEvents", todayEvents);
        
        return stats;
    }
    
    /**
     * 获取用户统计信息
     */
    @Transactional(readOnly = true)
    public Map<String, Object> getUserStatistics(String userId) {
        Map<String, Object> stats = new HashMap<>();
        
        // 用户总事件数
        long totalEvents = eventRepository.countByUserId(userId);
        stats.put("totalEvents", totalEvents);
        
        // 用户各类型事件数
        for (BehaviorEventType eventType : BehaviorEventType.values()) {
            long count = eventRepository.countByUserIdAndEventType(userId, eventType);
            stats.put(eventType.name().toLowerCase() + "Count", count);
        }
        
        // 用户处理成功率
        double successRate = eventRepository.getProcessSuccessRateByUser(userId);
        stats.put("successRate", successRate);
        
        // 用户今日事件数
        LocalDateTime todayStart = LocalDateTime.now().withHour(0).withMinute(0).withSecond(0).withNano(0);
        LocalDateTime todayEnd = todayStart.plusDays(1);
        long todayEvents = eventRepository.countByUserAndTimeRange(userId, todayStart, todayEnd);
        stats.put("todayEvents", todayEvents);
        
        return stats;
    }
}