package com.storeiq.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.storeiq.common.QueryPageParam;
import com.storeiq.common.Result;
import com.storeiq.dto.PurchaseRequest;
import com.storeiq.entity.Inventory;
import com.storeiq.entity.Product;
import com.storeiq.entity.PurchaseRecord;
import com.storeiq.mapper.InventoryMapper;
import com.storeiq.mapper.ProductMapper;
import com.storeiq.mapper.PurchaseRecordMapper;
import com.storeiq.mapper.StoreMapper;
import com.storeiq.mapper.SupplierMapper;
import com.storeiq.mapper.UserMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 进货管理控制器
 */
@RestController
@RequestMapping("/purchases")
public class PurchaseController {

    @Autowired
    private PurchaseRecordMapper purchaseMapper;

    @Autowired
    private ProductMapper productMapper;

    @Autowired
    private StoreMapper storeMapper;

    @Autowired
    private InventoryMapper inventoryMapper;

    @Autowired
    private SupplierMapper supplierMapper;

    @Autowired
    private UserMapper userMapper;

    /** 分页列表 */
    @PostMapping("/list")
    public Result<?> list(@RequestBody QueryPageParam query) {
        HashMap<String, Object> param = query.getParam();
        Page<PurchaseRecord> page = new Page<>(query.getPageNum(), query.getPageSize());
        LambdaQueryWrapper<PurchaseRecord> wrapper = new LambdaQueryWrapper<>();

        if (param != null) {
            String productId = (String) param.get("productId");
            String storeId = (String) param.get("storeId");
            if (StringUtils.isNotBlank(productId)) wrapper.eq(PurchaseRecord::getProductId, Long.parseLong(productId));
            if (StringUtils.isNotBlank(storeId)) wrapper.eq(PurchaseRecord::getStoreId, Long.parseLong(storeId));
        }
        wrapper.orderByDesc(PurchaseRecord::getCreateTime);

        IPage<PurchaseRecord> result = purchaseMapper.selectPage(page, wrapper);

        List<Map<String, Object>> enriched = result.getRecords().stream().map(r -> {
            Map<String, Object> map = new HashMap<>();
            map.put("id", r.getId());
            map.put("batchNo", r.getBatchNo());
            map.put("productId", r.getProductId());
            map.put("storeId", r.getStoreId());
            map.put("supplierId", r.getSupplierId());
            map.put("quantity", r.getQuantity());
            map.put("unitCost", r.getUnitCost());
            map.put("totalCost", r.getTotalCost());
            map.put("remark", r.getRemark());
            map.put("createTime", r.getCreateTime());

            Product p = productMapper.selectById(r.getProductId());
            map.put("productName", p != null ? p.getName() : "-");
            map.put("productCode", p != null ? p.getCode() : "-");

            com.storeiq.entity.Store s = storeMapper.selectById(r.getStoreId());
            map.put("storeName", s != null ? s.getName() : "-");

            com.storeiq.entity.Supplier sup = supplierMapper.selectById(r.getSupplierId());
            map.put("supplierName", sup != null ? sup.getName() : "-");

            com.storeiq.entity.User operator = userMapper.selectById(r.getOperatorId());
            map.put("operatorName", operator != null ? operator.getNickname() : "-");
            return map;
        }).collect(Collectors.toList());

        return Result.success(enriched, result.getTotal());
    }

    /** 新增进货（更新库存） */
    @PostMapping
    @Transactional
    public Result<?> save(@Valid @RequestBody PurchaseRequest req, HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");

        // 生成批次号
        String batchNo = "P" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));

        // 创建进货记录
        PurchaseRecord record = new PurchaseRecord();
        record.setBatchNo(batchNo);
        record.setProductId(req.getProductId());
        record.setStoreId(req.getStoreId());
        record.setSupplierId(req.getSupplierId());
        record.setQuantity(req.getQuantity());
        record.setUnitCost(req.getUnitCost());
        record.setTotalCost(req.getUnitCost().multiply(new BigDecimal(req.getQuantity())));
        record.setOperatorId(userId);
        record.setRemark(req.getRemark());
        record.setCreateTime(LocalDateTime.now());
        purchaseMapper.insert(record);

        // 更新库存
        updateInventory(req.getStoreId(), req.getProductId(), req.getQuantity());

        return Result.success("入库成功");
    }

    /** 详情 */
    @GetMapping("/{id}")
    public Result<?> detail(@PathVariable Long id) {
        PurchaseRecord record = purchaseMapper.selectById(id);
        if (record == null) return Result.fail("记录不存在");

        Product p = productMapper.selectById(record.getProductId());
        com.storeiq.entity.Store s = storeMapper.selectById(record.getStoreId());

        Map<String, Object> map = new HashMap<>();
        map.put("record", record);
        map.put("productName", p != null ? p.getName() : "-");
        map.put("storeName", s != null ? s.getName() : "-");
        return Result.success(map);
    }

    /** 更新库存（增加） */
    private void updateInventory(Long storeId, Long productId, int quantity) {
        Inventory inv = inventoryMapper.selectOne(
                new LambdaQueryWrapper<Inventory>()
                        .eq(Inventory::getStoreId, storeId)
                        .eq(Inventory::getProductId, productId));

        if (inv != null) {
            inv.setQuantity(inv.getQuantity() + quantity);
            inv.setUpdateTime(LocalDateTime.now());
            inventoryMapper.updateById(inv);
        } else {
            Inventory newInv = new Inventory();
            newInv.setStoreId(storeId);
            newInv.setProductId(productId);
            newInv.setQuantity(quantity);
            newInv.setUpdateTime(LocalDateTime.now());
            inventoryMapper.insert(newInv);
        }
    }
}
