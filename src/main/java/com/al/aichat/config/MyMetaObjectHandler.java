package com.al.aichat.config;

import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import org.apache.ibatis.reflection.MetaObject;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * MyBatis Plus 自动填充处理器
 *
 * 当实体类中使用 @TableField(fill = FieldFill.INSERT) 时，
 * 会自动调用这里的 insertFill 方法给字段赋值。
 *
 * 这样我们就不用每次手动设置 createdAt 和 updatedAt 了。
 */
@Component
public class MyMetaObjectHandler implements MetaObjectHandler {

    /**
     * 插入数据时自动填充
     */
    @Override
    public void insertFill(MetaObject metaObject) {
        // 参数1：字段名（实体类中的字段名）
        // 参数2：要填充的值
        this.strictInsertFill(metaObject, "createdAt", LocalDateTime.class, LocalDateTime.now());
        this.strictInsertFill(metaObject, "updatedAt", LocalDateTime.class, LocalDateTime.now());
    }

    /**
     * 更新数据时自动填充
     */
    @Override
    public void updateFill(MetaObject metaObject) {
        this.strictUpdateFill(metaObject, "updatedAt", LocalDateTime.class, LocalDateTime.now());
    }

}
