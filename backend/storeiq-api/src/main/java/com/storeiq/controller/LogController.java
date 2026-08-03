package com.storeiq.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.storeiq.common.QueryPageParam;
import com.storeiq.common.Result;
import com.storeiq.entity.OperationLog;
import com.storeiq.mapper.OperationLogMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;

/**
 * 操作日志控制器
 */
@RestController
@RequestMapping("/logs")
public class LogController {

    @Autowired
    private OperationLogMapper logMapper;

    @GetMapping("/modules")
    public Result<?> modules() {
        List<String> modules = java.util.Arrays.asList(
                "store", "product", "category", "inventory", "purchase", "sale", "supplier", "user");
        return Result.success(modules);
    }

    @PostMapping("/list")
    public Result<?> list(@RequestBody QueryPageParam query) {
        HashMap<String, Object> param = query.getParam();
        Page<OperationLog> page = new Page<>(query.getPageNum(), query.getPageSize());
        LambdaQueryWrapper<OperationLog> wrapper = new LambdaQueryWrapper<>();

        if (param != null) {
            String module = (String) param.get("module");
            String action = (String) param.get("action");
            if (StringUtils.isNotBlank(module)) wrapper.eq(OperationLog::getModule, module);
            if (StringUtils.isNotBlank(action)) wrapper.eq(OperationLog::getAction, action);
        }
        wrapper.orderByDesc(OperationLog::getCreateTime);

        IPage<OperationLog> result = logMapper.selectPage(page, wrapper);
        return Result.success(result.getRecords(), result.getTotal());
    }
}
