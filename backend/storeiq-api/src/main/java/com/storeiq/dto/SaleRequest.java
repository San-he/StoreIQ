package com.storeiq.dto;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import java.math.BigDecimal;

/**
 * 销售/出库请求
 */
public class SaleRequest {

    @NotNull(message = "商品ID不能为空")
    private Long productId;

    @NotNull(message = "店铺ID不能为空")
    private Long storeId;

    @NotNull(message = "数量不能为空")
    @Min(value = 1, message = "数量至少为1")
    private Integer quantity;

    @NotNull(message = "售价不能为空")
    private BigDecimal unitPrice;

    private String customerName;
    private String remark;

    public Long getProductId() { return productId; }
    public void setProductId(Long productId) { this.productId = productId; }

    public Long getStoreId() { return storeId; }
    public void setStoreId(Long storeId) { this.storeId = storeId; }

    public Integer getQuantity() { return quantity; }
    public void setQuantity(Integer quantity) { this.quantity = quantity; }

    public BigDecimal getUnitPrice() { return unitPrice; }
    public void setUnitPrice(BigDecimal unitPrice) { this.unitPrice = unitPrice; }

    public String getCustomerName() { return customerName; }
    public void setCustomerName(String customerName) { this.customerName = customerName; }

    public String getRemark() { return remark; }
    public void setRemark(String remark) { this.remark = remark; }
}
