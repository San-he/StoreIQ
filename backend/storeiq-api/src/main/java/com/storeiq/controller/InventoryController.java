package com.storeiq.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.storeiq.common.QueryPageParam;
import com.storeiq.common.Result;
import com.storeiq.entity.Inventory;
import com.storeiq.entity.Product;
import com.storeiq.entity.Store;
import com.storeiq.mapper.InventoryMapper;
import com.storeiq.mapper.ProductMapper;
import com.storeiq.mapper.StoreMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 库存管理控制器
 */
@RestController
@RequestMapping("/inventory")
public class InventoryController {

    @Autowired
    private InventoryMapper inventoryMapper;

    @Autowired
    private ProductMapper productMapper;

    @Autowired
    private StoreMapper storeMapper;

    /** 库存总览 */
    @PostMapping("/list")
    public Result<?> list(@RequestBody QueryPageParam query) {
        HashMap<String, Object> param = query.getParam();

        // 提取筛选参数（一次性赋值，保证 effectively final）
        final String storeIdStr = getString(param, "storeId");
        final String productName = getString(param, "productName");
        final String categoryIdStr = getString(param, "categoryId");
        final Integer quantityMin = getInteger(param, "quantityMin");
        final Integer quantityMax = getInteger(param, "quantityMax");

        LambdaQueryWrapper<Inventory> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.isNotBlank(storeIdStr)) wrapper.eq(Inventory::getStoreId, Long.parseLong(storeIdStr));
        wrapper.orderByAsc(Inventory::getStoreId).orderByAsc(Inventory::getProductId);

        // 全量加载（库存表通常不大）
        List<Inventory> all = inventoryMapper.selectList(wrapper);

        // 分类 / 商品名 / 数量区间 筛选
        final Long finalCatId = StringUtils.isNotBlank(categoryIdStr) ? Long.parseLong(categoryIdStr) : null;
        if (finalCatId != null || StringUtils.isNotBlank(productName) || quantityMin != null || quantityMax != null) {
            all = all.stream().filter(inv -> {
                Product p = productMapper.selectById(inv.getProductId());
                if (p == null) return false;
                if (finalCatId != null && !finalCatId.equals(p.getCategoryId())) return false;
                if (StringUtils.isNotBlank(productName) && !p.getName().contains(productName)) return false;
                if (quantityMin != null && inv.getQuantity() < quantityMin) return false;
                if (quantityMax != null && inv.getQuantity() > quantityMax) return false;
                return true;
            }).collect(Collectors.toList());
        }

        // 手动分页
        int pageNum = query.getPageNum();
        int pageSize = query.getPageSize();
        int total = all.size();
        int from = Math.min((pageNum - 1) * pageSize, total);
        int to = Math.min(from + pageSize, total);
        List<Inventory> pageList = all.subList(from, to);

        // 丰富数据
        List<Map<String, Object>> records = pageList.stream().map(inv -> {
            Map<String, Object> map = new HashMap<>();
            map.put("id", inv.getId());
            map.put("storeId", inv.getStoreId());
            map.put("productId", inv.getProductId());
            map.put("quantity", inv.getQuantity());
            map.put("updateTime", inv.getUpdateTime());

            Product p = productMapper.selectById(inv.getProductId());
            if (p != null) {
                map.put("productName", p.getName());
                map.put("productCode", p.getCode());
                map.put("categoryId", p.getCategoryId());
                map.put("brand", p.getBrand());
                map.put("safetyStock", p.getSafetyStock());
                map.put("salePrice", p.getSalePrice());
            }
            Store s = storeMapper.selectById(inv.getStoreId());
            if (s != null) {
                map.put("storeName", s.getName());
                map.put("storeRegion", s.getRegion());
            }
            return map;
        }).collect(Collectors.toList());

        return Result.success(records, (long) total);
    }

    /** 库存预警 */
    @GetMapping("/alerts")
    public Result<?> alerts() {
        List<Inventory> all = inventoryMapper.selectList(null);
        List<Map<String, Object>> alerts = new ArrayList<>();

        for (Inventory inv : all) {
            Product p = productMapper.selectById(inv.getProductId());
            if (p != null && inv.getQuantity() <= p.getSafetyStock()) {
                Map<String, Object> map = new HashMap<>();
                map.put("id", inv.getId());
                map.put("storeId", inv.getStoreId());
                map.put("productId", inv.getProductId());
                map.put("quantity", inv.getQuantity());
                map.put("safetyStock", p.getSafetyStock());
                map.put("productName", p.getName());
                map.put("productCode", p.getCode());
                Store s = storeMapper.selectById(inv.getStoreId());
                map.put("storeName", s != null ? s.getName() : "-");
                map.put("status", inv.getQuantity() <= 0 ? "缺货" : "预警");
                alerts.add(map);
            }
        }
        return Result.success(alerts);
    }

    /** 从 param 中安全取字符串 */
    private String getString(HashMap<String, Object> param, String key) {
        if (param == null) return null;
        Object v = param.get(key);
        return v != null ? v.toString() : null;
    }

    /** 从 param 中安全取整数 */
    private Integer getInteger(HashMap<String, Object> param, String key) {
        if (param == null) return null;
        Object v = param.get(key);
        if (v == null || !StringUtils.isNotBlank(v.toString())) return null;
        return Integer.parseInt(v.toString());
    }

    /** 切换库存商品在售状态（软删除/恢复） */
    @PutMapping("/{id}/status")
    public Result<?> updateStatus(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        Inventory inv = inventoryMapper.selectById(id);
        if (inv == null) return Result.fail("库存记录不存在");

        Integer status = body.get("status") != null ? Integer.parseInt(body.get("status").toString()) : 0;
        inv.setStatus(status);
        inv.setUpdateTime(java.time.LocalDateTime.now());
        inventoryMapper.updateById(inv);
        return Result.success();
    }
}
