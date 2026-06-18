package com.sparkit.notification.controller;

import com.sparkit.common.model.PageQuery;
import com.sparkit.common.model.PageResult;
import com.sparkit.common.model.R;
import com.sparkit.notification.model.entity.NotifyRecord;
import com.sparkit.notification.model.entity.NotifyTemplate;
import com.sparkit.notification.service.NotifyRecordService;
import com.sparkit.notification.service.NotifyTemplateService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 通知管理
 */
@RestController
@RequestMapping("/api/v1/admin/notification")
@RequiredArgsConstructor
public class NotificationController {

    private final NotifyTemplateService templateService;
    private final NotifyRecordService recordService;

    // ============ 模板 ============

    @GetMapping("/templates")
    public R<List<NotifyTemplate>> templateList() {
        return R.ok(templateService.list());
    }

    @GetMapping("/templates/{id}")
    public R<NotifyTemplate> templateGet(@PathVariable Long id) {
        return R.ok(templateService.getById(id));
    }

    @PostMapping("/templates")
    public R<?> templateCreate(@RequestBody NotifyTemplate template) {
        templateService.save(template);
        return R.ok();
    }

    @PutMapping("/templates/{id}")
    public R<?> templateUpdate(@PathVariable Long id, @RequestBody NotifyTemplate template) {
        template.setId(id);
        templateService.updateById(template);
        return R.ok();
    }

    @DeleteMapping("/templates/{id}")
    public R<?> templateDelete(@PathVariable Long id) {
        templateService.removeById(id);
        return R.ok();
    }

    // ============ 记录与统计 ============

    @GetMapping("/records")
    public R<PageResult<NotifyRecord>> recordList(PageQuery query) {
        return R.ok(recordService.page(query));
    }

    @GetMapping("/statistics/type-count")
    public R<List<Map<String, Object>>> countByType(@RequestParam(required = false) String startTime,
                                                     @RequestParam(required = false) String endTime) {
        return R.ok(recordService.countByType(startTime, endTime));
    }
}