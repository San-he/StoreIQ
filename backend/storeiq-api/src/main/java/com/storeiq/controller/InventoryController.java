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

        LambdaQueryWrapper<Inventory> wrapper = new LambdaQueryWrapper<>();
        if (param != null) {
            String storeId = (String) param.get("storeId");
            String categoryId = (String) param.get("categoryId");
            if (StringUtils.isNotBlank(storeId)) wrapper.eq(Inventory::getStoreId, Long.parseLong(storeId));
        }
        wrapper.orderByAsc(Inventory::getStoreId).orderByAsc(Inventory::getProductId);

        // 全量加载（库存表通常不大）
        List<Inventory> all = inventoryMapper.selectList(wrapper);

        // 筛选分类
        if (param != null && StringUtils.isNotBlank((String) param.get("categoryId"))) {
            Long catId = Long.parseLong((String) param.get("categoryId"));
            all = all.stream().filter(inv -> {
                Product p = productMapper.selectById(inv.getProductId());
                return p != null && catId.equals(p.getCategoryId());
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
}
