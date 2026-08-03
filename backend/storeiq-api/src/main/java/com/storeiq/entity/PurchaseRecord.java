package com.storeiq.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 进货记录
 */
@Data
@TableName("purchase_record")
public class PurchaseRecord implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    private String batchNo;
    private Long productId;
    private Long storeId;
    private Long supplierId;
    private Integer quantity;
    private BigDecimal unitCost;
    private BigDecimal totalCost;
    private Long operatorId;
    private String remark;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
