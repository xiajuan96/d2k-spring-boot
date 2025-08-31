package example.d2k.user.behavior.entity;

/**
 * 用户行为事件类型枚举
 * @author xiajuan96
 */
public enum BehaviorEventType {
    
    // 页面访问类
    PAGE_VIEW("页面浏览", "用户访问页面", true, 5),
    PAGE_ENTER("页面进入", "用户进入页面", true, 5),
    PAGE_EXIT("页面退出", "用户离开页面", true, 10),
    
    // 点击交互类
    CLICK("点击事件", "用户点击元素", true, 2),
    BUTTON_CLICK("按钮点击", "用户点击按钮", true, 2),
    LINK_CLICK("链接点击", "用户点击链接", true, 2),
    
    // 表单交互类
    FORM_START("表单开始", "用户开始填写表单", true, 3),
    FORM_SUBMIT("表单提交", "用户提交表单", false, 0),
    FORM_ABANDON("表单放弃", "用户放弃填写表单", true, 15),
    INPUT_FOCUS("输入框聚焦", "用户聚焦输入框", true, 30),
    INPUT_BLUR("输入框失焦", "用户离开输入框", true, 30),
    
    // 搜索行为类
    SEARCH("搜索事件", "用户执行搜索", true, 5),
    SEARCH_RESULT_CLICK("搜索结果点击", "用户点击搜索结果", true, 3),
    FILTER_APPLY("筛选应用", "用户应用筛选条件", true, 5),
    
    // 商品相关类
    PRODUCT_VIEW("商品浏览", "用户查看商品详情", true, 10),
    PRODUCT_LIKE("商品收藏", "用户收藏商品", false, 0),
    ADD_TO_CART("加入购物车", "用户将商品加入购物车", false, 0),
    REMOVE_FROM_CART("移出购物车", "用户从购物车移除商品", true, 5),
    
    // 购买流程类
    CHECKOUT_START("开始结账", "用户开始结账流程", false, 0),
    CHECKOUT_STEP("结账步骤", "用户完成结账步骤", true, 2),
    PAYMENT_START("开始支付", "用户开始支付", false, 0),
    PURCHASE("购买完成", "用户完成购买", false, 0),
    
    // 用户账户类
    LOGIN("用户登录", "用户登录系统", false, 0),
    LOGOUT("用户登出", "用户登出系统", true, 5),
    REGISTER("用户注册", "用户注册账户", false, 0),
    PROFILE_UPDATE("资料更新", "用户更新个人资料", true, 10),
    
    // 内容互动类
    CONTENT_VIEW("内容浏览", "用户查看内容", true, 15),
    CONTENT_SHARE("内容分享", "用户分享内容", false, 0),
    CONTENT_LIKE("内容点赞", "用户点赞内容", true, 5),
    COMMENT_POST("发表评论", "用户发表评论", false, 0),
    
    // 下载和上传类
    FILE_DOWNLOAD("文件下载", "用户下载文件", true, 5),
    FILE_UPLOAD("文件上传", "用户上传文件", false, 0),
    
    // 视频相关类
    VIDEO_PLAY("视频播放", "用户播放视频", true, 10),
    VIDEO_PAUSE("视频暂停", "用户暂停视频", true, 30),
    VIDEO_COMPLETE("视频完成", "用户观看完整视频", false, 0),
    
    // 错误和异常类
    ERROR_OCCURRED("错误发生", "系统发生错误", false, 0),
    TIMEOUT("操作超时", "用户操作超时", true, 5),
    
    // 自定义事件类
    CUSTOM("自定义事件", "用户自定义事件", true, 10);
    
    private final String displayName;
    private final String description;
    private final boolean needsDelayedProcessing;
    private final int defaultDelayMinutes;
    
    BehaviorEventType(String displayName, String description, boolean needsDelayedProcessing, int defaultDelayMinutes) {
        this.displayName = displayName;
        this.description = description;
        this.needsDelayedProcessing = needsDelayedProcessing;
        this.defaultDelayMinutes = defaultDelayMinutes;
    }
    
    public String getDisplayName() {
        return displayName;
    }
    
    public String getDescription() {
        return description;
    }
    
    public boolean needsDelayedProcessing() {
        return needsDelayedProcessing;
    }
    
    public int getDefaultDelayMinutes() {
        return defaultDelayMinutes;
    }
    
    /**
     * 是否为页面相关事件
     */
    public boolean isPageEvent() {
        return this == PAGE_VIEW || this == PAGE_ENTER || this == PAGE_EXIT;
    }
    
    /**
     * 是否为交互事件
     */
    public boolean isInteractionEvent() {
        return this == CLICK || this == BUTTON_CLICK || this == LINK_CLICK;
    }
    
    /**
     * 是否为表单事件
     */
    public boolean isFormEvent() {
        return this == FORM_START || this == FORM_SUBMIT || this == FORM_ABANDON ||
               this == INPUT_FOCUS || this == INPUT_BLUR;
    }
    
    /**
     * 是否为商业事件（与转化相关）
     */
    public boolean isBusinessEvent() {
        return this == ADD_TO_CART || this == CHECKOUT_START || this == PAYMENT_START ||
               this == PURCHASE || this == PRODUCT_LIKE;
    }
    
    /**
     * 是否为用户生命周期事件
     */
    public boolean isUserLifecycleEvent() {
        return this == LOGIN || this == LOGOUT || this == REGISTER || this == PROFILE_UPDATE;
    }
    
    /**
     * 是否为内容相关事件
     */
    public boolean isContentEvent() {
        return this == CONTENT_VIEW || this == CONTENT_SHARE || this == CONTENT_LIKE ||
               this == COMMENT_POST || this == VIDEO_PLAY || this == VIDEO_PAUSE || this == VIDEO_COMPLETE;
    }
    
    /**
     * 是否为高价值事件（需要立即处理）
     */
    public boolean isHighValueEvent() {
        return this == PURCHASE || this == REGISTER || this == ADD_TO_CART ||
               this == CHECKOUT_START || this == PAYMENT_START || this == ERROR_OCCURRED;
    }
    
    /**
     * 是否为低频事件（可以延迟较长时间处理）
     */
    public boolean isLowFrequencyEvent() {
        return this == CONTENT_VIEW || this == VIDEO_PLAY || this == FORM_ABANDON ||
               this == INPUT_FOCUS || this == INPUT_BLUR;
    }
    
    /**
     * 获取事件优先级（1-5，1最高）
     */
    public int getPriority() {
        if (isHighValueEvent()) {
            return 1;
        } else if (isBusinessEvent()) {
            return 2;
        } else if (isUserLifecycleEvent()) {
            return 3;
        } else if (isInteractionEvent() || isFormEvent()) {
            return 4;
        } else {
            return 5;
        }
    }
    
    /**
     * 根据事件类型获取推荐的批处理大小
     */
    public int getRecommendedBatchSize() {
        if (isHighValueEvent()) {
            return 10; // 高价值事件小批量处理
        } else if (isBusinessEvent()) {
            return 50;
        } else if (isPageEvent() || isInteractionEvent()) {
            return 100; // 高频事件大批量处理
        } else {
            return 200;
        }
    }
    
    /**
     * 根据名称查找事件类型
     */
    public static BehaviorEventType fromName(String name) {
        if (name == null || name.trim().isEmpty()) {
            return CUSTOM;
        }
        
        try {
            return BehaviorEventType.valueOf(name.toUpperCase());
        } catch (IllegalArgumentException e) {
            return CUSTOM;
        }
    }
    
    /**
     * 根据显示名称查找事件类型
     */
    public static BehaviorEventType fromDisplayName(String displayName) {
        if (displayName == null || displayName.trim().isEmpty()) {
            return CUSTOM;
        }
        
        for (BehaviorEventType type : values()) {
            if (type.getDisplayName().equals(displayName)) {
                return type;
            }
        }
        return CUSTOM;
    }
}