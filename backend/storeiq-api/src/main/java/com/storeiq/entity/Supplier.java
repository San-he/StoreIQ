package com.storeiq.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 供应商
 */
@Data
@TableName("supplier")
public class Supplier implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    private String name;
    private String contactPerson;
    private String contactPhone;
    private String address;
    private String description;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
