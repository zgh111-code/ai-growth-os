package com.al.aichat.mapper;

import com.al.aichat.entity.User;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 用户 Mapper 接口
 *
 * 继承 BaseMapper<User> 后，MyBatis Plus 会自动帮我们实现：
 *   - insert()    插入用户
 *   - selectById() 根据ID查询
 *   - selectOne()  根据条件查询单个
 *   - updateById() 根据ID更新
 *   - deleteById() 根据ID删除
 *   - ... 等等
 *
 * 我们不需要写任何 SQL 语句，MyBatis Plus 会自动生成。
 *
 * @Mapper 注解告诉 Spring，这是一个 Mapper 接口，需要创建代理对象
 */
@Mapper
public interface UserMapper extends BaseMapper<User> {

}
