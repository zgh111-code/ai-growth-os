package com.al.aichat.mapper;

import com.al.aichat.entity.ChatSession;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 会话 Mapper 接口
 *
 * 继承 MyBatis Plus 的 BaseMapper，自动获得 CRUD 方法：
 *   - insert       → 插入会话
 *   - selectById   → 根据 ID 查询
 *   - selectList   → 查询列表（配合 Wrapper 条件构造器）
 *   - updateById   → 根据 ID 更新
 *   - deleteById   → 根据 ID 删除
 *
 * 不需要写 SQL 语句，MyBatis Plus 会自动生成。
 */
@Mapper
public interface SessionMapper extends BaseMapper<ChatSession> {

}
