package com.storeiq.controller;

import com.storeiq.common.Result;
import com.storeiq.entity.*;
import com.storeiq.mapper.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 数据统计控制器 — 为 ECharts 提供数据
 */
@RestController
@RequestMapping("/stats")
public class StatsController {

    @Autowired private SaleRecordMapper saleMapper;
    @Autowired private PurchaseRecordMapper purchaseMapper;
    @Autowired private StoreMapper storeMapper;
    @Autowired private ProductMapper productMapper;
    @Autowired private InventoryMapper inventoryMapper;
    @Autowired private CategoryMapper categoryMapper;

    /** 首页概览卡片 */
    @GetMapping("/overview")
    public Result<?> overview() {
        Map<String, Object> data = new HashMap<>();

        // 店铺数
        data.put("storeCount", storeMapper.selectCount(null));

        // 商品数
        data.put("productCount", productMapper.selectCount(null));

        // 本月销售额
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.DAY_OF_MONTH, 1);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        java.time.LocalDateTime monthStart = java.time.LocalDateTime.ofInstant(cal.toInstant(), java.time.ZoneId.systemDefault());

        List<SaleRecord> monthSales = saleMapper.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<SaleRecord>()
                        .ge(SaleRecord::getCreateTime, monthStart));
        BigDecimal monthAmount = monthSales.stream()
                .map(SaleRecord::getTotalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        data.put("monthSales", monthAmount);

        // 本月利润
        BigDecimal monthProfit = monthSales.stream()
                .map(SaleRecord::getProfit)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        data.put("monthProfit", monthProfit);

        return Result.success(data);
    }

    /** 月度销售额趋势 */
    @GetMapping("/sales/monthly")
    public Result<?> salesMonthly(@RequestParam(defaultValue = "2026") int year) {
        List<Map<String, Object>> result = new ArrayList<>();
        String[] months = {"1月","2月","3月","4月","5月","6月","7月","8月","9月","10月","11月","12月"};

        for (int m = 1; m <= 12; m++) {
            java.time.LocalDateTime start = java.time.LocalDateTime.of(year, m, 1, 0, 0);
            java.time.LocalDateTime end = m < 12
                    ? java.time.LocalDateTime.of(year, m + 1, 1, 0, 0)
                    : java.time.LocalDateTime.of(year + 1, 1, 1, 0, 0);

            List<SaleRecord> records = saleMapper.selectList(
                    new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<SaleRecord>()
                            .ge(SaleRecord::getCreateTime, start)
                            .lt(SaleRecord::getCreateTime, end));

            BigDecimal amount = records.stream().map(SaleRecord::getTotalAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal profit = records.stream().map(SaleRecord::getProfit).reduce(BigDecimal.ZERO, BigDecimal::add);

            Map<String, Object> item = new HashMap<>();
            item.put("month", months[m - 1]);
            item.put("amount", amount);
            item.put("profit", profit);
            item.put("count", records.size());
            result.add(item);
        }
        return Result.success(result);
    }

    /** 季度销售额 */
    @GetMapping("/sales/quarterly")
    public Result<?> salesQuarterly(@RequestParam(defaultValue = "2026") int year) {
        List<Map<String, Object>> result = new ArrayList<>();
        String[] quarters = {"Q1","Q2","Q3","Q4"};
        int[][] ranges = {{1,3},{4,6},{7,9},{10,12}};

        for (int q = 0; q < 4; q++) {
            java.time.LocalDateTime start = java.time.LocalDateTime.of(year, ranges[q][0], 1, 0, 0);
            java.time.LocalDateTime end = q < 3
                    ? java.time.LocalDateTime.of(year, ranges[q][1] + 1, 1, 0, 0)
                    : java.time.LocalDateTime.of(year + 1, 1, 1, 0, 0);

            List<SaleRecord> records = saleMapper.selectList(
                    new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<SaleRecord>()
                            .ge(SaleRecord::getCreateTime, start)
                            .lt(SaleRecord::getCreateTime, end));

            Map<String, Object> item = new HashMap<>();
            item.put("quarter", quarters[q]);
            item.put("amount", records.stream().map(SaleRecord::getTotalAmount).reduce(BigDecimal.ZERO, BigDecimal::add));
            item.put("profit", records.stream().map(SaleRecord::getProfit).reduce(BigDecimal.ZERO, BigDecimal::add));
            result.add(item);
        }
        return Result.success(result);
    }

    /** 各店铺销售额对比 */
    @GetMapping("/sales/by-store")
    public Result<?> salesByStore() {
        List<Store> stores = storeMapper.selectList(null);
        List<Map<String, Object>> result = new ArrayList<>();

        for (Store store : stores) {
            List<SaleRecord> records = saleMapper.selectList(
                    new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<SaleRecord>()
                            .eq(SaleRecord::getStoreId, store.getId()));
            Map<String, Object> item = new HashMap<>();
            item.put("storeName", store.getName());
            item.put("region", store.getRegion());
            item.put("amount", records.stream().map(SaleRecord::getTotalAmount).reduce(BigDecimal.ZERO, BigDecimal::add));
            item.put("profit", records.stream().map(SaleRecord::getProfit).reduce(BigDecimal.ZERO, BigDecimal::add));
            result.add(item);
        }
        return Result.success(result);
    }

    /** 各分类销量占比 */
    @GetMapping("/sales/by-category")
    public Result<?> salesByCategory() {
        List<ProductCategory> categories = categoryMapper.selectList(null);
        List<SaleRecord> allSales = saleMapper.selectList(null);
        List<Map<String, Object>> result = new ArrayList<>();

        for (ProductCategory cat : categories) {
            // 找出该分类的所有 productId
            List<Long> productIds = productMapper.selectList(
                    new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<Product>()
                            .eq(Product::getCategoryId, cat.getId()))
                    .stream().map(Product::getId).collect(Collectors.toList());

            BigDecimal amount = allSales.stream()
                    .filter(s -> productIds.contains(s.getProductId()))
                    .map(SaleRecord::getTotalAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            Map<String, Object> item = new HashMap<>();
            item.put("categoryName", cat.getName());
            item.put("amount", amount);
            result.add(item);
        }
        return Result.success(result);
    }

    /** 利润趋势 */
    @GetMapping("/profit/trend")
    public Result<?> profitTrend(@RequestParam(defaultValue = "2026") int year) {
        List<Map<String, Object>> result = new ArrayList<>();
        String[] months = {"1月","2月","3月","4月","5月","6月","7月","8月","9月","10月","11月","12月"};

        for (int m = 1; m <= 12; m++) {
            java.time.LocalDateTime start = java.time.LocalDateTime.of(year, m, 1, 0, 0);
            java.time.LocalDateTime end = m < 12
                    ? java.time.LocalDateTime.of(year, m + 1, 1, 0, 0)
                    : java.time.LocalDateTime.of(year + 1, 1, 1, 0, 0);

            List<SaleRecord> records = saleMapper.selectList(
                    new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<SaleRecord>()
                            .ge(SaleRecord::getCreateTime, start)
                            .lt(SaleRecord::getCreateTime, end));

            Map<String, Object> item = new HashMap<>();
            item.put("month", months[m - 1]);
            item.put("amount", records.stream().map(SaleRecord::getTotalAmount).reduce(BigDecimal.ZERO, BigDecimal::add));
            item.put("profit", records.stream().map(SaleRecord::getProfit).reduce(BigDecimal.ZERO, BigDecimal::add));
            // 利润率
            BigDecimal amount = (BigDecimal) item.get("amount");
            BigDecimal profit = (BigDecimal) item.get("profit");
            item.put("profitRate", amount.compareTo(BigDecimal.ZERO) > 0
                    ? profit.multiply(new BigDecimal(100)).divide(amount, 2, java.math.RoundingMode.HALF_UP) : 0);

            result.add(item);
        }
        return Result.success(result);
    }

    /** 各店铺库存分布 */
    @GetMapping("/inventory/distribution")
    public Result<?> inventoryDistribution() {
        List<Store> stores = storeMapper.selectList(null);
        List<ProductCategory> categories = categoryMapper.selectList(null);
        List<Inventory> allInv = inventoryMapper.selectList(null);
        List<Product> allProducts = productMapper.selectList(null);

        // storeName -> { categoryName -> quantity }
        List<Map<String, Object>> result = new ArrayList<>();
        for (Store store : stores) {
            Map<String, Object> item = new HashMap<>();
            item.put("storeName", store.getName());

            int total = 0;
            for (ProductCategory cat : categories) {
                List<Long> productIds = allProducts.stream()
                        .filter(p -> cat.getId().equals(p.getCategoryId()))
                        .map(Product::getId).collect(Collectors.toList());

                int qty = allInv.stream()
                        .filter(inv -> inv.getStoreId().equals(store.getId()) && productIds.contains(inv.getProductId()))
                        .mapToInt(Inventory::getQuantity).sum();
                item.put(cat.getName(), qty);
                total += qty;
            }
            item.put("total", total);
            result.add(item);
        }
        // 附加分类列表供前端使用
        Map<String, Object> resp = new HashMap<>();
        resp.put("categories", categories.stream().map(ProductCategory::getName).collect(Collectors.toList()));
        resp.put("data", result);
        return Result.success(resp);
    }
}
