package com.storeiq.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.storeiq.common.QueryPageParam;
import com.storeiq.common.Result;
import com.storeiq.entity.Supplier;
import com.storeiq.mapper.SupplierMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;

/**
 * 供应商管理控制器
 */
@RestController
@RequestMapping("/suppliers")
public class SupplierController {

    @Autowired
    private SupplierMapper supplierMapper;

    @PostMapping("/list")
    public Result<?> list(@RequestBody QueryPageParam query) {
        HashMap<String, Object> param = query.getParam();
        Page<Supplier> page = new Page<>(query.getPageNum(), query.getPageSize());
        LambdaQueryWrapper<Supplier> wrapper = new LambdaQueryWrapper<>();

        if (param != null) {
            String name = (String) param.get("name");
            if (StringUtils.isNotBlank(name)) wrapper.like(Supplier::getName, name);
        }
        wrapper.orderByDesc(Supplier::getCreateTime);

        IPage<Supplier> result = supplierMapper.selectPage(page, wrapper);
        return Result.success(result.getRecords(), result.getTotal());
    }

    @GetMapping("/all")
    public Result<?> all() {
        List<Supplier> list = supplierMapper.selectList(
                new LambdaQueryWrapper<Supplier>().orderByAsc(Supplier::getName));
        return Result.success(list);
    }

    @PostMapping
    public Result<?> save(@RequestBody Supplier supplier) {
        return supplierMapper.insert(supplier) > 0 ? Result.success() : Result.fail();
    }

    @PutMapping("/{id}")
    public Result<?> update(@PathVariable Long id, @RequestBody Supplier supplier) {
        supplier.setId(id);
        return supplierMapper.updateById(supplier) > 0 ? Result.success() : Result.fail();
    }

    @DeleteMapping("/{id}")
    public Result<?> delete(@PathVariable Long id) {
        return supplierMapper.deleteById(id) > 0 ? Result.success() : Result.fail();
    }
}
