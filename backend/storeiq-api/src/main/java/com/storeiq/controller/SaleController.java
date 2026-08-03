package com.storeiq.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.storeiq.common.QueryPageParam;
import com.storeiq.common.Result;
import com.storeiq.dto.SaleRequest;
import com.storeiq.entity.Inventory;
import com.storeiq.entity.Product;
import com.storeiq.entity.SaleRecord;
import com.storeiq.mapper.InventoryMapper;
import com.storeiq.mapper.ProductMapper;
import com.storeiq.mapper.SaleRecordMapper;
import com.storeiq.mapper.StoreMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 销售管理控制器
 */
@RestController
@RequestMapping("/sales")
public class SaleController {

    @Autowired
    private SaleRecordMapper saleMapper;

    @Autowired
    private ProductMapper productMapper;

    @Autowired
    private StoreMapper storeMapper;

    @Autowired
    private InventoryMapper inventoryMapper;

    /** 分页列表 */
    @PostMapping("/list")
    public Result<?> list(@RequestBody QueryPageParam query) {
        HashMap<String, Object> param = query.getParam();
        Page<SaleRecord> page = new Page<>(query.getPageNum(), query.getPageSize());
        LambdaQueryWrapper<SaleRecord> wrapper = new LambdaQueryWrapper<>();

        if (param != null) {
            String productId = (String) param.get("productId");
            String storeId = (String) param.get("storeId");
            if (StringUtils.isNotBlank(productId)) wrapper.eq(SaleRecord::getProductId, Long.parseLong(productId));
            if (StringUtils.isNotBlank(storeId)) wrapper.eq(SaleRecord::getStoreId, Long.parseLong(storeId));
        }
        wrapper.orderByDesc(SaleRecord::getCreateTime);

        IPage<SaleRecord> result = saleMapper.selectPage(page, wrapper);

        List<Map<String, Object>> enriched = result.getRecords().stream().map(r -> {
            Map<String, Object> map = new HashMap<>();
            map.put("id", r.getId());
            map.put("productId", r.getProductId());
            map.put("storeId", r.getStoreId());
            map.put("quantity", r.getQuantity());
            map.put("unitPrice", r.getUnitPrice());
            map.put("totalAmount", r.getTotalAmount());
            map.put("unitCost", r.getUnitCost());
            map.put("profit", r.getProfit());
            map.put("customerName", r.getCustomerName());
            map.put("remark", r.getRemark());
            map.put("createTime", r.getCreateTime());

            Product p = productMapper.selectById(r.getProductId());
            map.put("productName", p != null ? p.getName() : "-");
            map.put("productCode", p != null ? p.getCode() : "-");

            com.storeiq.entity.Store s = storeMapper.selectById(r.getStoreId());
            map.put("storeName", s != null ? s.getName() : "-");
            return map;
        }).collect(Collectors.toList());

        return Result.success(enriched, result.getTotal());
    }

    /** 新增销售（校验库存+扣减） */
    @PostMapping
    @Transactional
    public Result<?> save(@Valid @RequestBody SaleRequest req, HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");

        // 校验库存
        Inventory inv = inventoryMapper.selectOne(
                new LambdaQueryWrapper<Inventory>()
                        .eq(Inventory::getStoreId, req.getStoreId())
                        .eq(Inventory::getProductId, req.getProductId()));
        if (inv == null || inv.getQuantity() < req.getQuantity()) {
            return Result.fail("库存不足，当前库存：" + (inv != null ? inv.getQuantity() : 0));
        }

        // 获取商品成本
        Product product = productMapper.selectById(req.getProductId());
        BigDecimal unitCost = product != null && product.getCostPrice() != null
                ? product.getCostPrice() : BigDecimal.ZERO;

        // 创建销售记录
        SaleRecord record = new SaleRecord();
        record.setProductId(req.getProductId());
        record.setStoreId(req.getStoreId());
        record.setQuantity(req.getQuantity());
        record.setUnitPrice(req.getUnitPrice());
        record.setTotalAmount(req.getUnitPrice().multiply(new BigDecimal(req.getQuantity())));
        record.setUnitCost(unitCost);
        record.setProfit(req.getUnitPrice().subtract(unitCost).multiply(new BigDecimal(req.getQuantity())));
        record.setCustomerName(req.getCustomerName());
        record.setOperatorId(userId);
        record.setRemark(req.getRemark());
        record.setCreateTime(LocalDateTime.now());
        saleMapper.insert(record);

        // 扣减库存
        inv.setQuantity(inv.getQuantity() - req.getQuantity());
        inv.setUpdateTime(LocalDateTime.now());
        inventoryMapper.updateById(inv);

        return Result.success("出库成功");
    }

    /** 扫码录入：根据 product.code 查询商品 */
    @GetMapping("/scan/{code}")
    public Result<?> scan(@PathVariable String code) {
        Product product = productMapper.selectOne(
                new LambdaQueryWrapper<Product>().eq(Product::getCode, code));
        if (product == null) return Result.fail("未找到该商品");

        Map<String, Object> map = new HashMap<>();
        map.put("id", product.getId());
        map.put("name", product.getName());
        map.put("code", product.getCode());
        map.put("salePrice", product.getSalePrice());
        map.put("costPrice", product.getCostPrice());
        map.put("unit", product.getUnit());
        return Result.success(map);
    }

    /** 详情 */
    @GetMapping("/{id}")
    public Result<?> detail(@PathVariable Long id) {
        SaleRecord record = saleMapper.selectById(id);
        if (record == null) return Result.fail("记录不存在");

        Product p = productMapper.selectById(record.getProductId());
        com.storeiq.entity.Store s = storeMapper.selectById(record.getStoreId());

        Map<String, Object> map = new HashMap<>();
        map.put("record", record);
        map.put("productName", p != null ? p.getName() : "-");
        map.put("storeName", s != null ? s.getName() : "-");
        return Result.success(map);
    }
}
