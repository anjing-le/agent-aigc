package com.anjing.aigc.model.response;

import com.anjing.aigc.model.enums.TaskStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 生成响应
 *
 * @author AIGC Team
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GenerateResponse {

    /** 任务ID */
    private String taskId;

    /** 任务状态 */
    private TaskStatus status;

    /** Agent分析结果 */
    private AgentAnalysis agentAnalysis;

    /** 预估完成时间（秒） */
    private Integer estimatedTime;
}

