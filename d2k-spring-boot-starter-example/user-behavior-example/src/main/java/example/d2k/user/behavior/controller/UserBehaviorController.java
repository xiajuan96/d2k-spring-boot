package example.d2k.user.behavior.controller;

import example.d2k.user.behavior.entity.BehaviorEventType;
import example.d2k.user.behavior.entity.ProcessStatus;
import example.d2k.user.behavior.entity.UserBehaviorEvent;
import example.d2k.user.behavior.service.UserBehaviorEventService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.*;

/**
 * 用户行为事件控制器
 *
 * @author xiajuan96
 */
@RestController
@RequestMapping("/api/user-behavior")
public class UserBehaviorController {

    private static final Logger logger = LoggerFactory.getLogger(UserBehaviorController.class);

    @Autowired
    private UserBehaviorEventService eventService;

    /**
     * 创建用户行为事件
     */
    @PostMapping("/events")
    public ResponseEntity<Map<String, Object>> createEvent(@RequestBody Map<String, Object> request) {
        try {
            String userId = (String) request.get("userId");
            String eventTypeName = (String) request.get("eventType");
            String eventName = (String) request.get("eventName");
            String pagePath = (String) request.get("pagePath");
            String ipAddress = (String) request.get("ipAddress");
            String deviceType = (String) request.get("deviceType");

            @SuppressWarnings("unchecked")
            Map<String, Object> eventProperties = (Map<String, Object>) request.get("eventProperties");

            // 参数验证
            if (userId == null || userId.trim().isEmpty()) {
                return ResponseEntity.badRequest().body(createErrorResponse("用户ID不能为空"));
            }

            if (eventTypeName == null || eventTypeName.trim().isEmpty()) {
                return ResponseEntity.badRequest().body(createErrorResponse("事件类型不能为空"));
            }

            BehaviorEventType eventType;
            try {
                eventType = BehaviorEventType.valueOf(eventTypeName.toUpperCase());
            } catch (IllegalArgumentException e) {
                return ResponseEntity.badRequest().body(createErrorResponse("无效的事件类型: " + eventTypeName));
            }

            // 创建事件
            UserBehaviorEvent event = eventService.createEvent(
                    userId, eventType, eventName, pagePath, ipAddress, deviceType, eventProperties
            );

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "用户行为事件创建成功");
            response.put("data", convertEventToMap(event));

            logger.info("用户行为事件创建成功: eventId={}, userId={}, eventType={}",
                    event.getId(), userId, eventType);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("创建用户行为事件失败", e);
            return ResponseEntity.internalServerError().body(createErrorResponse("创建用户行为事件失败: " + e.getMessage()));
        }
    }

    /**
     * 批量创建用户行为事件
     */
    @PostMapping("/events/batch")
    public ResponseEntity<Map<String, Object>> createEvents(@RequestBody Map<String, Object> request) {
        try {
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> eventRequests = (List<Map<String, Object>>) request.get("events");

            if (eventRequests == null || eventRequests.isEmpty()) {
                return ResponseEntity.badRequest().body(createErrorResponse("事件列表不能为空"));
            }

            List<UserBehaviorEvent> events = new ArrayList<>();

            for (Map<String, Object> eventRequest : eventRequests) {
                String userId = (String) eventRequest.get("userId");
                String eventTypeName = (String) eventRequest.get("eventType");
                String eventName = (String) eventRequest.get("eventName");
                String pagePath = (String) eventRequest.get("pagePath");
                String ipAddress = (String) eventRequest.get("ipAddress");
                String deviceType = (String) eventRequest.get("deviceType");

                @SuppressWarnings("unchecked")
                Map<String, Object> eventProperties = (Map<String, Object>) eventRequest.get("eventProperties");

                // 参数验证
                if (userId == null || userId.trim().isEmpty()) {
                    return ResponseEntity.badRequest().body(createErrorResponse("用户ID不能为空"));
                }

                BehaviorEventType eventType;
                try {
                    eventType = BehaviorEventType.valueOf(eventTypeName.toUpperCase());
                } catch (IllegalArgumentException e) {
                    return ResponseEntity.badRequest().body(createErrorResponse("无效的事件类型: " + eventTypeName));
                }

                UserBehaviorEvent event = new UserBehaviorEvent();
                event.setUserId(userId);
                event.setEventType(eventType);
                event.setEventName(eventName);
                event.setPagePath(pagePath);
                event.setIpAddress(ipAddress);
                event.setDeviceType(deviceType);
                event.setPropertiesMap(eventProperties);

                events.add(event);
            }

            // 批量创建事件
            List<UserBehaviorEvent> savedEvents = eventService.createEvents(events);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "批量创建用户行为事件成功");
            Map<String, Object> batchData = new HashMap<>();
            batchData.put("count", savedEvents.size());
            batchData.put("events", savedEvents.stream().map(this::convertEventToMap).collect(java.util.stream.Collectors.toList()));
            response.put("data", batchData);

            logger.info("批量创建用户行为事件成功: count={}", savedEvents.size());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("批量创建用户行为事件失败", e);
            return ResponseEntity.internalServerError().body(createErrorResponse("批量创建用户行为事件失败: " + e.getMessage()));
        }
    }

    /**
     * 查询用户行为事件详情
     */
    @GetMapping("/events/{eventId}")
    public ResponseEntity<Map<String, Object>> getEvent(@PathVariable String eventId) {
        try {
            UserBehaviorEvent event = eventService.findEventById(eventId);

            if (event == null) {
                return ResponseEntity.notFound().build();
            }

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "查询用户行为事件成功");
            response.put("data", convertEventToMap(event));

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("查询用户行为事件失败: eventId={}", eventId, e);
            return ResponseEntity.internalServerError().body(createErrorResponse("查询用户行为事件失败: " + e.getMessage()));
        }
    }

    /**
     * 查询用户的所有行为事件
     */
    @GetMapping("/users/{userId}/events")
    public ResponseEntity<Map<String, Object>> getUserEvents(
            @PathVariable String userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        try {
            Page<UserBehaviorEvent> eventsPage = eventService.findEventsByUser(userId, page, size);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "查询用户行为事件成功");
            Map<String, Object> userEventsData = new HashMap<>();
            userEventsData.put("events", eventsPage.getContent().stream().map(this::convertEventToMap).collect(java.util.stream.Collectors.toList()));
            userEventsData.put("totalElements", eventsPage.getTotalElements());
            userEventsData.put("totalPages", eventsPage.getTotalPages());
            userEventsData.put("currentPage", page);
            userEventsData.put("pageSize", size);
            response.put("data", userEventsData);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("查询用户行为事件失败: userId={}", userId, e);
            return ResponseEntity.internalServerError().body(createErrorResponse("查询用户行为事件失败: " + e.getMessage()));
        }
    }

    /**
     * 查询用户特定类型的行为事件
     */
    @GetMapping("/users/{userId}/events/{eventType}")
    public ResponseEntity<Map<String, Object>> getUserEventsByType(
            @PathVariable String userId,
            @PathVariable String eventType) {
        try {
            BehaviorEventType behaviorEventType;
            try {
                behaviorEventType = BehaviorEventType.valueOf(eventType.toUpperCase());
            } catch (IllegalArgumentException e) {
                return ResponseEntity.badRequest().body(createErrorResponse("无效的事件类型: " + eventType));
            }

            List<UserBehaviorEvent> events = eventService.findEventsByUserAndType(userId, behaviorEventType);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "查询用户特定类型行为事件成功");
            Map<String, Object> typeEventsData = new HashMap<>();
            typeEventsData.put("events", events.stream().map(this::convertEventToMap).collect(java.util.stream.Collectors.toList()));
            typeEventsData.put("count", events.size());
            response.put("data", typeEventsData);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("查询用户特定类型行为事件失败: userId={}, eventType={}", userId, eventType, e);
            return ResponseEntity.internalServerError().body(createErrorResponse("查询用户特定类型行为事件失败: " + e.getMessage()));
        }
    }

    /**
     * 查询特定状态的事件
     */
    @GetMapping("/events/status/{status}")
    public ResponseEntity<Map<String, Object>> getEventsByStatus(
            @PathVariable String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        try {
            ProcessStatus processStatus;
            try {
                processStatus = ProcessStatus.valueOf(status.toUpperCase());
            } catch (IllegalArgumentException e) {
                return ResponseEntity.badRequest().body(createErrorResponse("无效的处理状态: " + status));
            }

            Page<UserBehaviorEvent> eventsPage = eventService.findEventsByStatus(processStatus, page, size);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "查询特定状态事件成功");
            Map<String, Object> statusEventsData = new HashMap<>();
            statusEventsData.put("events", eventsPage.getContent().stream().map(this::convertEventToMap).collect(java.util.stream.Collectors.toList()));
            statusEventsData.put("totalElements", eventsPage.getTotalElements());
            statusEventsData.put("totalPages", eventsPage.getTotalPages());
            statusEventsData.put("currentPage", page);
            statusEventsData.put("pageSize", size);
            response.put("data", statusEventsData);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("查询特定状态事件失败: status={}", status, e);
            return ResponseEntity.internalServerError().body(createErrorResponse("查询特定状态事件失败: " + e.getMessage()));
        }
    }

    /**
     * 查询时间范围内的事件
     */
    @GetMapping("/events/time-range")
    public ResponseEntity<Map<String, Object>> getEventsByTimeRange(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        try {
            Page<UserBehaviorEvent> eventsPage = eventService.findEventsByTimeRange(startTime, endTime, page, size);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "查询时间范围内事件成功");
            Map<String, Object> timeRangeData = new HashMap<>();
            timeRangeData.put("events", eventsPage.getContent().stream().map(this::convertEventToMap).collect(java.util.stream.Collectors.toList()));
            timeRangeData.put("totalElements", eventsPage.getTotalElements());
            timeRangeData.put("totalPages", eventsPage.getTotalPages());
            timeRangeData.put("currentPage", page);
            timeRangeData.put("pageSize", size);
            timeRangeData.put("startTime", startTime.toString());
            timeRangeData.put("endTime", endTime.toString());
            response.put("data", timeRangeData);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("查询时间范围内事件失败: startTime={}, endTime={}", startTime, endTime, e);
            return ResponseEntity.internalServerError().body(createErrorResponse("查询时间范围内事件失败: " + e.getMessage()));
        }
    }

    /**
     * 更新事件状态
     */
    @PutMapping("/events/{eventId}/status")
    public ResponseEntity<Map<String, Object>> updateEventStatus(
            @PathVariable String eventId,
            @RequestBody Map<String, Object> request) {
        try {
            String statusName = (String) request.get("status");

            if (statusName == null || statusName.trim().isEmpty()) {
                return ResponseEntity.badRequest().body(createErrorResponse("状态不能为空"));
            }

            ProcessStatus newStatus;
            try {
                newStatus = ProcessStatus.valueOf(statusName.toUpperCase());
            } catch (IllegalArgumentException e) {
                return ResponseEntity.badRequest().body(createErrorResponse("无效的处理状态: " + statusName));
            }

            boolean success = eventService.updateEventStatus(eventId, newStatus);

            if (success) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", true);
                response.put("message", "更新事件状态成功");
                response.put("data", new HashMap() {{
                    put("eventId", eventId);
                    put("newStatus", newStatus.name());
                }});

                return ResponseEntity.ok(response);
            } else {
                return ResponseEntity.badRequest().body(createErrorResponse("更新事件状态失败，请检查事件ID和状态转换是否合法"));
            }

        } catch (Exception e) {
            logger.error("更新事件状态失败: eventId={}", eventId, e);
            return ResponseEntity.internalServerError().body(createErrorResponse("更新事件状态失败: " + e.getMessage()));
        }
    }

    /**
     * 手动重试事件
     */
    @PostMapping("/events/{eventId}/retry")
    public ResponseEntity<Map<String, Object>> retryEvent(@PathVariable String eventId) {
        try {
            boolean success = eventService.retryEvent(eventId);

            if (success) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", true);
                response.put("message", "手动重试事件成功");
                Map<String, Object> retryData = new HashMap<>();
                retryData.put("eventId", eventId);
                response.put("data", retryData);

                logger.info("手动重试事件成功: eventId={}", eventId);
                return ResponseEntity.ok(response);
            } else {
                return ResponseEntity.badRequest().body(createErrorResponse("手动重试事件失败，请检查事件ID和状态是否允许重试"));
            }

        } catch (Exception e) {
            logger.error("手动重试事件失败: eventId={}", eventId, e);
            return ResponseEntity.internalServerError().body(createErrorResponse("手动重试事件失败: " + e.getMessage()));
        }
    }

    /**
     * 取消事件处理
     */
    @PostMapping("/events/{eventId}/cancel")
    public ResponseEntity<Map<String, Object>> cancelEvent(@PathVariable String eventId) {
        try {
            boolean success = eventService.cancelEvent(eventId);

            if (success) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", true);
                response.put("message", "取消事件处理成功");
                Map<String, Object> cancelData = new HashMap<>();
                cancelData.put("eventId", eventId);
                response.put("data", cancelData);

                logger.info("取消事件处理成功: eventId={}", eventId);
                return ResponseEntity.ok(response);
            } else {
                return ResponseEntity.badRequest().body(createErrorResponse("取消事件处理失败，请检查事件ID和状态是否允许取消"));
            }

        } catch (Exception e) {
            logger.error("取消事件处理失败: eventId={}", eventId, e);
            return ResponseEntity.internalServerError().body(createErrorResponse("取消事件处理失败: " + e.getMessage()));
        }
    }

    /**
     * 触发批量处理
     */
    @PostMapping("/events/batch-process")
    public ResponseEntity<Map<String, Object>> triggerBatchProcess() {
        try {
            eventService.batchProcessEvents();

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "触发批量处理成功");
            Map<String, Object> data = new HashMap<>();
            data.put("timestamp", LocalDateTime.now().toString());
            response.put("data", data);

            logger.info("触发批量处理成功");
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("触发批量处理失败", e);
            return ResponseEntity.internalServerError().body(createErrorResponse("触发批量处理失败: " + e.getMessage()));
        }
    }

    /**
     * 触发延迟事件处理
     */
    @PostMapping("/events/process-delayed")
    public ResponseEntity<Map<String, Object>> triggerDelayedProcess() {
        try {
            eventService.processDelayedEvents();

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "触发延迟事件处理成功");
            Map<String, Object> delayedData = new HashMap<>();
            delayedData.put("timestamp", LocalDateTime.now().toString());
            response.put("data", delayedData);

            logger.info("触发延迟事件处理成功");
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("触发延迟事件处理失败", e);
            return ResponseEntity.internalServerError().body(createErrorResponse("触发延迟事件处理失败: " + e.getMessage()));
        }
    }

    /**
     * 触发重试事件处理
     */
    @PostMapping("/events/process-retry")
    public ResponseEntity<Map<String, Object>> triggerRetryProcess() {
        try {
            eventService.processRetryEvents();

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "触发重试事件处理成功");
            Map<String, Object> retryProcessData = new HashMap<>();
            retryProcessData.put("timestamp", LocalDateTime.now().toString());
            response.put("data", retryProcessData);

            logger.info("触发重试事件处理成功");
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("触发重试事件处理失败", e);
            return ResponseEntity.internalServerError().body(createErrorResponse("触发重试事件处理失败: " + e.getMessage()));
        }
    }

    /**
     * 触发事件清理
     */
    @PostMapping("/events/cleanup")
    public ResponseEntity<Map<String, Object>> triggerCleanup() {
        try {
            eventService.cleanupExpiredEvents();

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "触发事件清理成功");
            Map<String, Object> cleanupData = new HashMap<>();
            cleanupData.put("timestamp", LocalDateTime.now().toString());
            response.put("data", cleanupData);

            logger.info("触发事件清理成功");
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("触发事件清理失败", e);
            return ResponseEntity.internalServerError().body(createErrorResponse("触发事件清理失败: " + e.getMessage()));
        }
    }

    /**
     * 获取事件统计信息
     */
    @GetMapping("/statistics")
    public ResponseEntity<Map<String, Object>> getStatistics() {
        try {
            Map<String, Object> statistics = eventService.getEventStatistics();

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "获取事件统计信息成功");
            response.put("data", statistics);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("获取事件统计信息失败", e);
            return ResponseEntity.internalServerError().body(createErrorResponse("获取事件统计信息失败: " + e.getMessage()));
        }
    }

    /**
     * 获取用户统计信息
     */
    @GetMapping("/users/{userId}/statistics")
    public ResponseEntity<Map<String, Object>> getUserStatistics(@PathVariable String userId) {
        try {
            Map<String, Object> statistics = eventService.getUserStatistics(userId);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "获取用户统计信息成功");
            response.put("data", statistics);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("获取用户统计信息失败: userId={}", userId, e);
            return ResponseEntity.internalServerError().body(createErrorResponse("获取用户统计信息失败: " + e.getMessage()));
        }
    }

    /**
     * 获取事件类型枚举
     */
    @GetMapping("/event-types")
    public ResponseEntity<Map<String, Object>> getEventTypes() {
        try {
            List<Map<String, Object>> eventTypes = new ArrayList<>();

            for (BehaviorEventType eventType : BehaviorEventType.values()) {
                Map<String, Object> typeInfo = new HashMap<>();
                typeInfo.put("name", eventType.name());
                typeInfo.put("displayName", eventType.getDisplayName());
                typeInfo.put("description", eventType.getDescription());
                typeInfo.put("needsDelayedProcessing", eventType.needsDelayedProcessing());
                typeInfo.put("defaultDelayMinutes", eventType.getDefaultDelayMinutes());
                typeInfo.put("priority", eventType.getPriority());
                typeInfo.put("recommendedBatchSize", eventType.getRecommendedBatchSize());
                eventTypes.add(typeInfo);
            }

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "获取事件类型枚举成功");
            response.put("data", eventTypes);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("获取事件类型枚举失败", e);
            return ResponseEntity.internalServerError().body(createErrorResponse("获取事件类型枚举失败: " + e.getMessage()));
        }
    }

    /**
     * 获取处理状态枚举
     */
    @GetMapping("/process-statuses")
    public ResponseEntity<Map<String, Object>> getProcessStatuses() {
        try {
            List<Map<String, Object>> processStatuses = new ArrayList<>();

            for (ProcessStatus status : ProcessStatus.values()) {
                Map<String, Object> statusInfo = new HashMap<>();
                statusInfo.put("name", status.name());
                statusInfo.put("displayName", status.getDisplayName());
                statusInfo.put("description", status.getDescription());
                statusInfo.put("isFinalStatus", status.isFinalStatus());
                statusInfo.put("canRetry", status.canRetry());
                statusInfo.put("isSuccessStatus", status.isSuccessStatus());
                statusInfo.put("isFailureStatus", status.isFailureStatus());
                statusInfo.put("isProcessingStatus", status.isProcessingStatus());
                statusInfo.put("isWaitingStatus", status.isWaitingStatus());
                statusInfo.put("needsManualIntervention", status.needsManualIntervention());
                statusInfo.put("priority", status.getPriority());
                processStatuses.add(statusInfo);
            }

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "获取处理状态枚举成功");
            response.put("data", processStatuses);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("获取处理状态枚举失败", e);
            return ResponseEntity.internalServerError().body(createErrorResponse("获取处理状态枚举失败: " + e.getMessage()));
        }
    }

    /**
     * 健康检查
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> health() {
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "用户行为分析服务运行正常");
        Map<String, Object> healthData = new HashMap<>();
        healthData.put("service", "user-behavior-example");
        healthData.put("status", "UP");
        healthData.put("timestamp", LocalDateTime.now().toString());
        response.put("data", healthData);

        return ResponseEntity.ok(response);
    }

    /**
     * 模拟用户行为事件（用于测试）
     */
    @PostMapping("/simulate")
    public ResponseEntity<Map<String, Object>> simulateUserBehavior(@RequestBody Map<String, Object> request) {
        try {
            String userId = (String) request.getOrDefault("userId", "test_user_" + System.currentTimeMillis());
            Integer eventCount = (Integer) request.getOrDefault("eventCount", 10);

            List<UserBehaviorEvent> simulatedEvents = new ArrayList<>();
            Random random = new Random();
            BehaviorEventType[] eventTypes = BehaviorEventType.values();
            String[] deviceTypes = {"PC", "Mobile", "Tablet"};
            String[] pagePaths = {"/home", "/product", "/cart", "/checkout", "/profile", "/search"};

            for (int i = 0; i < eventCount; i++) {
                BehaviorEventType eventType = eventTypes[random.nextInt(eventTypes.length)];
                String deviceType = deviceTypes[random.nextInt(deviceTypes.length)];
                String pagePath = pagePaths[random.nextInt(pagePaths.length)];
                String ipAddress = "192.168.1." + (random.nextInt(254) + 1);

                Map<String, Object> eventProperties = new HashMap<>();
                eventProperties.put("sessionId", "session_" + random.nextInt(1000));
                eventProperties.put("userAgent", "Test User Agent");
                eventProperties.put("referrer", "https://example.com");

                if (eventType == BehaviorEventType.PRODUCT_VIEW || eventType == BehaviorEventType.ADD_TO_CART) {
                    eventProperties.put("productId", "product_" + random.nextInt(100));
                    eventProperties.put("productName", "Test Product " + random.nextInt(100));
                    eventProperties.put("price", random.nextDouble() * 1000);
                }

                if (eventType == BehaviorEventType.SEARCH) {
                    eventProperties.put("keyword", "test keyword " + random.nextInt(50));
                    eventProperties.put("resultCount", random.nextInt(100));
                }

                if (eventType == BehaviorEventType.PURCHASE) {
                    eventProperties.put("orderId", "order_" + random.nextInt(10000));
                    eventProperties.put("amount", random.nextDouble() * 500);
                    eventProperties.put("currency", "CNY");
                }

                UserBehaviorEvent event = new UserBehaviorEvent();
                event.setUserId(userId);
                event.setEventType(eventType);
                event.setEventName(eventType.getDisplayName());
                event.setPagePath(pagePath);
                event.setIpAddress(ipAddress);
                event.setDeviceType(deviceType);

                simulatedEvents.add(event);
            }

            // 批量创建模拟事件
            List<UserBehaviorEvent> savedEvents = eventService.createEvents(simulatedEvents);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "模拟用户行为事件成功");
            Map<String, Object> simulateData = new HashMap<>();
            simulateData.put("userId", userId);
            simulateData.put("eventCount", savedEvents.size());
            simulateData.put("events", savedEvents.stream().map(this::convertEventToMap).collect(java.util.stream.Collectors.toList()));
            response.put("data", simulateData);

            logger.info("模拟用户行为事件成功: userId={}, eventCount={}", userId, savedEvents.size());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("模拟用户行为事件失败", e);
            return ResponseEntity.internalServerError().body(createErrorResponse("模拟用户行为事件失败: " + e.getMessage()));
        }
    }

    /**
     * 将事件对象转换为Map
     */
    private Map<String, Object> convertEventToMap(UserBehaviorEvent event) {
        Map<String, Object> eventMap = new HashMap<>();
        eventMap.put("id", event.getId());
        eventMap.put("userId", event.getUserId());
        eventMap.put("eventType", event.getEventType().name());
        eventMap.put("eventTypeName", event.getEventType().getDisplayName());
        eventMap.put("eventName", event.getEventName());
        eventMap.put("pagePath", event.getPagePath());
        eventMap.put("ipAddress", event.getIpAddress());
        eventMap.put("deviceType", event.getDeviceType());
        eventMap.put("eventProperties", event.getPropertiesMap());
        eventMap.put("eventTimestamp", event.getEventTimestamp() != null ? event.getEventTimestamp().toString() : null);
        eventMap.put("processStatus", event.getProcessStatus().name());
        eventMap.put("processStatusName", event.getProcessStatus().getDisplayName());
        eventMap.put("retryCount", event.getRetryCount());
        eventMap.put("maxRetryCount", event.getMaxRetryCount());
        eventMap.put("isDelayedProcessing", event.getProcessStatus() == ProcessStatus.DELAYED);
        eventMap.put("delayUntil", null); // 延迟处理时间基于事件类型的默认延迟分钟数计算
        eventMap.put("lastProcessedTime", event.getActualProcessTime() != null ? event.getActualProcessTime().toString() : null);
        LocalDateTime nextRetryTime = event.getRetryCount() > 0 ? LocalDateTime.now().plusMinutes(5 * event.getRetryCount()) : null;
        eventMap.put("nextRetryTime", nextRetryTime != null ? nextRetryTime.toString() : null);
        eventMap.put("errorMessage", event.getErrorMessage());
        return eventMap;
    }

    /**
     * 创建错误响应
     */
    private Map<String, Object> createErrorResponse(String message) {
        Map<String, Object> response = new HashMap<>();
        response.put("success", false);
        response.put("message", message);
        response.put("timestamp", LocalDateTime.now().toString());
        return response;
    }
}