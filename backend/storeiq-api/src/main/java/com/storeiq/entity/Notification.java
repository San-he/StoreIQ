package com.storeiq.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 通知消息
 */
@Data
@TableName("notification")
public class Notification implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    private String title;
    private String content;
    private String type;
    private String level;
    private Integer isRead;
    private Long recipientId;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
