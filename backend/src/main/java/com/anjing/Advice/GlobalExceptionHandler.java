package com.anjing.Advice;

import com.anjing.model.errorcode.CommonErrorCode;
import com.anjing.model.exception.BizException;
import com.anjing.model.exception.SystemException;
import com.anjing.model.response.APIResponse;
import com.anjing.util.ExceptionUtils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.BindException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;

/**
 * 全局异常处理
 * 
 * <p>按照标准异常体系统一处理各类异常</p>
 * 
 * <h3>🎯 异常分类处理：</h3>
 * <ul>
 *   <li>🔧 业务异常 - BizException</li>
 *   <li>⚠️ 系统异常 - SystemException</li>
 *   <li>✅ 参数校验异常 - Validation相关</li>
 *   <li>🚨 其他异常 - 统一处理</li>
 * </ul>
 * 
 * @author Backend Template Team
 * @version 1.0
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler
{

    /**
     * 处理业务异常
     */
    @ExceptionHandler(BizException.class)
    public APIResponse<Object> handleBizException(BizException e, HttpServletRequest request)
    {
        ExceptionUtils.printException(e, request);
        return APIResponse.error(e.getErrorCode());
    }

    /**
     * 处理系统异常
     */
    @ExceptionHandler(SystemException.class)
    public APIResponse<Object> handleSystemException(SystemException e, HttpServletRequest request) {
        ExceptionUtils.printException(e, request);
        return APIResponse.error(e.getErrorCode());
    }

    /**
     * 处理参数校验异常 - @RequestBody 参数校验
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public APIResponse<Object> handleMethodArgumentNotValidException(MethodArgumentNotValidException e, HttpServletRequest request) {
        ExceptionUtils.printException(e, request);
        String errorMessage = e.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .reduce((msg1, msg2) -> msg1 + "; " + msg2)
                .orElse("参数校验失败");
        return APIResponse.error(CommonErrorCode.PARAMETER_ERROR.getCode(), errorMessage);
    }

    /**
     * 处理参数校验异常 - @ModelAttribute 参数校验
     */
    @ExceptionHandler(BindException.class)
    public APIResponse<Object> handleBindException(BindException e, HttpServletRequest request)
    {
        ExceptionUtils.printException(e, request);
        String errorMessage = e.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .reduce((msg1, msg2) -> msg1 + "; " + msg2)
                .orElse("参数绑定失败");
        return APIResponse.error(CommonErrorCode.PARAM_MISSING.getCode(), errorMessage);
    }

    /**
     * 处理参数校验异常 - @RequestParam 和 @PathVariable 参数校验
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public APIResponse<Object> handleConstraintViolationException(ConstraintViolationException e, HttpServletRequest request) {
        ExceptionUtils.printException(e, request);
        String errorMessage = e.getConstraintViolations().stream()
                .map(violation -> violation.getPropertyPath() + ": " + violation.getMessage())
                .reduce((msg1, msg2) -> msg1 + "; " + msg2)
                .orElse("约束校验失败");
        return APIResponse.error(CommonErrorCode.PARAM_FORMAT_ERROR.getCode(), errorMessage);
    }

    /**
     * 处理404异常
     */
    @ExceptionHandler(NoResourceFoundException.class)
    public APIResponse<Object> handleNoResourceFound(NoResourceFoundException ex, HttpServletRequest request)
    {
        String requestURI = request.getRequestURI();
        
        // 忽略favicon.ico的404异常，不记录日志
        if (requestURI != null && requestURI.endsWith("/favicon.ico")) {
            return APIResponse.error(CommonErrorCode.RESOURCE_NOT_FOUND);
        }
        
        log.warn("资源未找到: {}", requestURI);
        return APIResponse.error(CommonErrorCode.RESOURCE_NOT_FOUND.getCode(), "请求的资源不存在: " + requestURI);
    }

    /**
     * 处理其他所有异常
     */
    @ExceptionHandler(Exception.class)
    public APIResponse<Object> handleException(Exception e, HttpServletRequest request)
    {
        ExceptionUtils.printException(e, request);
        return APIResponse.error(CommonErrorCode.SYSTEM_ERROR.getCode(), "系统内部错误，请稍后重试");
    }
}
