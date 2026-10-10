package com.storeiq.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.storeiq.common.Result;
import com.storeiq.entity.*;
import com.storeiq.mapper.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * 数据统计控制器 — 为 ECharts 提供数据，支持时间/商品/店铺筛选
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

    /** 解析逗号分隔的 ID 字符串为列表 */
    private List<Long> parseIds(String ids) {
        if (ids == null || ids.trim().isEmpty()) return null;
        return Stream.of(ids.split(",")).map(String::trim).filter(s -> !s.isEmpty()).map(Long::parseLong).collect(Collectors.toList());
    }

    /** 构建销售记录通用查询 */
    private LambdaQueryWrapper<SaleRecord> buildSaleWrapper(List<Long> productIds, List<Long> storeIds, String startDate, String endDate) {
        LambdaQueryWrapper<SaleRecord> w = new LambdaQueryWrapper<>();
        if (productIds != null && !productIds.isEmpty()) w.in(SaleRecord::getProductId, productIds);
        if (storeIds != null && !storeIds.isEmpty()) w.in(SaleRecord::getStoreId, storeIds);
        if (startDate != null && !startDate.isEmpty())
            w.ge(SaleRecord::getCreateTime, LocalDate.parse(startDate).atStartOfDay());
        if (endDate != null && !endDate.isEmpty())
            w.le(SaleRecord::getCreateTime, LocalDate.parse(endDate).atTime(23, 59, 59));
        return w;
    }

    /** 首页概览卡片 */
    @GetMapping("/overview")
    public Result<?> overview(
            @RequestParam(required = false) String productIds,
            @RequestParam(required = false) String storeIds,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {

        Map<String, Object> data = new HashMap<>();
        data.put("storeCount", storeMapper.selectCount(null));
        data.put("productCount", productMapper.selectCount(null));

        List<SaleRecord> sales = saleMapper.selectList(buildSaleWrapper(parseIds(productIds), parseIds(storeIds), startDate, endDate));
        BigDecimal totalAmount = sales.stream().map(SaleRecord::getTotalAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalProfit = sales.stream().map(SaleRecord::getProfit).reduce(BigDecimal.ZERO, BigDecimal::add);

        data.put("monthSales", totalAmount);
        data.put("monthProfit", totalProfit);
        data.put("saleCount", sales.size());
        return Result.success(data);
    }

    /** 月度销售额趋势 */
    @GetMapping("/sales/monthly")
    public Result<?> salesMonthly(
            @RequestParam(required = false) String productIds,
            @RequestParam(required = false) String storeIds,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {

        List<Long> pIds = parseIds(productIds);
        List<Long> sIds = parseIds(storeIds);

        int year = LocalDate.now().getYear();
        if (startDate != null && !startDate.isEmpty()) year = LocalDate.parse(startDate).getYear();

        List<Map<String, Object>> result = new ArrayList<>();
        String[] months = {"1月","2月","3月","4月","5月","6月","7月","8月","9月","10月","11月","12月"};

        for (int m = 1; m <= 12; m++) {
            LocalDateTime start = LocalDateTime.of(year, m, 1, 0, 0);
            LocalDateTime end = m < 12
                    ? LocalDateTime.of(year, m + 1, 1, 0, 0)
                    : LocalDateTime.of(year + 1, 1, 1, 0, 0);

            // 合并全局筛选与月份区间
            LocalDateTime actualStart = start;
            LocalDateTime actualEnd = end;
            if (startDate != null && !startDate.isEmpty()) {
                LocalDateTime filterStart = LocalDate.parse(startDate).atStartOfDay();
                if (filterStart.isAfter(actualStart)) actualStart = filterStart;
            }
            if (endDate != null && !endDate.isEmpty()) {
                LocalDateTime filterEnd = LocalDate.parse(endDate).atTime(23, 59, 59);
                if (filterEnd.isBefore(actualEnd)) actualEnd = filterEnd;
            }

            LambdaQueryWrapper<SaleRecord> w = new LambdaQueryWrapper<>();
            w.ge(SaleRecord::getCreateTime, actualStart).lt(SaleRecord::getCreateTime, actualEnd);
            if (pIds != null && !pIds.isEmpty()) w.in(SaleRecord::getProductId, pIds);
            if (sIds != null && !sIds.isEmpty()) w.in(SaleRecord::getStoreId, sIds);

            List<SaleRecord> records = saleMapper.selectList(w);
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
    public Result<?> salesQuarterly(
            @RequestParam(required = false) String productIds,
            @RequestParam(required = false) String storeIds,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {

        List<Long> pIds = parseIds(productIds);
        List<Long> sIds = parseIds(storeIds);

        int year = LocalDate.now().getYear();
        if (startDate != null && !startDate.isEmpty()) year = LocalDate.parse(startDate).getYear();

        List<Map<String, Object>> result = new ArrayList<>();
        String[] quarters = {"Q1","Q2","Q3","Q4"};
        int[][] ranges = {{1,3},{4,6},{7,9},{10,12}};

        for (int q = 0; q < 4; q++) {
            LocalDateTime start = LocalDateTime.of(year, ranges[q][0], 1, 0, 0);
            LocalDateTime end = q < 3
                    ? LocalDateTime.of(year, ranges[q][1] + 1, 1, 0, 0)
                    : LocalDateTime.of(year + 1, 1, 1, 0, 0);

            LambdaQueryWrapper<SaleRecord> w = new LambdaQueryWrapper<>();
            w.ge(SaleRecord::getCreateTime, start).lt(SaleRecord::getCreateTime, end);
            if (pIds != null && !pIds.isEmpty()) w.in(SaleRecord::getProductId, pIds);
            if (sIds != null && !sIds.isEmpty()) w.in(SaleRecord::getStoreId, sIds);

            List<SaleRecord> records = saleMapper.selectList(w);
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
    public Result<?> salesByStore(
            @RequestParam(required = false) String productIds,
            @RequestParam(required = false) String storeIds,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {

        List<Long> pIds = parseIds(productIds);
        List<Long> sIds = parseIds(storeIds);

        List<Store> stores = storeMapper.selectList(null);
        List<Map<String, Object>> result = new ArrayList<>();

        for (Store store : stores) {
            if (sIds != null && !sIds.contains(store.getId())) continue;

            LambdaQueryWrapper<SaleRecord> w = new LambdaQueryWrapper<>();
            w.eq(SaleRecord::getStoreId, store.getId());
            if (pIds != null && !pIds.isEmpty()) w.in(SaleRecord::getProductId, pIds);
            if (startDate != null && !startDate.isEmpty())
                w.ge(SaleRecord::getCreateTime, LocalDate.parse(startDate).atStartOfDay());
            if (endDate != null && !endDate.isEmpty())
                w.le(SaleRecord::getCreateTime, LocalDate.parse(endDate).atTime(23, 59, 59));

            List<SaleRecord> records = saleMapper.selectList(w);
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
    public Result<?> salesByCategory(
            @RequestParam(required = false) String productIds,
            @RequestParam(required = false) String storeIds,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {

        List<Long> pIds = parseIds(productIds);
        List<Long> sIds = parseIds(storeIds);

        List<ProductCategory> categories = categoryMapper.selectList(null);
        List<SaleRecord> allSales = saleMapper.selectList(buildSaleWrapper(null, sIds, startDate, endDate));
        List<Map<String, Object>> result = new ArrayList<>();

        for (ProductCategory cat : categories) {
            List<Long> catProductIds = productMapper.selectList(
                    new LambdaQueryWrapper<Product>().eq(Product::getCategoryId, cat.getId()))
                    .stream().map(Product::getId).collect(Collectors.toList());

            // 如果指定了 productIds，只统计这些商品
            if (pIds != null) catProductIds.retainAll(pIds);

            BigDecimal amount = allSales.stream()
                    .filter(s -> catProductIds.contains(s.getProductId()))
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
    public Result<?> profitTrend(
            @RequestParam(required = false) String productIds,
            @RequestParam(required = false) String storeIds,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {

        List<Long> pIds = parseIds(productIds);
        List<Long> sIds = parseIds(storeIds);

        int year = LocalDate.now().getYear();
        if (startDate != null && !startDate.isEmpty()) year = LocalDate.parse(startDate).getYear();

        List<Map<String, Object>> result = new ArrayList<>();
        String[] months = {"1月","2月","3月","4月","5月","6月","7月","8月","9月","10月","11月","12月"};

        for (int m = 1; m <= 12; m++) {
            LocalDateTime start = LocalDateTime.of(year, m, 1, 0, 0);
            LocalDateTime end = m < 12
                    ? LocalDateTime.of(year, m + 1, 1, 0, 0)
                    : LocalDateTime.of(year + 1, 1, 1, 0, 0);

            LambdaQueryWrapper<SaleRecord> w = new LambdaQueryWrapper<>();
            w.ge(SaleRecord::getCreateTime, start).lt(SaleRecord::getCreateTime, end);
            if (pIds != null && !pIds.isEmpty()) w.in(SaleRecord::getProductId, pIds);
            if (sIds != null && !sIds.isEmpty()) w.in(SaleRecord::getStoreId, sIds);

            List<SaleRecord> records = saleMapper.selectList(w);
            Map<String, Object> item = new HashMap<>();
            item.put("month", months[m - 1]);
            BigDecimal amount = records.stream().map(SaleRecord::getTotalAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal profit = records.stream().map(SaleRecord::getProfit).reduce(BigDecimal.ZERO, BigDecimal::add);
            item.put("amount", amount);
            item.put("profit", profit);
            item.put("profitRate", amount.compareTo(BigDecimal.ZERO) > 0
                    ? profit.multiply(new BigDecimal(100)).divide(amount, 2, RoundingMode.HALF_UP) : 0);
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
        Map<String, Object> resp = new HashMap<>();
        resp.put("categories", categories.stream().map(ProductCategory::getName).collect(Collectors.toList()));
        resp.put("data", result);
        return Result.success(resp);
    }

    /** 商品销售统计（按店铺/时间/分类筛选，含趋势） */
    @GetMapping("/product-sales")
    public Result<?> productSales(
            @RequestParam(required = false) String storeIds,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize) {

        List<Long> sIds = parseIds(storeIds);
        // product-sales 目前仅支持单店铺聚合，取第一个 ID
        Long storeId = (sIds != null && !sIds.isEmpty()) ? sIds.get(0) : null;

        List<Map<String, Object>> aggRows = saleMapper.sumAmountByProduct(storeId);
        Set<Long> filteredProductIds = null;
        if (categoryId != null) {
            filteredProductIds = productMapper.selectList(
                    new LambdaQueryWrapper<Product>().eq(Product::getCategoryId, categoryId))
                    .stream().map(Product::getId).collect(Collectors.toSet());
        }

        LocalDate today = LocalDate.now();
        LocalDate monthStart = today.withDayOfMonth(1);
        LocalDateTime e = monthStart.atStartOfDay().minusSeconds(1);
        String prevStart = e.toLocalDate().minusMonths(1).withDayOfMonth(1).atStartOfDay().toString().replace("T", " ");
        String prevEnd = e.toString().replace("T", " ");

        Map<Long, BigDecimal> prevAmountMap = saleMapper
                .sumAmountByProductAndDate(storeId, prevStart, prevEnd)
                .stream().collect(Collectors.toMap(
                        m -> (Long) m.get("product_id"),
                        m -> (BigDecimal) m.get("amount"),
                        BigDecimal::add));

        List<Map<String, Object>> result = new ArrayList<>();
        for (Map<String, Object> row : aggRows) {
            Long pid = (Long) row.get("product_id");
            if (categoryId != null && !filteredProductIds.contains(pid)) continue;

            Product p = productMapper.selectById(pid);
            if (p == null) continue;

            int totalQuantity = ((Number) row.get("qty")).intValue();
            BigDecimal totalAmount = (BigDecimal) row.get("amount");

            BigDecimal prevAmount = prevAmountMap.getOrDefault(pid, BigDecimal.ZERO);
            String trend;
            if (prevAmount.compareTo(BigDecimal.ZERO) == 0) {
                trend = totalAmount.compareTo(BigDecimal.ZERO) > 0 ? "up" : "stable";
            } else if (totalAmount.compareTo(prevAmount) > 0) {
                trend = "up";
            } else if (totalAmount.compareTo(prevAmount) < 0) {
                trend = "down";
            } else {
                trend = "stable";
            }

            Map<String, Object> item = new HashMap<>();
            item.put("productId", pid);
            item.put("productName", p.getName());
            item.put("productCode", p.getCode());
            item.put("totalQuantity", totalQuantity);
            item.put("totalAmount", totalAmount);
            item.put("trend", trend);
            item.put("costPrice", p.getCostPrice());
            item.put("salePrice", p.getSalePrice());
            item.put("categoryId", p.getCategoryId());

            ProductCategory cat = categoryMapper.selectById(p.getCategoryId());
            item.put("categoryName", cat != null ? cat.getName() : "-");

            result.add(item);
        }

        result.sort((a, b) -> ((BigDecimal) b.get("totalAmount")).compareTo((BigDecimal) a.get("totalAmount")));

        long total = result.size();
        int from = (pageNum - 1) * pageSize;
        int to = Math.min(from + pageSize, result.size());
        List<Map<String, Object>> paged = from < total ? result.subList(from, to) : new ArrayList<>();

        return Result.success(paged, total);
    }
}
