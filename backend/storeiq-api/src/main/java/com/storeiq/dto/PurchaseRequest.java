package com.storeiq.dto;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import java.math.BigDecimal;

/**
 * 进货/入库请求
 */
public class PurchaseRequest {

    @NotNull(message = "商品ID不能为空")
    private Long productId;

    @NotNull(message = "店铺ID不能为空")
    private Long storeId;

    private Long supplierId;

    @NotNull(message = "数量不能为空")
    @Min(value = 1, message = "数量至少为1")
    private Integer quantity;

    @NotNull(message = "进货单价不能为空")
    private BigDecimal unitCost;

    private String remark;

    public Long getProductId() { return productId; }
    public void setProductId(Long productId) { this.productId = productId; }

    public Long getStoreId() { return storeId; }
    public void setStoreId(Long storeId) { this.storeId = storeId; }

    public Long getSupplierId() { return supplierId; }
    public void setSupplierId(Long supplierId) { this.supplierId = supplierId; }

    public Integer getQuantity() { return quantity; }
    public void setQuantity(Integer quantity) { this.quantity = quantity; }

    public BigDecimal getUnitCost() { return unitCost; }
    public void setUnitCost(BigDecimal unitCost) { this.unitCost = unitCost; }

    public String getRemark() { return remark; }
    public void setRemark(String remark) { this.remark = remark; }
}
