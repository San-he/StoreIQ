package com.storeiq.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 店铺
 */
@Data
@TableName("store")
public class Store implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    private String name;
    private String code;
    private String address;
    private BigDecimal longitude;
    private BigDecimal latitude;
    private String region;
    private Integer capacity;
    private String propertyType;
    private BigDecimal monthlyRent;
    private BigDecimal purchaseCost;
    private String contactPerson;
    private String contactPhone;
    private Integer status;
    private String description;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
