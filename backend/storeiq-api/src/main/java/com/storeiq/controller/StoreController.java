package com.storeiq.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.storeiq.common.QueryPageParam;
import com.storeiq.common.Result;
import com.storeiq.entity.*;
import com.storeiq.mapper.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
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

    @Autowired
    private SaleRecordMapper saleRecordMapper;

    @Autowired
    private ProductMapper productMapper;

    @Autowired
    private PurchaseRecordMapper purchaseRecordMapper;

    /** 分页列表 */
    @PostMapping("/list")
    public Result<?> list(@RequestBody QueryPageParam query) {
        HashMap<String, Object> param = query.getParam();
        String name = param != null ? (String) param.get("name") : null;
        String region = param != null ? (String) param.get("region") : null;
        String propertyType = param != null ? (String) param.get("propertyType") : null;
        Object statusObj = param != null ? param.get("status") : null;

        Page<Store> page = new Page<>(query.getPageNum(), query.getPageSize());
        LambdaQueryWrapper<Store> wrapper = new LambdaQueryWrapper<>();

        if (StringUtils.isNotBlank(name)) {
            wrapper.like(Store::getName, name);
        }
        if (StringUtils.isNotBlank(region)) {
            wrapper.eq(Store::getRegion, region);
        }
        if (StringUtils.isNotBlank(propertyType)) {
            wrapper.eq(Store::getPropertyType, propertyType);
        }
        wrapper.orderByDesc(Store::getCreateTime);
        // 未指定状态时返回全部（含停运）：删除（软删除）的店铺才能在列表中看到并可恢复启用
        if (statusObj != null && StringUtils.isNotBlank(statusObj.toString())) {
            wrapper.eq(Store::getStatus, Integer.parseInt(statusObj.toString()));
        }

        IPage<Store> result = storeMapper.selectPage(page, wrapper);

        // 当前页店铺 ID，批量查询销售额和库存（避免 N+1）
        Set<Long> storeIds = result.getRecords().stream().map(Store::getId).collect(Collectors.toSet());
        Map<Long, BigDecimal> salesMap = storeIds.isEmpty() ? Collections.emptyMap()
                : saleRecordMapper.sumAmountByStoreIds(storeIds)
                .stream().collect(Collectors.toMap(
                        m -> (Long) m.get("store_id"),
                        m -> (BigDecimal) m.get("total"),
                        BigDecimal::add));
        Map<Long, Integer> invMap = storeIds.isEmpty() ? Collections.emptyMap()
                : inventoryMapper.sumQuantityByStoreIds(storeIds)
                .stream().collect(Collectors.toMap(
                        m -> (Long) m.get("store_id"),
                        m -> ((Number) m.get("total")).intValue(),
                        Integer::sum));

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

            Integer used = invMap.getOrDefault(store.getId(), 0);
            map.put("used", used);
            map.put("usageRate", store.getCapacity() != null && store.getCapacity() > 0
                    ? Math.round(used * 100.0 / store.getCapacity()) : 0);

            // 销售总额
            map.put("totalSales", salesMap.getOrDefault(store.getId(), BigDecimal.ZERO));

            return map;
        }).collect(Collectors.toList());

        return Result.success(records, result.getTotal());
    }

    /** 地图标记（全量轻量，含销售额/利润） */
    @GetMapping("/map")
    public Result<?> map() {
        // 返回全部店铺（含停运）：大屏柱体以"流动光带/静止变暗"区分运营状态
        List<Store> stores = storeMapper.selectList(null);

        if (stores.isEmpty()) return Result.success(Collections.emptyList());

        Set<Long> storeIds = stores.stream().map(Store::getId).collect(Collectors.toSet());

        // SQL 聚合销售额（只返回需要的店铺）
        Map<Long, BigDecimal> salesMap = saleRecordMapper.sumAmountByStoreIds(storeIds)
                .stream().collect(Collectors.toMap(
                        m -> (Long) m.get("store_id"),
                        m -> (BigDecimal) m.get("total"),
                        BigDecimal::add));

        // SQL 聚合利润（地图柱状图高度依据）
        Map<Long, BigDecimal> profitMap = saleRecordMapper.sumProfitByStoreIds(storeIds)
                .stream().collect(Collectors.toMap(
                        m -> (Long) m.get("store_id"),
                        m -> (BigDecimal) m.get("total"),
                        BigDecimal::add));

        // SQL 聚合库存量（一次查询替代 N 次）
        Map<Long, Integer> invMap = inventoryMapper.sumQuantityByStoreIds(storeIds)
                .stream().collect(Collectors.toMap(
                        m -> (Long) m.get("store_id"),
                        m -> ((Number) m.get("total")).intValue(),
                        Integer::sum));

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
            map.put("contactPerson", store.getContactPerson());
            map.put("totalSales", salesMap.getOrDefault(store.getId(), BigDecimal.ZERO));
            map.put("profit", profitMap.getOrDefault(store.getId(), BigDecimal.ZERO));

            Integer used = invMap.getOrDefault(store.getId(), 0);
            map.put("used", used);
            map.put("usageRate", store.getCapacity() != null && store.getCapacity() > 0
                    ? Math.round(used * 100.0 / store.getCapacity()) : 0);

            return map;
        }).collect(Collectors.toList());

        return Result.success(list);
    }

    /** 店铺详情（含库存概览 + 销售汇总） */
    @GetMapping("/{id}")
    public Result<?> detail(@PathVariable Long id) {
        Store store = storeMapper.selectById(id);
        if (store == null) return Result.fail("店铺不存在");

        Map<String, Object> result = new HashMap<>();
        result.put("store", store);

        // 库存概览
        int used = inventoryMapper.selectList(
                new LambdaQueryWrapper<Inventory>()
                        .eq(Inventory::getStoreId, id)
        ).stream().mapToInt(Inventory::getQuantity).sum();
        result.put("used", used);
        result.put("usageRate", store.getCapacity() > 0 ? Math.round(used * 100.0 / store.getCapacity()) : 0);

        // 销售汇总
        List<SaleRecord> storeSales = saleRecordMapper.selectList(
                new LambdaQueryWrapper<SaleRecord>().eq(SaleRecord::getStoreId, id));
        long totalSales = storeSales.size();
        BigDecimal totalRevenue = storeSales.stream()
                .map(SaleRecord::getTotalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        result.put("totalSales", totalSales);
        result.put("totalRevenue", totalRevenue);

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

    /** 某店铺库存明细（含商品详情 + 在售状态） */
    @GetMapping("/{id}/inventory")
    public Result<?> storeInventory(@PathVariable Long id) {
        List<Inventory> inventoryList = inventoryMapper.selectList(
                new LambdaQueryWrapper<Inventory>()
                        .eq(Inventory::getStoreId, id));

        List<Map<String, Object>> records = inventoryList.stream().map(inv -> {
            Map<String, Object> map = new HashMap<>();
            map.put("id", inv.getId());
            map.put("storeId", inv.getStoreId());
            map.put("productId", inv.getProductId());
            map.put("quantity", inv.getQuantity());
            map.put("status", inv.getStatus() != null ? inv.getStatus() : 1);
            map.put("updateTime", inv.getUpdateTime());

            Product p = productMapper.selectById(inv.getProductId());
            if (p != null) {
                map.put("productName", p.getName());
                map.put("productCode", p.getCode());
                map.put("costPrice", p.getCostPrice());
                map.put("salePrice", p.getSalePrice());
                map.put("categoryId", p.getCategoryId());
                map.put("brand", p.getBrand());
                map.put("model", p.getModel());
                map.put("productStatus", p.getStatus());
            }
            return map;
        }).collect(Collectors.toList());

        return Result.success(records);
    }

    /** 为店铺添加商品（upsert inventory） */
    @PostMapping("/{id}/inventory")
    public Result<?> addInventory(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        Long productId = body.get("productId") != null ? Long.parseLong(body.get("productId").toString()) : null;
        Integer quantity = body.get("quantity") != null ? Integer.parseInt(body.get("quantity").toString()) : 0;

        if (productId == null) return Result.fail("商品ID不能为空");

        // 检查是否已存在
        Inventory exist = inventoryMapper.selectOne(
                new LambdaQueryWrapper<Inventory>()
                        .eq(Inventory::getStoreId, id)
                        .eq(Inventory::getProductId, productId));

        if (exist != null) {
            // 已存在则恢复在售状态并更新数量
            exist.setStatus(1);
            exist.setQuantity(exist.getQuantity() + quantity);
            exist.setUpdateTime(java.time.LocalDateTime.now());
            inventoryMapper.updateById(exist);
        } else {
            Inventory inv = new Inventory();
            inv.setStoreId(id);
            inv.setProductId(productId);
            inv.setQuantity(quantity);
            inv.setStatus(1);
            inv.setUpdateTime(java.time.LocalDateTime.now());
            inventoryMapper.insert(inv);
        }
        return Result.success();
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

    /** 销售趋势（按月聚合销售数量） */
    @GetMapping("/{id}/sales-trend")
    public Result<?> salesTrend(@PathVariable Long id, @RequestParam(defaultValue = "0") int year) {
        if (year == 0) year = java.time.LocalDate.now().getYear();

        // 所有有销售记录的年份
        List<Integer> years = saleRecordMapper.distinctSaleYears(id).stream()
                .map(m -> (Integer) m.get("year"))
                .collect(Collectors.toList());

        // 按月聚合
        List<Map<String, Object>> data = saleRecordMapper.sumQuantityByMonth(id, year);

        Map<String, Object> result = new HashMap<>();
        result.put("years", years);
        result.put("data", data);
        return Result.success(result);
    }

    /** 库存商品的最早/最晚进货日期 */
    @GetMapping("/{id}/inventory/purchase-dates")
    public Result<?> inventoryPurchaseDates(@PathVariable Long id) {
        // 该店铺所有库存商品 ID
        Set<Long> productIds = inventoryMapper.selectList(
                new LambdaQueryWrapper<Inventory>().eq(Inventory::getStoreId, id))
                .stream().map(Inventory::getProductId).collect(Collectors.toSet());

        if (productIds.isEmpty()) return Result.success(Collections.emptyMap());

        // 按商品聚合最早/最晚进货日期
        Map<Long, Map<String, Object>> dateMap = purchaseRecordMapper
                .minMaxPurchaseDateByProductIds(id, productIds).stream()
                .collect(Collectors.toMap(
                        m -> (Long) m.get("product_id"),
                        m -> {
                            Map<String, Object> dates = new HashMap<>();
                            dates.put("earliest", m.get("earliest"));
                            dates.put("latest", m.get("latest"));
                            return dates;
                        }));
        return Result.success(dateMap);
    }
}
