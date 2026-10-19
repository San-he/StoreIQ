package com.storeiq.common;

import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import org.apache.ibatis.reflection.MetaObject;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * MyBatis-Plus 字段自动填充处理器
 * 实体中标注 @TableField(fill = FieldFill.INSERT / INSERT_UPDATE) 的
 * createTime / updateTime 字段，在插入/更新时自动写入当前时间。
 * 此前项目缺少该处理器，自动填充从未生效（部分接口靠手动 set，部分靠数据库默认值，
 * sys_user 等表因此出现 create_time 为 null）。
 */
@Component
public class MyMetaObjectHandler implements MetaObjectHandler {

    @Override
    public void insertFill(MetaObject metaObject) {
        // 非严格填充：字段存在且值为 null 时才写入（实体已手动设置的值不会被覆盖）
        LocalDateTime now = LocalDateTime.now();
        fillStrategy(metaObject, "createTime", now);
        fillStrategy(metaObject, "updateTime", now);
    }

    @Override
    public void updateFill(MetaObject metaObject) {
        fillStrategy(metaObject, "updateTime", LocalDateTime.now());
    }
}
