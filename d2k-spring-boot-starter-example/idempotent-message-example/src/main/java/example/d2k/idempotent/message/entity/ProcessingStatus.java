package example.d2k.idempotent.message.entity;

/**
 * 消息处理状态枚举
 * 定义消息在处理过程中的各种状态
 * @author xiajuan96
 */
public enum ProcessingStatus {
    
    /**
     * 待处理 - 消息已接收，等待处理
     */
    PENDING("待处理", "消息已接收，等待处理", false, false, true),
    
    /**
     * 处理中 - 消息正在被处理
     */
    PROCESSING("处理中", "消息正在被处理", false, false, false),
    
    /**
     * 处理成功 - 消息处理完成且成功
     */
    SUCCESS("处理成功", "消息处理完成且成功", true, false, false),
    
    /**
     * 处理失败 - 消息处理失败，可能需要重试
     */
    FAILED("处理失败", "消息处理失败，可能需要重试", true, true, true),
    
    /**
     * 重试中 - 消息正在重试处理
     */
    RETRYING("重试中", "消息正在重试处理", false, false, false),
    
    /**
     * 重试失败 - 消息重试处理失败
     */
    RETRY_FAILED("重试失败", "消息重试处理失败", true, true, true),
    
    /**
     * 重试耗尽 - 消息重试次数已用完，不再重试
     */
    RETRY_EXHAUSTED("重试耗尽", "消息重试次数已用完，不再重试", true, true, false),
    
    /**
     * 已跳过 - 消息被跳过处理（如重复消息）
     */
    SKIPPED("已跳过", "消息被跳过处理（如重复消息）", true, false, false),
    
    /**
     * 已取消 - 消息处理被取消
     */
    CANCELLED("已取消", "消息处理被取消", true, false, false),
    
    /**
     * 超时 - 消息处理超时
     */
    TIMEOUT("超时", "消息处理超时", true, true, true),
    
    /**
     * 已归档 - 消息已归档，不再处理
     */
    ARCHIVED("已归档", "消息已归档，不再处理", true, false, false),
    
    /**
     * 无效消息 - 消息格式或内容无效
     */
    INVALID("无效消息", "消息格式或内容无效", true, false, false),
    
    /**
     * 重复消息 - 检测到重复消息，已忽略
     */
    DUPLICATE("重复消息", "检测到重复消息，已忽略", true, false, false);
    
    private final String displayName;
    private final String description;
    private final boolean isFinal;
    private final boolean isError;
    private final boolean canRetry;
    
    ProcessingStatus(String displayName, String description, boolean isFinal, boolean isError, boolean canRetry) {
        this.displayName = displayName;
        this.description = description;
        this.isFinal = isFinal;
        this.isError = isError;
        this.canRetry = canRetry;
    }
    
    public String getDisplayName() {
        return displayName;
    }
    
    public String getDescription() {
        return description;
    }
    
    public boolean isFinal() {
        return isFinal;
    }
    
    public boolean isError() {
        return isError;
    }
    
    public boolean canRetry() {
        return canRetry;
    }
    
    /**
     * 判断是否为成功状态
     */
    public boolean isSuccess() {
        return this == SUCCESS;
    }
    
    /**
     * 判断是否为失败状态
     */
    public boolean isFailure() {
        return isError;
    }
    
    /**
     * 判断是否为处理中状态
     */
    public boolean isProcessing() {
        return this == PROCESSING || this == RETRYING;
    }
    
    /**
     * 判断是否为等待状态
     */
    public boolean isPending() {
        return this == PENDING;
    }
    
    /**
     * 判断是否为重试相关状态
     */
    public boolean isRetryRelated() {
        return this == RETRYING || this == RETRY_FAILED || this == RETRY_EXHAUSTED;
    }
    
    /**
     * 判断是否需要人工干预
     */
    public boolean requiresManualIntervention() {
        return this == RETRY_EXHAUSTED || this == INVALID || this == TIMEOUT;
    }
    
    /**
     * 判断是否可以重新处理
     */
    public boolean canReprocess() {
        return this == FAILED || this == RETRY_FAILED || this == TIMEOUT || this == CANCELLED;
    }
    
    /**
     * 判断是否可以取消
     */
    public boolean canCancel() {
        return this == PENDING || this == PROCESSING || this == RETRYING;
    }
    
    /**
     * 判断是否可以归档
     */
    public boolean canArchive() {
        return isFinal && this != ARCHIVED;
    }
    
    /**
     * 获取状态优先级（用于排序）
     */
    public int getPriority() {
        switch (this) {
            case PROCESSING:
            case RETRYING:
                return 1; // 最高优先级
            case PENDING:
                return 2;
            case FAILED:
            case RETRY_FAILED:
            case TIMEOUT:
                return 3;
            case RETRY_EXHAUSTED:
                return 4;
            case SUCCESS:
                return 5;
            case SKIPPED:
            case DUPLICATE:
                return 6;
            case CANCELLED:
            case INVALID:
                return 7;
            case ARCHIVED:
                return 8; // 最低优先级
            default:
                return 9;
        }
    }
    
    /**
     * 获取下一个可能的状态
     */
    public ProcessingStatus[] getNextPossibleStatuses() {
        switch (this) {
            case PENDING:
                return new ProcessingStatus[]{PROCESSING, CANCELLED, INVALID};
            case PROCESSING:
                return new ProcessingStatus[]{SUCCESS, FAILED, TIMEOUT, CANCELLED};
            case FAILED:
                return new ProcessingStatus[]{RETRYING, RETRY_EXHAUSTED, CANCELLED, ARCHIVED};
            case RETRYING:
                return new ProcessingStatus[]{SUCCESS, RETRY_FAILED, TIMEOUT, CANCELLED};
            case RETRY_FAILED:
                return new ProcessingStatus[]{RETRYING, RETRY_EXHAUSTED, CANCELLED, ARCHIVED};
            case SUCCESS:
            case RETRY_EXHAUSTED:
            case SKIPPED:
            case CANCELLED:
            case TIMEOUT:
            case INVALID:
            case DUPLICATE:
                return new ProcessingStatus[]{ARCHIVED};
            case ARCHIVED:
                return new ProcessingStatus[]{}; // 归档状态无法转换
            default:
                return new ProcessingStatus[]{};
        }
    }
    
    /**
     * 检查状态转换是否有效
     */
    public boolean canTransitionTo(ProcessingStatus targetStatus) {
        ProcessingStatus[] possibleStatuses = getNextPossibleStatuses();
        for (ProcessingStatus status : possibleStatuses) {
            if (status == targetStatus) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * 根据名称查找处理状态
     */
    public static ProcessingStatus findByName(String name) {
        if (name == null || name.trim().isEmpty()) {
            return null;
        }
        
        try {
            return ProcessingStatus.valueOf(name.toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
    
    /**
     * 根据显示名称查找处理状态
     */
    public static ProcessingStatus findByDisplayName(String displayName) {
        if (displayName == null || displayName.trim().isEmpty()) {
            return null;
        }
        
        for (ProcessingStatus status : ProcessingStatus.values()) {
            if (status.getDisplayName().equals(displayName)) {
                return status;
            }
        }
        return null;
    }
    
    /**
     * 获取所有成功状态
     */
    public static ProcessingStatus[] getSuccessStatuses() {
        return new ProcessingStatus[]{SUCCESS};
    }
    
    /**
     * 获取所有失败状态
     */
    public static ProcessingStatus[] getFailureStatuses() {
        return new ProcessingStatus[]{FAILED, RETRY_FAILED, RETRY_EXHAUSTED, TIMEOUT, INVALID};
    }
    
    /**
     * 获取所有处理中状态
     */
    public static ProcessingStatus[] getProcessingStatuses() {
        return new ProcessingStatus[]{PROCESSING, RETRYING};
    }
    
    /**
     * 获取所有等待状态
     */
    public static ProcessingStatus[] getPendingStatuses() {
        return new ProcessingStatus[]{PENDING};
    }
    
    /**
     * 获取所有最终状态
     */
    public static ProcessingStatus[] getFinalStatuses() {
        return java.util.Arrays.stream(ProcessingStatus.values())
                .filter(ProcessingStatus::isFinal)
                .toArray(ProcessingStatus[]::new);
    }
    
    /**
     * 获取所有可重试状态
     */
    public static ProcessingStatus[] getRetryableStatuses() {
        return java.util.Arrays.stream(ProcessingStatus.values())
                .filter(ProcessingStatus::canRetry)
                .toArray(ProcessingStatus[]::new);
    }
    
    /**
     * 获取所有需要人工干预的状态
     */
    public static ProcessingStatus[] getManualInterventionStatuses() {
        return java.util.Arrays.stream(ProcessingStatus.values())
                .filter(ProcessingStatus::requiresManualIntervention)
                .toArray(ProcessingStatus[]::new);
    }
}