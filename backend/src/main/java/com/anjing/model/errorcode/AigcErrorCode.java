package com.anjing.model.errorcode;

/**
 * AIGC 创作平台错误码。
 */
public enum AigcErrorCode implements ErrorCode {

    TASK_NOT_FOUND("2400", "任务不存在"),
    ASSET_NOT_FOUND("2401", "资产不存在"),
    CONTENT_TYPE_UNSUPPORTED("2402", "不支持的内容类型"),
    MATERIAL_EMPTY("2403", "请上传素材文件"),
    MATERIAL_TYPE_UNSUPPORTED("2404", "仅支持图片或视频素材"),
    MATERIAL_SIZE_EXCEEDED("2405", "素材文件过大"),
    MATERIAL_SAVE_FAILED("2406", "素材保存失败");

    private final String code;
    private final String message;

    AigcErrorCode(String code, String message) {
        this.code = code;
        this.message = message;
    }

    @Override
    public String getCode() {
        return code;
    }

    @Override
    public String getMessage() {
        return message;
    }
}
