package com.sparkit.notification.controller;

import com.sparkit.common.model.PageQuery;
import com.sparkit.common.model.PageResult;
import com.sparkit.common.model.R;
import com.sparkit.notification.model.entity.NotifyRecord;
import com.sparkit.notification.model.entity.NotifyTemplate;
import com.sparkit.notification.service.NotifyRecordService;
import com.sparkit.notification.service.NotifyTemplateService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 通知管理
 */
@Tag(name = "通知管理", description = "通知模板和发送记录的增删改查")
@RestController
@RequestMapping("/api/v1/admin/notification")
@RequiredArgsConstructor
public class NotificationController {

    private final NotifyTemplateService templateService;
    private final NotifyRecordService recordService;

    // ============ 模板 ============

    @Operation(summary = "通知模板列表")
    @GetMapping("/templates")
    public R<List<NotifyTemplate>> templateList() {
        return R.ok(templateService.list());
    }

    @Operation(summary = "获取模板详情")
    @GetMapping("/templates/{id}")
    public R<NotifyTemplate> templateGet(@PathVariable Long id) {
        return R.ok(templateService.getById(id));
    }

    @Operation(summary = "创建模板")
    @PostMapping("/templates")
    public R<?> templateCreate(@RequestBody NotifyTemplate template) {
        templateService.save(template);
        return R.ok();
    }

    @Operation(summary = "更新模板")
    @PutMapping("/templates/{id}")
    public R<?> templateUpdate(@PathVariable Long id, @RequestBody NotifyTemplate template) {
        template.setId(id);
        templateService.updateById(template);
        return R.ok();
    }

    @Operation(summary = "删除模板")
    @DeleteMapping("/templates/{id}")
    public R<?> templateDelete(@PathVariable Long id) {
        templateService.removeById(id);
        return R.ok();
    }

    // ============ 记录与统计 ============

    @Operation(summary = "发送记录列表")
    @GetMapping("/records")
    public R<PageResult<NotifyRecord>> recordList(PageQuery query) {
        return R.ok(recordService.page(query));
    }

    @Operation(summary = "按类型统计发送量")
    @GetMapping("/statistics/type-count")
    public R<List<Map<String, Object>>> countByType(@RequestParam(required = false) String startTime,
                                                     @RequestParam(required = false) String endTime) {
        return R.ok(recordService.countByType(startTime, endTime));
    }
}