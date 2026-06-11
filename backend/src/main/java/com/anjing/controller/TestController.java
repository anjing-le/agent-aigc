package com.anjing.controller;

import com.anjing.annotation.ScaffoldSample;
import com.anjing.config.middleware.MiddlewareManager;
import com.anjing.model.constants.ApiConstants;
import com.anjing.model.constants.ServiceBoundaryConstants;
import com.anjing.model.errorcode.CommonErrorCode;
import com.anjing.model.exception.BizException;
import com.anjing.model.response.APIResponse;
import com.anjing.util.DateUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.env.Environment;
import org.springframework.web.bind.annotation.*;

import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.time.Duration;
import java.util.*;

/**
 * 脚手架功能测试控制器
 *
 * <p>用于测试和演示脚手架的各种功能特性，同时作为学员学习 API 开发的参考示例。</p>
 * <p>复制为业务项目时，请按 {@code project_document/TEMPLATE_BOUNDARIES.md} 明确删除或替换。</p>
 *
 * <h3>测试功能清单：</h3>
 * <ul>
 *   <li>健康检查接口</li>
 *   <li>Controller 层日志记录</li>
 *   <li>全局异常处理演示</li>
 *   <li>参数校验演示</li>
 *   <li>简单 CRUD 示例（内存数据）</li>
 * </ul>
 *
 * @author Backend Template Team
 * @version 1.0
 */
@ScaffoldSample("示例接口：健康检查、异常处理和内存 CRUD 演示")
@RestController
@RequestMapping(ApiConstants.Test.BASE)
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Scaffold Test", description = "Health-check and scaffold sample APIs")
public class TestController {

    private final MiddlewareManager middlewareManager;
    private final Environment environment;

    /** 内存数据存储（演示用，实际项目使用数据库） */
    private final Map<Long, Map<String, Object>> memoryStore = new LinkedHashMap<>();
    private long idSequence = 1;

    // ==================== 健康检查 ====================

    /**
     * 健康检查接口
     *
     * <p>用于验证后端服务是否正常运行</p>
     *
     * @return 服务状态信息
     */
    @GetMapping(ApiConstants.Test.HEALTH)
    @Operation(summary = "Health check")
    public APIResponse<Map<String, Object>> health() {
        RuntimeMXBean runtime = ManagementFactory.getRuntimeMXBean();
        Duration uptime = Duration.ofMillis(runtime.getUptime());

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("status", "UP");
        data.put("application", environment.getProperty("spring.application.name", ServiceBoundaryConstants.APPLICATION_ID));
        data.put("timestamp", DateUtils.nowIso());
        data.put("uptime", String.format("%d小时%d分%d秒",
                uptime.toHours(), uptime.toMinutesPart(), uptime.toSecondsPart()));
        data.put("javaVersion", System.getProperty("java.version"));
        String[] activeProfiles = environment.getActiveProfiles();
        data.put("activeProfiles", activeProfiles.length == 0 ? List.of("default") : Arrays.asList(activeProfiles));

        return APIResponse.success(data);
    }

    /**
     * 可选能力状态接口
     *
     * <p>用于展示中间件和基础能力当前处于 disabled / configured / ready / degraded 哪种状态。</p>
     *
     * @return 可选能力状态报告
     */
    @GetMapping(ApiConstants.Test.FEATURES)
    @Operation(summary = "Feature status")
    public APIResponse<MiddlewareManager.MiddlewareStatusReport> features() {
        return APIResponse.success(middlewareManager.statusReport());
    }

    /**
     * 简单的 Ping 接口
     *
     * @return pong
     */
    @GetMapping(ApiConstants.Test.PING)
    @Operation(summary = "Ping")
    public APIResponse<String> ping() {
        return APIResponse.successData("pong");
    }

    // ==================== 异常处理演示 ====================

    /**
     * 演示业务异常处理
     *
     * <p>抛出 BizException，验证全局异常处理器是否正常拦截</p>
     *
     * @return 不会正常返回
     */
    @GetMapping(ApiConstants.Test.EXCEPTION_BIZ)
    @Operation(summary = "Business exception sample")
    public APIResponse<Void> testBizException() {
        log.info("测试业务异常处理");
        throw new BizException(CommonErrorCode.PARAMETER_ERROR);
    }

    /**
     * 演示系统异常处理
     *
     * <p>抛出运行时异常，验证全局异常兜底处理</p>
     *
     * @return 不会正常返回
     */
    @GetMapping(ApiConstants.Test.EXCEPTION_SYSTEM)
    @Operation(summary = "System exception sample")
    public APIResponse<Void> testSystemException() {
        log.info("测试系统异常处理");
        throw new RuntimeException("这是一个模拟的系统异常");
    }

    // ==================== CRUD 示例（内存数据） ====================

    /**
     * 创建记录（Create）
     *
     * <p>演示 POST 请求和 RequestBody 接收 JSON 数据</p>
     *
     * @param body 记录数据
     * @return 创建结果
     */
    @PostMapping(ApiConstants.Test.ITEMS)
    @Operation(summary = "Create sample item")
    public APIResponse<Map<String, Object>> createItem(@RequestBody Map<String, Object> body) {
        long id = idSequence++;
        String now = DateUtils.nowIso();
        body.put("id", id);
        body.put("createTime", now);
        body.put("updateTime", now);
        memoryStore.put(id, body);

        log.info("创建记录成功: id={}", id);
        return APIResponse.success(body, "创建成功");
    }

    /**
     * 查询记录列表（Read - List）
     *
     * <p>演示 GET 请求和 RequestParam 接收查询参数</p>
     *
     * @param keyword 搜索关键词（可选）
     * @return 记录列表
     */
    @GetMapping(ApiConstants.Test.ITEMS)
    @Operation(summary = "List sample items")
    public APIResponse<Map<String, Object>> listItems(
            @RequestParam(required = false) String keyword) {

        Collection<Map<String, Object>> items;
        if (keyword != null && !keyword.isBlank()) {
            items = memoryStore.values().stream()
                    .filter(item -> item.toString().contains(keyword))
                    .toList();
        } else {
            items = memoryStore.values();
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("list", items);
        result.put("total", items.size());

        return APIResponse.success(result);
    }

    /**
     * 查询单条记录（Read - Detail）
     *
     * <p>演示 PathVariable 路径参数</p>
     *
     * @param id 记录 ID
     * @return 记录详情
     */
    @GetMapping(ApiConstants.Test.ITEM_DETAIL)
    @Operation(summary = "Get sample item")
    public APIResponse<Map<String, Object>> getItem(@PathVariable Long id) {
        Map<String, Object> item = memoryStore.get(id);
        if (item == null) {
            throw new BizException(CommonErrorCode.DATA_NOT_FOUND);
        }
        return APIResponse.success(item);
    }

    /**
     * 更新记录（Update）
     *
     * <p>演示 PUT 请求和 PathVariable + RequestBody 组合</p>
     *
     * @param id   记录 ID
     * @param body 更新数据
     * @return 更新结果
     */
    @PutMapping(ApiConstants.Test.ITEM_DETAIL)
    @Operation(summary = "Update sample item")
    public APIResponse<Map<String, Object>> updateItem(
            @PathVariable Long id,
            @RequestBody Map<String, Object> body) {

        Map<String, Object> existing = memoryStore.get(id);
        if (existing == null) {
            throw new BizException(CommonErrorCode.DATA_NOT_FOUND);
        }

        body.put("id", id);
        body.put("createTime", existing.get("createTime"));
        body.put("updateTime", DateUtils.nowIso());
        memoryStore.put(id, body);

        log.info("更新记录成功: id={}", id);
        return APIResponse.success(body, "更新成功");
    }

    /**
     * 删除记录（Delete）
     *
     * <p>演示 DELETE 请求</p>
     *
     * @param id 记录 ID
     * @return 删除结果
     */
    @DeleteMapping(ApiConstants.Test.ITEM_DETAIL)
    @Operation(summary = "Delete sample item")
    public APIResponse<Void> deleteItem(@PathVariable Long id) {
        Map<String, Object> removed = memoryStore.remove(id);
        if (removed == null) {
            throw new BizException(CommonErrorCode.DATA_NOT_FOUND);
        }

        log.info("删除记录成功: id={}", id);
        return APIResponse.successMessage("删除成功");
    }
}
