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
    MATERIAL_SAVE_FAILED("2406", "素材保存失败"),
    MATERIAL_NOT_FOUND("2407", "素材不存在"),
    GENERATION_PARAM_INVALID("2408", "生成参数不合法"),
    PROVIDER_UNAVAILABLE("2409", "模型 Provider 暂不可用"),
    PROVIDER_CALL_FAILED("2410", "模型 Provider 调用失败"),
    MATERIAL_USAGE_UNSUPPORTED("2411", "素材不适用于当前创作类型"),
    STORAGE_FILE_NOT_FOUND("2412", "文件不存在或不可访问"),
    OWNERSHIP_BACKFILL_INVALID("2413", "归属回填请求不合法");

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
