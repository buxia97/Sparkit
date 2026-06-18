package com.sparkit.ai.controller;

import com.sparkit.common.model.PageQuery;
import com.sparkit.common.model.PageResult;
import com.sparkit.common.model.R;
import com.sparkit.ai.model.entity.AiGeneration;
import com.sparkit.ai.model.entity.AiModel;
import com.sparkit.ai.service.AiGenerationService;
import com.sparkit.ai.service.AiModelService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * AI 管理
 */
@RestController
@RequestMapping("/api/v1/admin/ai")
@RequiredArgsConstructor
public class AiController {

    private final AiModelService modelService;
    private final AiGenerationService generationService;

    // ============ 模型管理 ============

    @GetMapping("/models")
    public R<List<AiModel>> modelList() {
        return R.ok(modelService.listEnabled());
    }

    @GetMapping("/models/{id}")
    public R<AiModel> modelGet(@PathVariable Long id) {
        return R.ok(modelService.getById(id));
    }

    @PostMapping("/models")
    public R<?> modelCreate(@RequestBody AiModel model) {
        modelService.save(model);
        return R.ok();
    }

    @PutMapping("/models/{id}")
    public R<?> modelUpdate(@PathVariable Long id, @RequestBody AiModel model) {
        model.setId(id);
        modelService.updateById(model);
        return R.ok();
    }

    @DeleteMapping("/models/{id}")
    public R<?> modelDelete(@PathVariable Long id) {
        modelService.removeById(id);
        return R.ok();
    }

    // ============ 生成记录与统计 ============

    @GetMapping("/generations")
    public R<PageResult<AiGeneration>> generationList(PageQuery query) {
        return R.ok(generationService.page(query));
    }

    @GetMapping("/statistics/model-count")
    public R<List<Map<String, Object>>> countByModel(@RequestParam(required = false) String startTime,
                                                      @RequestParam(required = false) String endTime) {
        return R.ok(generationService.countByModel(startTime, endTime));
    }
}