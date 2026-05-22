package com.al.aichat.mapper;

import com.al.aichat.entity.ChatMessage;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 聊天消息 Mapper 接口
 *
 * 继承 BaseMapper 后自动拥有增删改查方法。
 * 专门用来操作 chat_message 表。
 */
@Mapper
public interface ChatMessageMapper extends BaseMapper<ChatMessage> {

}
