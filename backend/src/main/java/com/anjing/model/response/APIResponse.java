package com.anjing.model.response;

import com.anjing.context.GlobalRequestContextHolder;
import com.anjing.model.constants.PlatformContractConstants;
import com.anjing.model.errorcode.ErrorCode;
import com.anjing.util.DateUtils;
import lombok.Data;

/**
 * 统一API响应结果
 */
@Data
public class APIResponse<T>
{

    /**
     * 成功状态码
     */
    public static final String SUCCESS_CODE = PlatformContractConstants.Response.SUCCESS_CODE;

    /**
     * 默认成功消息
     */
    public static final String SUCCESS_MESSAGE = "操作成功";

    /**
     * 失败状态码
     */
    public static final String ERROR_CODE = "-1";

    /**
     * 响应码
     */
    private String code;

    /**
     * 响应消息
     */
    private String message;

    /**
     * 响应数据
     */
    private T data;

    /**
     * 时间戳
     */
    private Long timestamp;

    /**
     * Request id for frontend and log correlation.
     */
    private String requestId;

    public APIResponse() {
        this.timestamp = DateUtils.nowEpochMilli();
        this.requestId = GlobalRequestContextHolder.requestIdOrNull();
    }

    public APIResponse(String code, String message, T data) {
        this.code = code;
        this.message = message;
        this.data = data;
        this.timestamp = DateUtils.nowEpochMilli();
        this.requestId = GlobalRequestContextHolder.requestIdOrNull();
    }

    public APIResponse(String code, String message) {
        this.code = code;
        this.message = message;
        this.timestamp = DateUtils.nowEpochMilli();
        this.requestId = GlobalRequestContextHolder.requestIdOrNull();
    }

    /**
     * 判断是否成功
     * 
     * @return 是否成功
     */
    public boolean isSuccess() {
        return SUCCESS_CODE.equals(code);
    }

    /**
     * 成功响应
     * 
     * @param data 数据
     * @param <T>  数据类型
     * @return 响应结果
     */
    public static <T> APIResponse<T> success(T data) {
        return new APIResponse<>(SUCCESS_CODE, SUCCESS_MESSAGE, data);
    }

    /**
     * 成功响应，明确表示参数是数据。用于 String 等容易和消息重载混淆的场景。
     *
     * @param data 数据
     * @param <T>  数据类型
     * @return 响应结果
     */
    public static <T> APIResponse<T> successData(T data) {
        return success(data);
    }

    /**
     * 成功响应
     * 
     * @param data    数据
     * @param message 消息
     * @param <T>     数据类型
     * @return 响应结果
     */
    public static <T> APIResponse<T> success(T data, String message) {
        return new APIResponse<>(SUCCESS_CODE, message, data);
    }

    /**
     * 成功响应（无数据）
     * 
     * @return 响应结果
     */
    public static <T> APIResponse<T> success() {
        return new APIResponse<>(SUCCESS_CODE, SUCCESS_MESSAGE, null);
    }

    /**
     * 成功响应（无数据）
     * 
     * @param message 消息
     * @return 响应结果
     */
    public static <T> APIResponse<T> successMessage(String message) {
        return new APIResponse<>(SUCCESS_CODE, message, null);
    }

    /**
     * 成功响应（无数据）
     *
     * @param message 消息
     * @return 响应结果
     * @deprecated 使用 {@link #successMessage(String)}，避免和 {@link #success(Object)} 的 String 数据场景混淆。
     */
    @Deprecated(since = "1.1.0", forRemoval = false)
    public static <T> APIResponse<T> success(String message) {
        return successMessage(message);
    }

    /**
     * 错误响应
     * 
     * @param message 错误消息
     * @param <T>     数据类型
     * @return 响应结果
     */
    public static <T> APIResponse<T> error(String message) {
        return new APIResponse<>(ERROR_CODE, message);
    }

    /**
     * 错误响应
     *
     * @param errorCode 错误码
     * @param <T>       数据类型
     * @return 响应结果
     */
    public static <T> APIResponse<T> error(ErrorCode errorCode) {
        return new APIResponse<>(errorCode.getCode(), errorCode.getMessage());
    }

    /**
     * 错误响应
     *
     * @param errorCode 错误码
     * @param data      附加数据
     * @param <T>       数据类型
     * @return 响应结果
     */
    public static <T> APIResponse<T> error(ErrorCode errorCode, T data) {
        return new APIResponse<>(errorCode.getCode(), errorCode.getMessage(), data);
    }

    /**
     * 错误响应
     * 
     * @param code    错误码
     * @param message 错误消息
     * @param <T>     数据类型
     * @return 响应结果
     */
    public static <T> APIResponse<T> error(String code, String message) {
        return new APIResponse<>(code, message);
    }

    /**
     * 错误响应
     * 
     * @param code    错误码
     * @param message 错误消息
     * @param data    数据
     * @param <T>     数据类型
     * @return 响应结果
     */
    public static <T> APIResponse<T> error(String code, String message, T data) {
        return new APIResponse<>(code, message, data);
    }
}
