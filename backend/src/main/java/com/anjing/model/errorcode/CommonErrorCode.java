package com.anjing.model.errorcode;

/**
 * 通用错误码枚举
 * 
 * <p>定义系统级通用错误码，按照标准分类</p>
 * 
 * <h3>🎯 错误码规范：</h3>
 * <ul>
 *   <li>成功: 0</li>
 *   <li>系统错误: 1xxx</li>
 *   <li>业务错误: 2xxx</li>
 *   <li>参数错误: 3xxx</li>
 *   <li>权限错误: 4xxx</li>
 * </ul>
 * 
 * @author Backend Template Team
 * @version 1.0
 */
public enum CommonErrorCode implements ErrorCode {

    // 系统错误 1xxx
    SYSTEM_ERROR("1000", "系统内部错误"),
    SYSTEM_TIMEOUT("1001", "系统超时"),
    DATABASE_ERROR("1002", "数据库异常"),
    NETWORK_ERROR("1003", "网络异常"),
    SERVICE_UNAVAILABLE("1004", "服务不可用"),
    RESOURCE_NOT_FOUND("1005", "请求资源不存在"),

    // 业务通用 2xxx
    DATA_NOT_FOUND("2000", "数据不存在"),
    DATA_ALREADY_EXISTS("2001", "数据已存在"),
    OPERATION_NOT_ALLOWED("2002", "操作不允许"),

    // 参数错误 3xxx
    PARAMETER_ERROR("3000", "参数错误"),
    PARAM_INVALID("3010", "参数无效"),
    PARAM_MISSING("3001", "缺少必要参数"),
    PARAM_FORMAT_ERROR("3002", "参数格式错误"),
    PARAM_OUT_OF_RANGE("3003", "参数超出范围"),

    // 权限错误 4xxx
    UNAUTHORIZED("4000", "未授权访问"),
    FORBIDDEN("4001", "权限不足"),
    TOKEN_INVALID("4002", "Token无效"),
    TOKEN_EXPIRED("4003", "Token已过期"),
    LOGIN_REQUIRED("4004", "需要登录");

    private final String code;
    private final String message;

    CommonErrorCode(String code, String message) {
        this.code = code;
        this.message = message;
    }

    @Override
    public String getCode() {
        return this.code;
    }

    @Override
    public String getMessage() {
        return this.message;
    }
}
