package com.anjing.aigc.model.response;

import com.anjing.aigc.model.dto.ModelInfo;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 模型列表响应
 *
 * @author AIGC Team
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ModelListResponse {

    /** 图片生成模型 */
    private List<ModelInfo> imageModels;

    /** 视频生成模型 */
    private List<ModelInfo> videoModels;

    /** 音频生成模型（预留） */
    private List<ModelInfo> audioModels;
}

