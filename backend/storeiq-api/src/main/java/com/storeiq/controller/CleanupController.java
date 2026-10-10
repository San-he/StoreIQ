package com.storeiq.controller;

import com.storeiq.common.Result;
import com.storeiq.entity.CleanupRule;
import com.storeiq.entity.Notification;
import com.storeiq.mapper.CleanupRuleMapper;
import com.storeiq.mapper.NotificationMapper;
import com.storeiq.task.CleanupScheduler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * 数据清理控制器 — 汇总、规则管理、手动触发
 */
@RestController
@RequestMapping("/cleanup")
public class CleanupController {

    @Autowired
    private NotificationMapper notificationMapper;

    @Autowired
    private CleanupRuleMapper cleanupRuleMapper;

    @Autowired(required = false)
    private CleanupScheduler cleanupScheduler;

    /** 返回最新未读 data_cleanup 通知的 content JSON */
    @GetMapping("/summary")
    public Result<?> summary() {
        List<Notification> list = notificationMapper.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<Notification>()
                        .eq(Notification::getType, "data_cleanup")
                        .eq(Notification::getIsRead, 0)
                        .orderByDesc(Notification::getCreateTime)
                        .last("LIMIT 1"));
        if (list.isEmpty()) {
            Map<String, Object> empty = new HashMap<>();
            empty.put("totalRecords", 0);
            empty.put("tables", Collections.emptyList());
            empty.put("scanTime", null);
            return Result.success(empty);
        }
        Notification notification = list.get(0);
        Map<String, Object> result = new HashMap<>();
        result.put("notificationId", notification.getId());
        result.put("title", notification.getTitle());
        result.put("content", notification.getContent());
        result.put("createTime", notification.getCreateTime() != null ? notification.getCreateTime().toString() : null);
        return Result.success(result);
    }

    /** 返回所有保留规则 */
    @GetMapping("/rules")
    public Result<?> rules() {
        List<CleanupRule> list = cleanupRuleMapper.selectList(null);
        return Result.success(list);
    }

    /** 保存保留规则 */
    @PutMapping("/rules")
    public Result<?> saveRules(@RequestBody List<CleanupRule> rules) {
        for (CleanupRule rule : rules) {
            if (rule.getId() != null) {
                cleanupRuleMapper.updateById(rule);
            } else {
                cleanupRuleMapper.insert(rule);
            }
        }
        return Result.success("保存成功");
    }

    /** 手动触发清理扫描（调试用） */
    @PostMapping("/execute")
    public Result<?> execute() {
        if (cleanupScheduler == null) return Result.fail("清理调度器未初始化");
        try {
            cleanupScheduler.executeNow();
            return Result.success("清理完成");
        } catch (Exception e) {
            return Result.fail("清理失败: " + e.getMessage());
        }
    }

    /** 清理历史列表 */
    @GetMapping("/history")
    public Result<?> history() {
        List<Notification> list = notificationMapper.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<Notification>()
                        .eq(Notification::getType, "data_cleanup")
                        .eq(Notification::getIsRead, 1)
                        .orderByDesc(Notification::getCreateTime)
                        .last("LIMIT 20"));
        return Result.success(list);
    }
}
