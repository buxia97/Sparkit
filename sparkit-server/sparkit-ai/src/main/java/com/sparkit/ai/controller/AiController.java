package com.sparkit.ai.controller;

import com.sparkit.common.model.PageQuery;
import com.sparkit.common.model.PageResult;
import com.sparkit.common.model.R;
import com.sparkit.ai.model.entity.AiGeneration;
import com.sparkit.ai.model.entity.AiMessage;
import com.sparkit.ai.model.entity.AiModel;
import com.sparkit.ai.model.entity.AiSession;
import com.sparkit.ai.service.AiChatService;
import com.sparkit.ai.service.AiGenerationService;
import com.sparkit.ai.service.AiModelService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;
import java.util.Map;

/**
 * AI 管理
 */
@Tag(name = "AI 管理", description = "AI 模型管理、对话、会话管理")
@RestController
@RequestMapping("/api/v1/admin/ai")
@RequiredArgsConstructor
public class AiController {

    private final AiModelService modelService;
    private final AiGenerationService generationService;
    private final AiChatService chatService;

    // ============ 模型管理 ============

    @Operation(summary = "模型列表")
    @GetMapping("/models")
    public R<List<AiModel>> modelList() {
        return R.ok(modelService.listEnabled());
    }

    @Operation(summary = "获取模型详情")
    @GetMapping("/models/{id}")
    public R<AiModel> modelGet(@PathVariable Long id) {
        return R.ok(modelService.getById(id));
    }

    @Operation(summary = "创建模型")
    @PostMapping("/models")
    public R<?> modelCreate(@RequestBody AiModel model) {
        modelService.save(model);
        return R.ok();
    }

    @Operation(summary = "更新模型")
    @PutMapping("/models/{id}")
    public R<?> modelUpdate(@PathVariable Long id, @RequestBody AiModel model) {
        model.setId(id);
        modelService.updateById(model);
        return R.ok();
    }

    @Operation(summary = "删除模型")
    @DeleteMapping("/models/{id}")
    public R<?> modelDelete(@PathVariable Long id) {
        modelService.removeById(id);
        return R.ok();
    }

    // ============ 对话 ============

    @Operation(summary = "AI 对话")
    @PostMapping("/chat")
    public R<Map<String, Object>> chat(@RequestBody Map<String, Object> params) {
        Long sessionId = params.get("sessionId") != null ? ((Number) params.get("sessionId")).longValue() : null;
        String provider = (String) params.get("provider");
        String model = (String) params.get("model");
        String content = (String) params.get("content");
        Map<String, Object> result = chatService.chat(sessionId, provider, model, content);
        return result.containsKey("error") ? R.fail((String) result.get("error")) : R.ok(result);
    }

    @Operation(summary = "AI 流式对话")
    @PostMapping(value = "/chat/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter chatStream(@RequestBody Map<String, Object> params) {
        Long sessionId = params.get("sessionId") != null ? ((Number) params.get("sessionId")).longValue() : null;
        String provider = (String) params.get("provider");
        String model = (String) params.get("model");
        String content = (String) params.get("content");
        return chatService.chatStream(sessionId, provider, model, content);
    }

    @Operation(summary = "可用模型列表")
    @GetMapping("/models/available")
    public R<List<Map<String, Object>>> availableModels() {
        return R.ok(chatService.getModels());
    }

    // ============ 会话管理 ============

    @Operation(summary = "创建会话")
    @PostMapping("/sessions")
    public R<AiSession> createSession(@RequestBody Map<String, Object> params) {
        Long userId = params.get("userId") != null ? ((Number) params.get("userId")).longValue() : null;
        String provider = (String) params.get("provider");
        String sessionName = (String) params.get("sessionName");
        return R.ok(chatService.createSession(userId, provider, sessionName));
    }

    @Operation(summary = "会话消息列表")
    @GetMapping("/sessions/{sessionId}/messages")
    public R<List<AiMessage>> sessionMessages(@PathVariable Long sessionId) {
        return R.ok(chatService.getSessionMessages(sessionId));
    }

    // ============ 生成记录与统计 ============

    @Operation(summary = "生成记录列表")
    @GetMapping("/generations")
    public R<PageResult<AiGeneration>> generationList(PageQuery query) {
        return R.ok(generationService.page(query));
    }

    @Operation(summary = "按模型统计生成量")
    @GetMapping("/statistics/model-count")
    public R<List<Map<String, Object>>> countByModel(@RequestParam(required = false) String startTime,
                                                      @RequestParam(required = false) String endTime) {
        return R.ok(generationService.countByModel(startTime, endTime));
    }
}