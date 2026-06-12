package com.anjing.aigc.model.response;

import com.anjing.aigc.model.dto.AssetDTO;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AssetDetailResponse {

    private AssetDTO asset;

    private TaskStatusResponse task;
}
