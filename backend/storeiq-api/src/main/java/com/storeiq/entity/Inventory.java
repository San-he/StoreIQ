package com.storeiq.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 库存快照
 */
@Data
@TableName("inventory")
public class Inventory implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long storeId;
    private Long productId;
    private Integer quantity;

    /** 0=暂停售卖, 1=在售 */
    private Integer status;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
