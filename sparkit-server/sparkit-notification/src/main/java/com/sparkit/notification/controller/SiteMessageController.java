package com.sparkit.notification.controller;

import com.sparkit.common.model.PageQuery;
import com.sparkit.common.model.PageResult;
import com.sparkit.common.model.R;
import com.sparkit.notification.model.entity.SiteMessage;
import com.sparkit.notification.service.SiteMessageService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 站内信管理
 */
@RestController
@RequestMapping("/api/v1/admin/site-message")
@RequiredArgsConstructor
public class SiteMessageController {

    private final SiteMessageService siteMessageService;

    @GetMapping
    public R<PageResult<SiteMessage>> list(PageQuery query, @RequestParam(required = false) Integer isRead) {
        return R.ok(siteMessageService.page(query, isRead));
    }

    @GetMapping("/unread-count")
    public R<Map<String, Long>> unreadCount() {
        return R.ok(Map.of("count", siteMessageService.unreadCount()));
    }

    @PutMapping("/{id}/read")
    public R<?> read(@PathVariable Long id) {
        siteMessageService.read(id);
        return R.ok();
    }

    @PutMapping("/read-all")
    public R<?> readAll() {
        siteMessageService.readAll();
        return R.ok();
    }

    @DeleteMapping("/{id}")
    public R<?> delete(@PathVariable Long id) {
        siteMessageService.delete(id);
        return R.ok();
    }

    @PostMapping("/send")
    public R<?> send(@RequestBody SendMessageRequest request) {
        siteMessageService.send(request.getReceiverId(), request.getTitle(), request.getContent(), request.getContentType());
        return R.ok();
    }
}