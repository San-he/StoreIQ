package com.storeiq.dto;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.math.BigDecimal;

/**
 * 批量导入销售条目 — 使用 SKU / 名称来定位关联数据
 */
public class SaleBatchItem {

    @NotBlank(message = "商品SKU不能为空")
    private String productCode;

    @NotBlank(message = "店铺名称不能为空")
    private String storeName;

    @NotNull(message = "数量不能为空")
    @Min(value = 1, message = "数量至少为1")
    private Integer quantity;

    @NotNull(message = "售价不能为空")
    private BigDecimal unitPrice;

    private String customerName;
    private String remark;

    public String getProductCode() { return productCode; }
    public void setProductCode(String productCode) { this.productCode = productCode; }

    public String getStoreName() { return storeName; }
    public void setStoreName(String storeName) { this.storeName = storeName; }

    public Integer getQuantity() { return quantity; }
    public void setQuantity(Integer quantity) { this.quantity = quantity; }

    public BigDecimal getUnitPrice() { return unitPrice; }
    public void setUnitPrice(BigDecimal unitPrice) { this.unitPrice = unitPrice; }

    public String getCustomerName() { return customerName; }
    public void setCustomerName(String customerName) { this.customerName = customerName; }

    public String getRemark() { return remark; }
    public void setRemark(String remark) { this.remark = remark; }
}
