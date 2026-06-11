package com.anjing.example;

import com.anjing.annotation.ScaffoldSample;
import com.anjing.client.RemoteHttpClient;
import com.anjing.client.RemoteHttpRequest;
import com.anjing.model.constants.ApiConstants;
import com.anjing.model.constants.ServiceBoundaryConstants;
import com.anjing.model.request.BaseRequest;
import com.anjing.model.response.APIResponse;
import com.anjing.util.RemoteCallWrapper;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * 🌐 远程调用包装工具使用示例
 * 
 * <p>演示如何使用{@link RemoteCallWrapper}进行各种远程调用场景</p>
 * 
 * <h3>🎯 示例场景：</h3>
 * <ul>
 *   <li><b>Dubbo RPC调用</b> - 微服务间的RPC调用示例</li>
 *   <li><b>HTTP接口调用</b> - 第三方API调用示例</li>
 *   <li><b>重试机制</b> - 失败重试的使用示例</li>
 *   <li><b>无参数调用</b> - 配置查询等无参数调用示例</li>
 * </ul>
 * 
 * @author Backend Template Team
 * @version 1.0
 * @since 1.0.0
 */
@ScaffoldSample("远程调用包装工具示例")
@Service
@RequiredArgsConstructor
@Slf4j
public class RemoteCallExampleService {

    private final RemoteHttpClient remoteHttpClient;

    // ==================== 模拟的远程服务接口 ====================
    
    /**
     * 模拟订单服务接口（实际项目中这会是Dubbo接口或HTTP客户端）
     */
    private final MockOrderService orderService = new MockOrderService();
    
    /**
     * 模拟用户服务接口
     */
    private final MockUserService userService = new MockUserService();
    
    /**
     * 模拟配置服务接口
     */
    private final MockConfigService configService = new MockConfigService();

    // ==================== 使用示例方法 ====================

    /**
     * 示例1: 基础远程调用 - 最简单的用法
     * 场景：创建订单
     */
    public APIResponse<OrderVO> createOrderExample() {
        log.info("=== 示例1: 基础远程调用 ===");
        
        OrderCreateRequest request = new OrderCreateRequest();
        request.setUserId(12345L);
        request.setProductId(67890L);
        request.setQuantity(2);
        request.setAmount(199.99);
        
        // 🚀 最简单的远程调用
        OrderResponse response = RemoteCallWrapper.call(
            req -> orderService.createOrder(req), 
            request
        );
        
        return APIResponse.success(convertToVO(response), "订单创建成功");
    }

    /**
     * 示例2: 指定方法名称的远程调用
     * 场景：查询用户信息
     */
    public APIResponse<UserVO> getUserInfoExample() {
        log.info("=== 示例2: 指定方法名称的远程调用 ===");
        
        UserQueryRequest request = new UserQueryRequest();
        request.setUserId(12345L);
        
        // 🎯 指定调用名称，便于日志追踪
        UserResponse response = RemoteCallWrapper.call(
            req -> userService.getUserInfo(req), 
            request,
            "getUserInfo"
        );
        
        return APIResponse.success(convertToVO(response), "用户信息查询成功");
    }

    /**
     * 示例3: 带重试机制的远程调用
     * 场景：支付处理（网络不稳定，需要重试）
     */
    public APIResponse<PaymentVO> processPaymentExample() {
        log.info("=== 示例3: 带重试机制的远程调用 ===");
        
        PaymentRequest request = new PaymentRequest();
        request.setOrderId(12345L);
        request.setAmount(199.99);
        request.setPaymentMethod("ALIPAY");
        
        // 🔄 带重试的远程调用：重试3次，间隔1秒
        PaymentResponse response = RemoteCallWrapper.callWithRetry(
            req -> orderService.processPayment(req),
            request,
            "processPayment",
            3,    // 重试3次
            true, // 检查响应状态
            1000  // 重试间隔1秒
        );
        
        return APIResponse.success(convertToVO(response), "支付处理成功");
    }

    /**
     * 示例4: 无参数远程调用
     * 场景：获取系统配置
     */
    public APIResponse<ConfigVO> getSystemConfigExample() {
        log.info("=== 示例4: 无参数远程调用 ===");
        
        // 📞 无参数调用
        ConfigResponse response = RemoteCallWrapper.callNoParam(
            () -> configService.getSystemConfig(),
            "getSystemConfig"
        );
        
        return APIResponse.success(convertToVO(response), "系统配置获取成功");
    }

    /**
     * 示例5: 不校验响应的远程调用
     * 场景：发送通知（成功失败都不影响主流程）
     */
    public APIResponse<String> sendNotificationExample() {
        log.info("=== 示例5: 不校验响应的远程调用 ===");
        
        NotificationRequest request = new NotificationRequest();
        request.setUserId(12345L);
        request.setMessage("您的订单已创建成功");
        request.setType("ORDER_CREATED");
        
        // 🔍 不校验响应状态（适合非关键调用）
        NotificationResponse response = RemoteCallWrapper.call(
            req -> userService.sendNotification(req), 
            request,
            "sendNotification",
            false  // 不校验响应状态
        );
        
        return APIResponse.successData("通知发送完成");
    }

    /**
     * 示例6: 服务间调用上下文请求头
     * 场景：HTTP/WebClient/Feign 调用下游服务前，统一透传链路、租户、用户、语言和时区
     */
    public APIResponse<Map<String, String>> serviceCallHeadersExample() {
        log.info("=== 示例6: 服务间调用上下文请求头 ===");

        Map<String, String> headers = RemoteCallWrapper.serviceCallHeaders(ServiceBoundaryConstants.APPLICATION_ID);
        return APIResponse.success(headers, "服务调用请求头生成成功");
    }

    /**
     * 示例7: 统一 HTTP 远程调用适配器
     * 场景：用统一 adapter 调用下游 HTTP 服务，并自动透传上下文请求头
     */
    public APIResponse<String> remoteHttpClientExample() {
        log.info("=== 示例7: 统一 HTTP 远程调用适配器 ===");

        RemoteHttpRequest request = RemoteHttpRequest.builder()
                .method(HttpMethod.GET)
                .serviceId(ServiceBoundaryConstants.APPLICATION_ID)
                .path(ApiConstants.Test.PING_FULL)
                .callerId(ServiceBoundaryConstants.APPLICATION_ID)
                .checkResponse(true)
                .build();

        return remoteHttpClient.exchange(request, new ParameterizedTypeReference<APIResponse<String>>() {
        });
    }

    /**
     * 示例8: 复杂业务场景 - 组合多个远程调用
     * 场景：下单流程（用户验证 -> 库存检查 -> 创建订单 -> 发送通知）
     */
    public APIResponse<OrderVO> complexOrderProcessExample() {
        log.info("=== 示例8: 复杂业务场景 - 组合多个远程调用 ===");
        
        Long userId = 12345L;
        Long productId = 67890L;
        Integer quantity = 2;
        
        try {
            // 1. 验证用户
            UserQueryRequest userRequest = new UserQueryRequest();
            userRequest.setUserId(userId);
            
            UserResponse userResponse = RemoteCallWrapper.call(
                req -> userService.getUserInfo(req),
                userRequest,
                "validateUser"
            );
            
            // 2. 检查库存
            StockQueryRequest stockRequest = new StockQueryRequest();
            stockRequest.setProductId(productId);
            stockRequest.setQuantity(quantity);
            
            StockResponse stockResponse = RemoteCallWrapper.call(
                req -> orderService.checkStock(req),
                stockRequest,
                "checkStock"
            );
            
            // 3. 创建订单
            OrderCreateRequest orderRequest = new OrderCreateRequest();
            orderRequest.setUserId(userId);
            orderRequest.setProductId(productId);
            orderRequest.setQuantity(quantity);
            orderRequest.setAmount(stockResponse.getPrice() * quantity);
            
            OrderResponse orderResponse = RemoteCallWrapper.callWithRetry(
                req -> orderService.createOrder(req),
                orderRequest,
                "createOrder",
                2,    // 创建订单重试2次
                true,
                500   // 间隔500ms
            );
            
            // 4. 发送通知（不阻塞主流程）
            NotificationRequest notificationRequest = new NotificationRequest();
            notificationRequest.setUserId(userId);
            notificationRequest.setMessage("订单创建成功，订单号：" + orderResponse.getOrderId());
            notificationRequest.setType("ORDER_CREATED");
            
            RemoteCallWrapper.call(
                req -> userService.sendNotification(req),
                notificationRequest,
                "sendOrderNotification",
                false  // 通知失败不影响主流程
            );
            
            return APIResponse.success(convertToVO(orderResponse), "订单处理成功");
            
        } catch (Exception e) {
            log.error("复杂订单流程处理失败", e);
            return APIResponse.error("订单处理失败: " + e.getMessage());
        }
    }

    // ==================== 模拟的远程服务实现 ====================
    
    /**
     * 模拟订单服务
     */
    private static class MockOrderService {
        
        public OrderResponse createOrder(OrderCreateRequest request) {
            // 模拟网络延迟
            simulateDelay(100);
            
            OrderResponse response = new OrderResponse();
            response.setSuccess(true);
            response.setResponseCode("SUCCESS");
            response.setResponseMessage("订单创建成功");
            response.setOrderId(System.currentTimeMillis());
            response.setStatus("CREATED");
            return response;
        }
        
        public PaymentResponse processPayment(PaymentRequest request) {
            // 模拟网络不稳定（30%概率失败）
            if (Math.random() < 0.3) {
                throw new RuntimeException("网络连接超时");
            }
            
            simulateDelay(200);
            
            PaymentResponse response = new PaymentResponse();
            response.setSuccess(true);
            response.setResponseCode("SUCCESS");
            response.setResponseMessage("支付成功");
            response.setPaymentId(System.currentTimeMillis());
            response.setStatus("PAID");
            return response;
        }
        
        public StockResponse checkStock(StockQueryRequest request) {
            simulateDelay(50);
            
            StockResponse response = new StockResponse();
            response.setSuccess(true);
            response.setResponseCode("SUCCESS");
            response.setResponseMessage("库存充足");
            response.setAvailableStock(100);
            response.setPrice(99.99);
            return response;
        }
    }
    
    /**
     * 模拟用户服务
     */
    private static class MockUserService {
        
        public UserResponse getUserInfo(UserQueryRequest request) {
            simulateDelay(80);
            
            UserResponse response = new UserResponse();
            response.setSuccess(true);
            response.setResponseCode("SUCCESS");
            response.setResponseMessage("用户信息查询成功");
            response.setUserId(request.getUserId());
            response.setUsername("testUser");
            response.setEmail("test@example.com");
            return response;
        }
        
        public NotificationResponse sendNotification(NotificationRequest request) {
            simulateDelay(30);
            
            NotificationResponse response = new NotificationResponse();
            response.setSuccess(true);
            response.setResponseCode("SUCCESS");
            response.setResponseMessage("通知发送成功");
            response.setNotificationId(System.currentTimeMillis());
            return response;
        }
    }
    
    /**
     * 模拟配置服务
     */
    private static class MockConfigService {
        
        public ConfigResponse getSystemConfig() {
            simulateDelay(20);
            
            ConfigResponse response = new ConfigResponse();
            response.setSuccess(true);
            response.setResponseCode("SUCCESS");
            response.setResponseMessage("配置获取成功");
            response.setMaxOrderAmount(10000.0);
            response.setDefaultTimeout(30);
            return response;
        }
    }
    
    // ==================== 辅助方法 ====================
    
    /**
     * 模拟网络延迟
     */
    private static void simulateDelay(long milliseconds) {
        try {
            Thread.sleep(milliseconds);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
    
    /**
     * 转换为VO对象（实际项目中使用MapStruct等工具）
     */
    private OrderVO convertToVO(OrderResponse response) {
        OrderVO vo = new OrderVO();
        vo.setOrderId(response.getOrderId());
        vo.setStatus(response.getStatus());
        return vo;
    }
    
    private UserVO convertToVO(UserResponse response) {
        UserVO vo = new UserVO();
        vo.setUserId(response.getUserId());
        vo.setUsername(response.getUsername());
        vo.setEmail(response.getEmail());
        return vo;
    }
    
    private PaymentVO convertToVO(PaymentResponse response) {
        PaymentVO vo = new PaymentVO();
        vo.setPaymentId(response.getPaymentId());
        vo.setStatus(response.getStatus());
        return vo;
    }
    
    private ConfigVO convertToVO(ConfigResponse response) {
        ConfigVO vo = new ConfigVO();
        vo.setMaxOrderAmount(response.getMaxOrderAmount());
        vo.setDefaultTimeout(response.getDefaultTimeout());
        return vo;
    }

    // ==================== 请求响应DTO类 ====================
    
    @Data
    @EqualsAndHashCode(callSuper = false)
    public static class OrderCreateRequest extends BaseRequest {
        private Long userId;
        private Long productId;
        private Integer quantity;
        private Double amount;
    }
    
    @Data
    @EqualsAndHashCode(callSuper = false)
    public static class UserQueryRequest extends BaseRequest {
        private Long userId;
    }
    
    @Data
    @EqualsAndHashCode(callSuper = false)
    public static class PaymentRequest extends BaseRequest {
        private Long orderId;
        private Double amount;
        private String paymentMethod;
    }
    
    @Data
    @EqualsAndHashCode(callSuper = false)
    public static class StockQueryRequest extends BaseRequest {
        private Long productId;
        private Integer quantity;
    }
    
    @Data
    @EqualsAndHashCode(callSuper = false)
    public static class NotificationRequest extends BaseRequest {
        private Long userId;
        private String message;
        private String type;
    }
    
    // 响应类
    @Data
    public static class OrderResponse {
        private Boolean success;
        private String responseCode;
        private String responseMessage;
        private Long orderId;
        private String status;
    }
    
    @Data
    public static class UserResponse {
        private Boolean success;
        private String responseCode;
        private String responseMessage;
        private Long userId;
        private String username;
        private String email;
    }
    
    @Data
    public static class PaymentResponse {
        private Boolean success;
        private String responseCode;
        private String responseMessage;
        private Long paymentId;
        private String status;
    }
    
    @Data
    public static class StockResponse {
        private Boolean success;
        private String responseCode;
        private String responseMessage;
        private Integer availableStock;
        private Double price;
    }
    
    @Data
    public static class NotificationResponse {
        private Boolean success;
        private String responseCode;
        private String responseMessage;
        private Long notificationId;
    }
    
    @Data
    public static class ConfigResponse {
        private Boolean success;
        private String responseCode;
        private String responseMessage;
        private Double maxOrderAmount;
        private Integer defaultTimeout;
    }
    
    // VO类
    @Data
    public static class OrderVO {
        private Long orderId;
        private String status;
    }
    
    @Data
    public static class UserVO {
        private Long userId;
        private String username;
        private String email;
    }
    
    @Data
    public static class PaymentVO {
        private Long paymentId;
        private String status;
    }
    
    @Data
    public static class ConfigVO {
        private Double maxOrderAmount;
        private Integer defaultTimeout;
    }
}
