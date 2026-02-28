package com.anjing.model.response;

import lombok.Data;

/**
 * 统一API响应结果
 * 
 * <p>前后端约定响应格式：</p>
 * <ul>
 *   <li>code: 200 表示成功，其他表示失败</li>
 *   <li>message: 响应消息</li>
 *   <li>data: 响应数据</li>
 *   <li>timestamp: 时间戳</li>
 * </ul>
 */
@Data
public class APIResponse<T>
{

    /**
     * 成功状态码
     */
    public static final int SUCCESS_CODE = 200;

    /**
     * 通用错误状态码
     */
    public static final int ERROR_CODE = 400;
    
    /**
     * 未授权状态码
     */
    public static final int UNAUTHORIZED_CODE = 401;
    
    /**
     * 服务器错误状态码
     */
    public static final int SERVER_ERROR_CODE = 500;

    /**
     * 响应码
     */
    private int code;

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

    public APIResponse() {
        this.timestamp = System.currentTimeMillis();
    }

    public APIResponse(int code, String message, T data) {
        this.code = code;
        this.message = message;
        this.data = data;
        this.timestamp = System.currentTimeMillis();
    }

    public APIResponse(int code, String message) {
        this.code = code;
        this.message = message;
        this.timestamp = System.currentTimeMillis();
    }

    /**
     * 判断是否成功
     * 
     * @return 是否成功
     */
    public boolean isSuccess() {
        return SUCCESS_CODE == code;
    }

    /**
     * 成功响应
     * 
     * @param data 数据
     * @param <T>  数据类型
     * @return 响应结果
     */
    public static <T> APIResponse<T> success(T data) {
        return new APIResponse<>(SUCCESS_CODE, "操作成功", data);
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
        return new APIResponse<>(SUCCESS_CODE, "操作成功", null);
    }

    /**
     * 成功响应（无数据）
     * 
     * @param message 消息
     * @return 响应结果
     */
    public static <T> APIResponse<T> success(String message) {
        return new APIResponse<>(SUCCESS_CODE, message, null);
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
     * @param code    错误码
     * @param message 错误消息
     * @param <T>     数据类型
     * @return 响应结果
     */
    public static <T> APIResponse<T> error(int code, String message) {
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
    public static <T> APIResponse<T> error(int code, String message, T data) {
        return new APIResponse<>(code, message, data);
    }
    
    /**
     * 未授权响应
     * 
     * @param message 错误消息
     * @param <T>     数据类型
     * @return 响应结果
     */
    public static <T> APIResponse<T> unauthorized(String message) {
        return new APIResponse<>(UNAUTHORIZED_CODE, message);
    }
    
    /**
     * 服务器错误响应
     * 
     * @param message 错误消息
     * @param <T>     数据类型
     * @return 响应结果
     */
    public static <T> APIResponse<T> serverError(String message) {
        return new APIResponse<>(SERVER_ERROR_CODE, message);
    }
}
