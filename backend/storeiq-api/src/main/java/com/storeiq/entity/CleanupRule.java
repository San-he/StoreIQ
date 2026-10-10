package com.storeiq.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 数据清理保留规则
 */
@Data
@TableName("cleanup_rule")
public class CleanupRule implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 表类型: operation_log / purchase_record / sale_record */
    private String tableType;

    /** 保留天数 */
    private Integer retentionDays;

    /** 保留的店铺ID，逗号分隔，NULL=全部 */
    private String storeIds;

    /** 保留的分类ID，逗号分隔，NULL=全部 */
    private String categoryIds;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
