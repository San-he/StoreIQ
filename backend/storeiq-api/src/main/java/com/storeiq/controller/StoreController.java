package com.storeiq.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.storeiq.common.QueryPageParam;
import com.storeiq.common.Result;
import com.storeiq.entity.Store;
import com.storeiq.mapper.InventoryMapper;
import com.storeiq.mapper.StoreMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 店铺管理控制器
 */
@RestController
@RequestMapping("/stores")
public class StoreController {

    @Autowired
    private StoreMapper storeMapper;

    @Autowired
    private InventoryMapper inventoryMapper;

    /** 分页列表 */
    @PostMapping("/list")
    public Result<?> list(@RequestBody QueryPageParam query) {
        HashMap<String, Object> param = query.getParam();
        String name = param != null ? (String) param.get("name") : null;
        String region = param != null ? (String) param.get("region") : null;

        Page<Store> page = new Page<>(query.getPageNum(), query.getPageSize());
        LambdaQueryWrapper<Store> wrapper = new LambdaQueryWrapper<>();

        if (StringUtils.isNotBlank(name)) {
            wrapper.like(Store::getName, name);
        }
        if (StringUtils.isNotBlank(region)) {
            wrapper.eq(Store::getRegion, region);
        }
        wrapper.orderByDesc(Store::getCreateTime);
        wrapper.eq(Store::getStatus, 1);

        IPage<Store> result = storeMapper.selectPage(page, wrapper);

        // 计算已用容量
        List<Map<String, Object>> records = result.getRecords().stream().map(store -> {
            Map<String, Object> map = new HashMap<>();
            map.put("id", store.getId());
            map.put("name", store.getName());
            map.put("code", store.getCode());
            map.put("address", store.getAddress());
            map.put("longitude", store.getLongitude());
            map.put("latitude", store.getLatitude());
            map.put("region", store.getRegion());
            map.put("capacity", store.getCapacity());
            map.put("propertyType", store.getPropertyType());
            map.put("monthlyRent", store.getMonthlyRent());
            map.put("purchaseCost", store.getPurchaseCost());
            map.put("contactPerson", store.getContactPerson());
            map.put("contactPhone", store.getContactPhone());
            map.put("status", store.getStatus());
            map.put("description", store.getDescription());

            // 计算已用容量
            Integer used = inventoryMapper.selectList(
                    new LambdaQueryWrapper<com.storeiq.entity.Inventory>()
                            .eq(com.storeiq.entity.Inventory::getStoreId, store.getId())
            ).stream().mapToInt(com.storeiq.entity.Inventory::getQuantity).sum();
            map.put("used", used);
            map.put("usageRate", store.getCapacity() != null && store.getCapacity() > 0
                    ? Math.round(used * 100.0 / store.getCapacity()) : 0);

            return map;
        }).collect(Collectors.toList());

        return Result.success(records, result.getTotal());
    }

    /** 地图标记（全量轻量） */
    @GetMapping("/map")
    public Result<?> map() {
        List<Store> stores = storeMapper.selectList(
                new LambdaQueryWrapper<Store>().eq(Store::getStatus, 1));

        List<Map<String, Object>> list = stores.stream().map(store -> {
            Map<String, Object> map = new HashMap<>();
            map.put("id", store.getId());
            map.put("name", store.getName());
            map.put("code", store.getCode());
            map.put("longitude", store.getLongitude());
            map.put("latitude", store.getLatitude());
            map.put("region", store.getRegion());
            map.put("capacity", store.getCapacity());
            map.put("propertyType", store.getPropertyType());

            // 已用容量
            Integer used = inventoryMapper.selectList(
                    new LambdaQueryWrapper<com.storeiq.entity.Inventory>()
                            .eq(com.storeiq.entity.Inventory::getStoreId, store.getId())
            ).stream().mapToInt(com.storeiq.entity.Inventory::getQuantity).sum();
            map.put("used", used);
            map.put("usageRate", store.getCapacity() != null && store.getCapacity() > 0
                    ? Math.round(used * 100.0 / store.getCapacity()) : 0);

            return map;
        }).collect(Collectors.toList());

        return Result.success(list);
    }

    /** 店铺详情（含库存概览） */
    @GetMapping("/{id}")
    public Result<?> detail(@PathVariable Long id) {
        Store store = storeMapper.selectById(id);
        if (store == null) return Result.fail("店铺不存在");

        Map<String, Object> result = new HashMap<>();
        result.put("store", store);

        // 库存概览
        int used = inventoryMapper.selectList(
                new LambdaQueryWrapper<com.storeiq.entity.Inventory>()
                        .eq(com.storeiq.entity.Inventory::getStoreId, id)
        ).stream().mapToInt(com.storeiq.entity.Inventory::getQuantity).sum();
        result.put("used", used);
        result.put("usageRate", store.getCapacity() > 0 ? Math.round(used * 100.0 / store.getCapacity()) : 0);

        return Result.success(result);
    }

    /** 新增 */
    @PostMapping
    public Result<?> save(@RequestBody Store store) {
        store.setCreateTime(java.time.LocalDateTime.now());
        store.setUpdateTime(java.time.LocalDateTime.now());
        return storeMapper.insert(store) > 0 ? Result.success() : Result.fail();
    }

    /** 编辑 */
    @PutMapping("/{id}")
    public Result<?> update(@PathVariable Long id, @RequestBody Store store) {
        store.setId(id);
        store.setUpdateTime(java.time.LocalDateTime.now());
        return storeMapper.updateById(store) > 0 ? Result.success() : Result.fail();
    }

    /** 删除（软删除） */
    @DeleteMapping("/{id}")
    public Result<?> delete(@PathVariable Long id) {
        Store store = storeMapper.selectById(id);
        if (store == null) return Result.fail("店铺不存在");
        store.setStatus(0);
        store.setUpdateTime(java.time.LocalDateTime.now());
        return storeMapper.updateById(store) > 0 ? Result.success() : Result.fail();
    }

    /** 某店铺库存明细 */
    @GetMapping("/{id}/inventory")
    public Result<?> storeInventory(@PathVariable Long id) {
        List<com.storeiq.entity.Inventory> inventoryList = inventoryMapper.selectList(
                new LambdaQueryWrapper<com.storeiq.entity.Inventory>()
                        .eq(com.storeiq.entity.Inventory::getStoreId, id));

        return Result.success(inventoryList);
    }

    /** 区域列表 */
    @GetMapping("/regions")
    public Result<?> regions() {
        List<Store> stores = storeMapper.selectList(new LambdaQueryWrapper<Store>().eq(Store::getStatus, 1));
        List<String> regions = stores.stream()
                .map(Store::getRegion)
                .filter(Objects::nonNull)
                .distinct()
                .sorted()
                .collect(Collectors.toList());
        return Result.success(regions);
    }
}
