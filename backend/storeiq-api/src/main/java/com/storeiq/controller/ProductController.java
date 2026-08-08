package com.storeiq.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.storeiq.common.QueryPageParam;
import com.storeiq.common.Result;
import com.storeiq.entity.Product;
import com.storeiq.entity.ProductCategory;
import com.storeiq.mapper.CategoryMapper;
import com.storeiq.mapper.InventoryMapper;
import com.storeiq.mapper.ProductMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 商品管理控制器
 */
@RestController
@RequestMapping("/products")
public class ProductController {

    @Autowired
    private ProductMapper productMapper;

    @Autowired
    private CategoryMapper categoryMapper;

    @Autowired
    private InventoryMapper inventoryMapper;

    /** 分页列表 */
    @PostMapping("/list")
    public Result<?> list(@RequestBody QueryPageParam query) {
        HashMap<String, Object> param = query.getParam();
        Page<Product> page = new Page<>(query.getPageNum(), query.getPageSize());
        LambdaQueryWrapper<Product> wrapper = new LambdaQueryWrapper<>();

        if (param != null) {
            String name = (String) param.get("name");
            Object categoryId = param.get("categoryId");
            String brand = (String) param.get("brand");
            Object costPriceMin = param.get("costPriceMin");
            Object costPriceMax = param.get("costPriceMax");
            Object salePriceMin = param.get("salePriceMin");
            Object salePriceMax = param.get("salePriceMax");
            Object statusObj = param.get("status");

            if (StringUtils.isNotBlank(name)) wrapper.like(Product::getName, name);
            if (categoryId != null && StringUtils.isNotBlank(categoryId.toString())) wrapper.eq(Product::getCategoryId, Long.parseLong(categoryId.toString()));
            if (StringUtils.isNotBlank(brand)) wrapper.eq(Product::getBrand, brand);
            if (costPriceMin != null && StringUtils.isNotBlank(costPriceMin.toString()))
                wrapper.ge(Product::getCostPrice, new java.math.BigDecimal(costPriceMin.toString()));
            if (costPriceMax != null && StringUtils.isNotBlank(costPriceMax.toString()))
                wrapper.le(Product::getCostPrice, new java.math.BigDecimal(costPriceMax.toString()));
            if (salePriceMin != null && StringUtils.isNotBlank(salePriceMin.toString()))
                wrapper.ge(Product::getSalePrice, new java.math.BigDecimal(salePriceMin.toString()));
            if (salePriceMax != null && StringUtils.isNotBlank(salePriceMax.toString()))
                wrapper.le(Product::getSalePrice, new java.math.BigDecimal(salePriceMax.toString()));
            if (statusObj != null && StringUtils.isNotBlank(statusObj.toString())) {
                wrapper.eq(Product::getStatus, Integer.parseInt(statusObj.toString()));
            } else {
                wrapper.eq(Product::getStatus, 1);
            }
        } else {
            wrapper.eq(Product::getStatus, 1);
        }
        wrapper.orderByDesc(Product::getCreateTime);

        IPage<Product> result = productMapper.selectPage(page, wrapper);

        // 批量查库存总量
        Set<Long> productIds = result.getRecords().stream().map(Product::getId).collect(Collectors.toSet());
        Map<Long, Integer> inventoryMap = productIds.isEmpty() ? Collections.emptyMap()
                : inventoryMapper.sumQuantityByProductIds(productIds).stream()
                .collect(Collectors.toMap(m -> (Long) m.get("product_id"), m -> ((Number) m.get("total")).intValue(), Integer::sum));

        // 加载分类名称
        List<Map<String, Object>> enriched = result.getRecords().stream().map(p -> {
            Map<String, Object> map = new HashMap<>();
            map.put("id", p.getId());
            map.put("name", p.getName());
            map.put("code", p.getCode());
            map.put("categoryId", p.getCategoryId());
            map.put("brand", p.getBrand());
            map.put("model", p.getModel());
            map.put("unit", p.getUnit());
            map.put("costPrice", p.getCostPrice());
            map.put("salePrice", p.getSalePrice());
            map.put("safetyStock", p.getSafetyStock());
            map.put("image", p.getImage());
            map.put("description", p.getDescription());
            map.put("status", p.getStatus());
            map.put("createTime", p.getCreateTime());
            map.put("totalInventory", inventoryMap.getOrDefault(p.getId(), 0));

            ProductCategory cat = categoryMapper.selectById(p.getCategoryId());
            map.put("categoryName", cat != null ? cat.getName() : "-");
            return map;
        }).collect(Collectors.toList());

        return Result.success(enriched, result.getTotal());
    }

    /** 全量列表（供下拉选择） */
    @GetMapping("/all")
    public Result<?> all() {
        List<Product> products = productMapper.selectList(
                new LambdaQueryWrapper<Product>().eq(Product::getStatus, 1).orderByAsc(Product::getName));
        return Result.success(products);
    }

    /** 详情 */
    @GetMapping("/{id}")
    public Result<?> detail(@PathVariable Long id) {
        Product product = productMapper.selectById(id);
        if (product == null) return Result.fail("商品不存在");
        return Result.success(product);
    }

    /** 新增 */
    @PostMapping
    public Result<?> save(@RequestBody Product product) {
        product.setCreateTime(LocalDateTime.now());
        product.setUpdateTime(LocalDateTime.now());
        return productMapper.insert(product) > 0 ? Result.success() : Result.fail();
    }

    /** 编辑 */
    @PutMapping("/{id}")
    public Result<?> update(@PathVariable Long id, @RequestBody Product product) {
        product.setId(id);
        product.setUpdateTime(LocalDateTime.now());
        return productMapper.updateById(product) > 0 ? Result.success() : Result.fail();
    }

    /** 删除（软删除） */
    @DeleteMapping("/{id}")
    public Result<?> delete(@PathVariable Long id) {
        Product product = productMapper.selectById(id);
        if (product == null) return Result.fail("商品不存在");
        product.setStatus(0);
        product.setUpdateTime(LocalDateTime.now());
        return productMapper.updateById(product) > 0 ? Result.success() : Result.fail();
    }
}
