package com.storeiq.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.storeiq.common.Result;
import com.storeiq.entity.ProductCategory;
import com.storeiq.mapper.CategoryMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 商品分类控制器
 */
@RestController
@RequestMapping("/categories")
public class CategoryController {

    @Autowired
    private CategoryMapper categoryMapper;

    @GetMapping("/list")
    public Result<?> list() {
        List<ProductCategory> list = categoryMapper.selectList(
                new LambdaQueryWrapper<ProductCategory>().orderByAsc(ProductCategory::getId));
        return Result.success(list);
    }

    @PostMapping
    public Result<?> save(@RequestBody ProductCategory category) {
        return categoryMapper.insert(category) > 0 ? Result.success() : Result.fail();
    }

    @PutMapping("/{id}")
    public Result<?> update(@PathVariable Long id, @RequestBody ProductCategory category) {
        category.setId(id);
        return categoryMapper.updateById(category) > 0 ? Result.success() : Result.fail();
    }

    @DeleteMapping("/{id}")
    public Result<?> delete(@PathVariable Long id) {
        return categoryMapper.deleteById(id) > 0 ? Result.success() : Result.fail();
    }
}
