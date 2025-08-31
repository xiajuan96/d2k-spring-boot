package example.d2k.user.behavior.entity;

/**
 * 用户行为事件处理状态枚举
 * @author xiajuan96
 */
public enum ProcessStatus {
    
    /**
     * 待处理 - 事件已创建，等待处理
     */
    PENDING("待处理", "事件已创建，等待处理", false, true),
    
    /**
     * 处理中 - 事件正在被处理
     */
    PROCESSING("处理中", "事件正在被处理", false, false),
    
    /**
     * 已处理 - 事件处理完成
     */
    PROCESSED("已处理", "事件处理完成", true, false),
    
    /**
     * 处理失败 - 事件处理失败，可能需要重试
     */
    FAILED("处理失败", "事件处理失败，可能需要重试", false, true),
    
    /**
     * 已跳过 - 事件被跳过处理
     */
    SKIPPED("已跳过", "事件被跳过处理", true, false),
    
    /**
     * 已取消 - 事件处理被取消
     */
    CANCELLED("已取消", "事件处理被取消", true, false),
    
    /**
     * 重试中 - 事件正在重试处理
     */
    RETRYING("重试中", "事件正在重试处理", false, false),
    
    /**
     * 重试失败 - 事件重试失败，已达到最大重试次数
     */
    RETRY_FAILED("重试失败", "事件重试失败，已达到最大重试次数", true, false),
    
    /**
     * 延迟处理 - 事件安排延迟处理
     */
    DELAYED("延迟处理", "事件安排延迟处理", false, true),
    
    /**
     * 批处理中 - 事件在批处理队列中
     */
    BATCH_PROCESSING("批处理中", "事件在批处理队列中", false, false),
    
    /**
     * 已归档 - 事件已归档存储
     */
    ARCHIVED("已归档", "事件已归档存储", true, false);
    
    private final String displayName;
    private final String description;
    private final boolean isFinalStatus;
    private final boolean canRetry;
    
    ProcessStatus(String displayName, String description, boolean isFinalStatus, boolean canRetry) {
        this.displayName = displayName;
        this.description = description;
        this.isFinalStatus = isFinalStatus;
        this.canRetry = canRetry;
    }
    
    public String getDisplayName() {
        return displayName;
    }
    
    public String getDescription() {
        return description;
    }
    
    public boolean isFinalStatus() {
        return isFinalStatus;
    }
    
    public boolean canRetry() {
        return canRetry;
    }
    
    /**
     * 是否为成功状态
     */
    public boolean isSuccessStatus() {
        return this == PROCESSED;
    }
    
    /**
     * 是否为失败状态
     */
    public boolean isFailureStatus() {
        return this == FAILED || this == RETRY_FAILED || this == CANCELLED;
    }
    
    /**
     * 是否为处理中状态
     */
    public boolean isProcessingStatus() {
        return this == PROCESSING || this == RETRYING || this == BATCH_PROCESSING;
    }
    
    /**
     * 是否为等待状态
     */
    public boolean isWaitingStatus() {
        return this == PENDING || this == DELAYED;
    }
    
    /**
     * 是否需要人工干预
     */
    public boolean needsManualIntervention() {
        return this == RETRY_FAILED || this == CANCELLED;
    }
    
    /**
     * 是否可以重新处理
     */
    public boolean canReprocess() {
        return this == FAILED || this == RETRY_FAILED || this == CANCELLED || this == SKIPPED;
    }
    
    /**
     * 是否可以取消
     */
    public boolean canCancel() {
        return this == PENDING || this == DELAYED || this == FAILED;
    }
    
    /**
     * 是否可以归档
     */
    public boolean canArchive() {
        return isFinalStatus && this != ARCHIVED;
    }
    
    /**
     * 获取下一个可能的状态
     */
    public ProcessStatus[] getNextPossibleStatuses() {
        switch (this) {
            case PENDING:
                return new ProcessStatus[]{PROCESSING, DELAYED, CANCELLED, SKIPPED};
            case PROCESSING:
                return new ProcessStatus[]{PROCESSED, FAILED, CANCELLED};
            case PROCESSED:
                return new ProcessStatus[]{ARCHIVED};
            case FAILED:
                return new ProcessStatus[]{RETRYING, CANCELLED, SKIPPED};
            case SKIPPED:
                return new ProcessStatus[]{PENDING, ARCHIVED};
            case CANCELLED:
                return new ProcessStatus[]{PENDING, ARCHIVED};
            case RETRYING:
                return new ProcessStatus[]{PROCESSED, RETRY_FAILED, CANCELLED};
            case RETRY_FAILED:
                return new ProcessStatus[]{PENDING, CANCELLED, ARCHIVED};
            case DELAYED:
                return new ProcessStatus[]{PENDING, PROCESSING, CANCELLED};
            case BATCH_PROCESSING:
                return new ProcessStatus[]{PROCESSED, FAILED, CANCELLED};
            case ARCHIVED:
                return new ProcessStatus[]{}; // 归档状态不能转换到其他状态
            default:
                return new ProcessStatus[]{};
        }
    }
    
    /**
     * 检查是否可以转换到目标状态
     */
    public boolean canTransitionTo(ProcessStatus targetStatus) {
        ProcessStatus[] possibleStatuses = getNextPossibleStatuses();
        for (ProcessStatus status : possibleStatuses) {
            if (status == targetStatus) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * 获取状态优先级（用于排序，数字越小优先级越高）
     */
    public int getPriority() {
        switch (this) {
            case PROCESSING:
            case RETRYING:
            case BATCH_PROCESSING:
                return 1; // 正在处理的最高优先级
            case FAILED:
                return 2; // 失败的需要及时处理
            case PENDING:
                return 3; // 待处理的正常优先级
            case DELAYED:
                return 4; // 延迟处理的较低优先级
            case RETRY_FAILED:
                return 5; // 重试失败的需要人工干预
            case PROCESSED:
            case SKIPPED:
            case CANCELLED:
                return 6; // 已完成的状态
            case ARCHIVED:
                return 7; // 归档的最低优先级
            default:
                return 8;
        }
    }
    
    /**
     * 根据名称查找状态
     */
    public static ProcessStatus fromName(String name) {
        if (name == null || name.trim().isEmpty()) {
            return PENDING;
        }
        
        try {
            return ProcessStatus.valueOf(name.toUpperCase());
        } catch (IllegalArgumentException e) {
            return PENDING;
        }
    }
    
    /**
     * 根据显示名称查找状态
     */
    public static ProcessStatus fromDisplayName(String displayName) {
        if (displayName == null || displayName.trim().isEmpty()) {
            return PENDING;
        }
        
        for (ProcessStatus status : values()) {
            if (status.getDisplayName().equals(displayName)) {
                return status;
            }
        }
        return PENDING;
    }
    
    /**
     * 获取所有最终状态
     */
    public static ProcessStatus[] getFinalStatuses() {
        return new ProcessStatus[]{PROCESSED, SKIPPED, CANCELLED, RETRY_FAILED, ARCHIVED};
    }
    
    /**
     * 获取所有活跃状态（非最终状态）
     */
    public static ProcessStatus[] getActiveStatuses() {
        return new ProcessStatus[]{PENDING, PROCESSING, FAILED, RETRYING, DELAYED, BATCH_PROCESSING};
    }
    
    /**
     * 获取所有需要重试的状态
     */
    public static ProcessStatus[] getRetryableStatuses() {
        return new ProcessStatus[]{PENDING, FAILED, DELAYED};
    }
}