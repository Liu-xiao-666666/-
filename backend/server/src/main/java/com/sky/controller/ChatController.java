package com.sky.controller;

import com.sky.service.ChatService;
import com.sky.vo.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * AI 聊天控制器（美食问问）
 * <p>
 * 接收用户消息，转发给 ChatService 调用 DeepSeek 大模型，
 * 返回智能美食推荐或订单咨询结果
 */
@RestController
@RequestMapping("/chat")
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;

    /**
     * 发送消息给 AI 助手
     * <p>
     * 将用户 ID 和消息内容传递给 ChatService，
     * ChatService 会构建包含用户历史的上下文并调用 DeepSeek API，
     * 支持 Function Calling 点餐
     *
     * @param body 包含 userId 和 message 的 JSON
     * @return 包含 reply 和 cartUpdated 的响应
     */
    @PostMapping
    public Result<Map<String, Object>> chat(@RequestBody Map<String, Object> body) {
        Long userId = Long.valueOf(body.get("userId").toString());
        String message = body.get("message").toString();
        Map<String, Object> data = chatService.chat(userId, message);
        return Result.success(data);
    }
}
