package com.anjing.aigc.model.response;

import com.anjing.aigc.model.enums.TaskStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 任务状态响应
 *
 * @author AIGC Team
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TaskStatusResponse {

    /** 任务ID */
    private String taskId;

    /** 任务状态 */
    private TaskStatus status;

    /** 进度百分比 0-100 */
    private Integer progress;

    /** 生成结果（完成时返回） */
    private GenerationResult result;

    /** 错误信息（失败时返回） */
    private String errorMessage;

    /** 创建时间 */
    private LocalDateTime createdAt;

    /** 更新时间 */
    private LocalDateTime updatedAt;
}

