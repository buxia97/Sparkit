package com.sparkit.system.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.sparkit.common.model.PageQuery;
import com.sparkit.common.model.PageResult;
import com.sparkit.common.model.R;
import com.sparkit.framework.model.entity.IpBlacklist;
import com.sparkit.framework.service.IpBlacklistService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * IP 黑名单管理
 */
@Tag(name = "IP 黑名单", description = "IP 封禁、解封、检查")
@RestController
@RequiredArgsConstructor
public class IpBlacklistController {

    private final IpBlacklistService ipBlacklistService;

    /** 分页查询 */
    @Operation(summary = "分页查询黑名单")
    @GetMapping("/api/v1/admin/ip-blacklist")
    public R<PageResult<IpBlacklist>> list(PageQuery query) {
        IPage<IpBlacklist> page = new Page<>(query.getPage(), query.getPageSize());
        LambdaQueryWrapper<IpBlacklist> wrapper = new LambdaQueryWrapper<>();
        if (query.getKeyword() != null) {
            wrapper.like(IpBlacklist::getIp, query.getKeyword());
        }
        wrapper.orderByDesc(IpBlacklist::getCreateTime);
        IPage<IpBlacklist> result = ipBlacklistService.page(page, wrapper);
        return R.ok(PageResult.of(result.getRecords(), result.getTotal(), result.getCurrent(), result.getSize()));
    }

    /** 封禁 IP */
    @Operation(summary = "封禁 IP")
    @PostMapping("/api/v1/admin/ip-blacklist/ban")
    public R<?> ban(@RequestBody Map<String, Object> params) {
        String ip = (String) params.get("ip");
        String reason = (String) params.getOrDefault("reason", "手动封禁");
        Integer duration = params.get("duration") != null
                ? Integer.valueOf(params.get("duration").toString()) : null;
        ipBlacklistService.banIp(ip, reason, duration);
        return R.ok();
    }

    /** 解封 IP */
    @Operation(summary = "解封 IP")
    @PostMapping("/api/v1/admin/ip-blacklist/unban")
    public R<?> unban(@RequestParam String ip) {
        ipBlacklistService.unbanIp(ip);
        return R.ok();
    }

    /** 检查 IP 是否被封禁 */
    @Operation(summary = "检查 IP 状态")
    @GetMapping("/api/v1/admin/ip-blacklist/check")
    public R<Map<String, Boolean>> check(@RequestParam String ip) {
        boolean banned = ipBlacklistService.isBanned(ip);
        return R.ok(Map.of("banned", banned));
    }
}