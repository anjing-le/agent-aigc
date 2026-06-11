package com.anjing.aspect;

import com.anjing.annotation.Facade;
import com.anjing.model.exception.BizException;
import com.anjing.model.exception.SystemException;
import com.anjing.model.errorcode.CommonErrorCode;
import com.anjing.model.response.APIResponse;
import com.anjing.util.BeanValidator;
import com.anjing.util.JsonUtils;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.util.StopWatch;

import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

/**
 * 🎯 Facade统一校验切面
 * 
 * <p>为标记@Facade注解的方法提供统一的参数校验、日志记录和异常处理</p>
 * 
 * <h3>🚀 核心功能：</h3>
 * <ul>
 *   <li>🔍 <b>自动参数校验</b> - 遍历所有方法参数，执行JSR-303校验</li>
 *   <li>📋 <b>详细日志记录</b> - 记录方法调用、参数、执行时间、异常信息</li>
 *   <li>🛡️ <b>异常统一处理</b> - 校验失败自动转换为统一的响应格式</li>
 *   <li>⏱️ <b>性能监控</b> - 自动记录方法执行时间</li>
 *   <li>🎛️ <b>灵活配置</b> - 支持开关控制校验和日志功能</li>
 * </ul>
 * 
 * <h3>📋 适用场景：</h3>
 * <ul>
 *   <li>🌐 <b>RPC服务</b> - Dubbo、gRPC等RPC服务的统一处理</li>
 *   <li>🔧 <b>Service层</b> - 业务服务方法的参数校验和监控</li>
 *   <li>📊 <b>内部API</b> - 模块间调用的统一处理</li>
 *   <li>🎯 <b>复杂业务</b> - 需要校验+日志+监控的综合场景</li>
 * </ul>
 * 
 * <h3>⚡ 执行顺序说明：</h3>
 * <p>使用@Order(100)确保在分布式锁等关键切面之后执行，避免冲突</p>
 * <pre>
 * 执行顺序：
 * 1. 🔒 DistributeLockAspect (@Order(Integer.MIN_VALUE))
 * 2. 🔄 TransactionAspect (默认顺序)
 * 3. 🎯 FacadeAspect (@Order(100)) ← 当前切面
 * 4. 📋 业务方法
 * </pre>
 * 
 * <h3>🎯 处理流程：</h3>
 * <pre>
 * 1. 📋 解析@Facade注解配置
 * 2. 🔍 执行参数校验（如果启用）
 * 3. 📝 记录方法调用日志（如果启用）
 * 4. ⏱️ 开始性能计时
 * 5. 🚀 执行目标方法
 * 6. 📊 记录执行结果和耗时
 * 7. 🛡️ 统一异常处理和响应格式转换
 * </pre>
 * 
 * <h3>💡 最佳实践：</h3>
 * <ul>
 *   <li>🎯 <b>合理使用</b> - 仅在RPC服务、Service层等需要统一处理的场景使用</li>
 *   <li>🚀 <b>性能考虑</b> - 避免在高频简单方法上使用，注意AOP开销</li>
 *   <li>📝 <b>参数对象</b> - 确保参数对象正确使用JSR-303校验注解</li>
 *   <li>🔧 <b>返回类型</b> - 支持自动识别返回类型并构造失败响应</li>
 * </ul>
 * 
 * @author Backend Template Team
 * @version 1.0
 * @see com.anjing.annotation.Facade
 * @see com.anjing.util.BeanValidator
 * @since 1.0.0
 */
@Aspect
@Component
@Order(100)  // 确保在分布式锁等关键切面之后执行
@Slf4j
public class FacadeAspect {

    /**
     * 🎯 Facade方法统一处理
     * 
     * <p>拦截所有标记@Facade注解的方法，提供统一的参数校验、日志记录和异常处理</p>
     * 
     * @param pjp 连接点
     * @return 方法执行结果
     * @throws Exception 方法执行异常
     */
    @Around("@annotation(com.anjing.annotation.Facade)")
    public Object process(ProceedingJoinPoint pjp) throws Exception {
        StopWatch stopWatch = new StopWatch();
        stopWatch.start();

        Method method = ((MethodSignature) pjp.getSignature()).getMethod();
        Facade facade = method.getAnnotation(Facade.class);
        Object[] args = pjp.getArgs();
        
        String methodName = method.getDeclaringClass().getSimpleName() + "." + method.getName();
        String scene = facade.scene().isEmpty() ? methodName : facade.scene();

        try {
            // 1. 记录方法调用开始日志
            if (facade.enableLogging()) {
                logMethodStart(methodName, scene, args);
            }

            // 2. 执行参数校验
            if (facade.enableValidation()) {
                validateParameters(args, facade.validationGroups(), methodName);
            }

            // 3. 执行目标方法
            Object result = pjp.proceed();

            // 4. 记录成功日志
            stopWatch.stop();
            if (facade.enableLogging()) {
                logMethodSuccess(methodName, scene, args, result, stopWatch.getTotalTimeMillis());
            }

            return result;

        } catch (BizException e) {
            // 5. 处理业务异常
            stopWatch.stop();
            logMethodError(methodName, scene, args, e, stopWatch.getTotalTimeMillis());
            return handleBizException(method, e);

        } catch (SystemException e) {
            // 6. 处理系统异常
            stopWatch.stop();
            logMethodError(methodName, scene, args, e, stopWatch.getTotalTimeMillis());
            return handleSystemException(method, e);

        } catch (Throwable e) {
            // 7. 处理其他异常
            stopWatch.stop();
            logMethodError(methodName, scene, args, e, stopWatch.getTotalTimeMillis());
            return handleUnknownException(method, e);
        }
    }

    /**
     * 🔍 执行参数校验
     * 
     * @param args 方法参数数组
     * @param groups 校验分组
     * @param methodName 方法名（用于日志）
     */
    private void validateParameters(Object[] args, Class<?>[] groups, String methodName) {
        if (args == null || args.length == 0) {
            return;
        }

        for (int i = 0; i < args.length; i++) {
            Object parameter = args[i];
            if (parameter == null) {
                continue;
            }

            // 跳过基本类型和字符串
            if (isSimpleType(parameter.getClass())) {
                continue;
            }

            try {
                if (groups.length > 0) {
                    BeanValidator.validateObject(parameter, groups);
                } else {
                    BeanValidator.validateObject(parameter);
                }
            } catch (BizException e) {
                log.warn("方法 {} 第{}个参数校验失败: {}", methodName, i + 1, e.getMessage());
                throw new BizException(String.format("第%d个参数校验失败: %s", i + 1, e.getMessage()), CommonErrorCode.PARAM_INVALID);
            }
        }
    }

    /**
     * 判断是否为简单类型（不需要校验的类型）
     */
    private boolean isSimpleType(Class<?> type) {
        return type.isPrimitive() ||
               type == String.class ||
               type == Integer.class ||
               type == Long.class ||
               type == Double.class ||
               type == Float.class ||
               type == Boolean.class ||
               type == Byte.class ||
               type == Short.class ||
               type == Character.class ||
               Number.class.isAssignableFrom(type);
    }

    /**
     * 📝 记录方法调用开始日志
     */
    private void logMethodStart(String methodName, String scene, Object[] args) {
        try {
            String argsJson = JsonUtils.toJson(args);
            log.info("🚀 [Facade] 开始执行方法: {} | 场景: {} | 参数: {}", methodName, scene, argsJson);
        } catch (Exception e) {
            log.info("🚀 [Facade] 开始执行方法: {} | 场景: {} | 参数: [序列化失败: {}]", methodName, scene, e.getMessage());
        }
    }

    /**
     * ✅ 记录方法执行成功日志
     */
    private void logMethodSuccess(String methodName, String scene, Object[] args, Object result, long timeMillis) {
        try {
            String resultJson = JsonUtils.toJson(result);
            log.info("✅ [Facade] 方法执行成功: {} | 场景: {} | 耗时: {}ms | 结果: {}", 
                    methodName, scene, timeMillis, truncateString(resultJson, 500));
        } catch (Exception e) {
            log.info("✅ [Facade] 方法执行成功: {} | 场景: {} | 耗时: {}ms | 结果: [序列化失败: {}]", 
                    methodName, scene, timeMillis, e.getMessage());
        }
    }

    /**
     * ❌ 记录方法执行失败日志
     */
    private void logMethodError(String methodName, String scene, Object[] args, Throwable e, long timeMillis) {
        try {
            String argsJson = JsonUtils.toJson(args);
            log.error("❌ [Facade] 方法执行失败: {} | 场景: {} | 耗时: {}ms | 参数: {} | 异常: {}", 
                    methodName, scene, timeMillis, argsJson, e.getMessage());
        } catch (Exception ex) {
            log.error("❌ [Facade] 方法执行失败: {} | 场景: {} | 耗时: {}ms | 参数: [序列化失败] | 异常: {}", 
                    methodName, scene, timeMillis, e.getMessage());
        }
    }

    /**
     * 🛡️ 处理业务异常
     */
    private Object handleBizException(Method method, BizException e) {
        Class<?> returnType = method.getReturnType();
        
        // 如果返回类型是APIResponse，直接返回错误响应
        if (APIResponse.class.isAssignableFrom(returnType)) {
            return APIResponse.error(e.getErrorCode());
        }
        
        // 如果返回类型是泛型APIResponse，尝试构造
        if (isGenericAPIResponse(method)) {
            return APIResponse.error(e.getErrorCode());
        }
        
        // 其他情况重新抛出异常，让GlobalExceptionHandler处理
        throw e;
    }

    /**
     * ⚠️ 处理系统异常
     */
    private Object handleSystemException(Method method, SystemException e) {
        Class<?> returnType = method.getReturnType();
        
        if (APIResponse.class.isAssignableFrom(returnType) || isGenericAPIResponse(method)) {
            return APIResponse.error(e.getErrorCode());
        }
        
        throw e;
    }

    /**
     * 🚨 处理未知异常
     */
    private Object handleUnknownException(Method method, Throwable e) throws Exception {
        Class<?> returnType = method.getReturnType();
        
        if (APIResponse.class.isAssignableFrom(returnType) || isGenericAPIResponse(method)) {
            return APIResponse.error(CommonErrorCode.SYSTEM_ERROR);
        }
        
        // 包装为Exception重新抛出
        if (e instanceof Exception) {
            throw (Exception) e;
        } else {
            throw new Exception(e);
        }
    }
    
    /**
     * 判断方法返回类型是否为泛型APIResponse
     */
    private boolean isGenericAPIResponse(Method method) {
        Type genericReturnType = method.getGenericReturnType();
        if (genericReturnType instanceof ParameterizedType) {
            ParameterizedType parameterizedType = (ParameterizedType) genericReturnType;
            Type rawType = parameterizedType.getRawType();
            return APIResponse.class.equals(rawType);
        }
        return false;
    }

    /**
     * 截断字符串（避免日志过长）
     */
    private String truncateString(String str, int maxLength) {
        if (str == null || str.length() <= maxLength) {
            return str;
        }
        return str.substring(0, maxLength) + "...";
    }
}
