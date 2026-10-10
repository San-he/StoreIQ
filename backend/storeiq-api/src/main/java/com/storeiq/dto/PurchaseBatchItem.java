package com.storeiq.dto;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.math.BigDecimal;

/**
 * 批量导入进货条目 — 使用 SKU / 名称来定位关联数据
 */
public class PurchaseBatchItem {

    @NotBlank(message = "商品SKU不能为空")
    private String productCode;

    @NotBlank(message = "店铺名称不能为空")
    private String storeName;

    private String supplierName;

    @NotNull(message = "数量不能为空")
    @Min(value = 1, message = "数量至少为1")
    private Integer quantity;

    @NotNull(message = "进货单价不能为空")
    private BigDecimal unitCost;

    private String remark;

    public String getProductCode() { return productCode; }
    public void setProductCode(String productCode) { this.productCode = productCode; }

    public String getStoreName() { return storeName; }
    public void setStoreName(String storeName) { this.storeName = storeName; }

    public String getSupplierName() { return supplierName; }
    public void setSupplierName(String supplierName) { this.supplierName = supplierName; }

    public Integer getQuantity() { return quantity; }
    public void setQuantity(Integer quantity) { this.quantity = quantity; }

    public BigDecimal getUnitCost() { return unitCost; }
    public void setUnitCost(BigDecimal unitCost) { this.unitCost = unitCost; }

    public String getRemark() { return remark; }
    public void setRemark(String remark) { this.remark = remark; }
}
