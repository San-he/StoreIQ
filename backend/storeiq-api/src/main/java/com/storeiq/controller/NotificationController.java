package com.storeiq.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.storeiq.common.Result;
import com.storeiq.entity.Notification;
import com.storeiq.mapper.NotificationMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 通知消息控制器
 */
@RestController
@RequestMapping("/notifications")
public class NotificationController {

    @Autowired
    private NotificationMapper notificationMapper;

    @GetMapping("/list")
    public Result<?> list() {
        List<Notification> list = notificationMapper.selectList(
                new LambdaQueryWrapper<Notification>()
                        .orderByDesc(Notification::getCreateTime)
                        .last("LIMIT 50"));
        return Result.success(list);
    }

    @GetMapping("/unread-count")
    public Result<?> unreadCount() {
        long count = notificationMapper.selectCount(
                new LambdaQueryWrapper<Notification>().eq(Notification::getIsRead, 0));
        return Result.success(count);
    }

    @PutMapping("/{id}/read")
    public Result<?> markRead(@PathVariable Long id) {
        Notification notification = notificationMapper.selectById(id);
        if (notification == null) return Result.fail("通知不存在");
        notification.setIsRead(1);
        notificationMapper.updateById(notification);
        return Result.success();
    }

    @PutMapping("/read-all")
    public Result<?> markAllRead() {
        List<Notification> unread = notificationMapper.selectList(
                new LambdaQueryWrapper<Notification>().eq(Notification::getIsRead, 0));
        for (Notification n : unread) {
            n.setIsRead(1);
            notificationMapper.updateById(n);
        }
        return Result.success();
    }
}
