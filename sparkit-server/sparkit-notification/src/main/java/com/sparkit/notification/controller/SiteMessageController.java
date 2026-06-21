package com.sparkit.notification.controller;

import com.sparkit.common.model.PageQuery;
import com.sparkit.common.model.PageResult;
import com.sparkit.common.model.R;
import com.sparkit.notification.model.entity.SiteMessage;
import com.sparkit.notification.service.SiteMessageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 站内信管理
 */
@Tag(name = "站内信管理", description = "站内信的发送、阅读、统计")
@RestController
@RequestMapping("/api/v1/admin/site-message")
@RequiredArgsConstructor
public class SiteMessageController {

    private final SiteMessageService siteMessageService;

    @Operation(summary = "站内信列表")
    @GetMapping
    public R<PageResult<SiteMessage>> list(PageQuery query, @RequestParam(required = false) Integer isRead) {
        return R.ok(siteMessageService.page(query, isRead));
    }

    @Operation(summary = "未读数量")
    @GetMapping("/unread-count")
    public R<Map<String, Long>> unreadCount() {
        return R.ok(Map.of("count", siteMessageService.unreadCount()));
    }

    @Operation(summary = "标记已读")
    @PutMapping("/{id}/read")
    public R<?> read(@PathVariable Long id) {
        siteMessageService.read(id);
        return R.ok();
    }

    @Operation(summary = "全部标记已读")
    @PutMapping("/read-all")
    public R<?> readAll() {
        siteMessageService.readAll();
        return R.ok();
    }

    @Operation(summary = "删除站内信")
    @DeleteMapping("/{id}")
    public R<?> delete(@PathVariable Long id) {
        siteMessageService.delete(id);
        return R.ok();
    }

    @Operation(summary = "发送站内信")
    @PostMapping("/send")
    public R<?> send(@RequestBody SendMessageRequest request) {
        siteMessageService.send(request.getReceiverId(), request.getTitle(), request.getContent(), request.getContentType());
        return R.ok();
    }
}