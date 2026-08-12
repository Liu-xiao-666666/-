package com.sky.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.sky.dto.CartDTO;
import com.sky.entity.Dish;
import com.sky.entity.Order;
import com.sky.entity.OrderDetail;
import com.sky.entity.User;
import com.sky.mapper.DishMapper;
import com.sky.mapper.OrderDetailMapper;
import com.sky.mapper.OrderMapper;
import com.sky.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

/**
 * AI 智能聊天服务（美食问问）
 * <p>
 * 核心大模型集成服务 — 每次对话动态构建包含用户画像、
 * 历史订单和当前在售菜品的上下文，注入 System Prompt 实现
 * 角色定义、职能边界控制和内容安全护栏，
 * 通过 RestTemplate 调用 DeepSeek Chat Completion API 获取智能回复
 */
@Service
@RequiredArgsConstructor
public class ChatService {

    /** DeepSeek API 密钥 */
    @Value("${deepseek.api-key}")
    private String apiKey;

    /** DeepSeek API 基础地址 */
    @Value("${deepseek.base-url}")
    private String baseUrl;

    /** DeepSeek 模型名称 */
    @Value("${deepseek.model}")
    private String model;

    private final UserMapper userMapper;
    private final OrderMapper orderMapper;
    private final DishMapper dishMapper;
    private final OrderDetailMapper detailMapper;
    private final CartService cartService;
    private final RestTemplate restTemplate = new RestTemplate();

    /** 常见请求头，避免重复创建 */
    private HttpHeaders authHeaders() {
        HttpHeaders h = new HttpHeaders();
        h.setContentType(MediaType.APPLICATION_JSON);
        h.setBearerAuth(apiKey);
        return h;
    }

    /**
     * 与 AI 助手对话（含 Function Calling 点餐能力）
     *
     * @param userId  当前用户 ID
     * @param message 用户输入的消息
     * @return Map 包含 reply（回复文本）和 cartUpdated（是否修改了购物车）
     */
    public Map<String, Object> chat(Long userId, String message) {
        Map<String, Object> result = new HashMap<>();
        result.put("cartUpdated", false);
        List<Dish> dishes = dishMapper.selectList(
                new LambdaQueryWrapper<Dish>().eq(Dish::getStatus, 1));
        String ctx = buildContext(userId, dishes);
        String systemPrompt = buildSystemPrompt(ctx);
        List<Map<String, Object>> messages = new ArrayList<>();
        messages.add(Map.of("role", "system", "content", systemPrompt));
        messages.add(Map.of("role", "user", "content", message));

        try {
            // 第一次调用：带 tools 定义
            Map<String, Object> body = buildRequestBody(messages, true);
            Map<String, Object> resp = restTemplate.postForObject(
                    baseUrl + "/chat/completions",
                    new HttpEntity<>(body, authHeaders()), Map.class);
            if (resp == null) { result.put("reply", "抱歉，我暂时无法回答，请稍后再试。"); return result; }

            List<Map<String, Object>> choices = (List<Map<String, Object>>) resp.get("choices");
            if (choices == null || choices.isEmpty()) { result.put("reply", "抱歉，我暂时无法回答，请稍后再试。"); return result; }

            Map<String, Object> choice = choices.get(0);
            Map<String, Object> msg = (Map<String, Object>) choice.get("message");

            // 如果有 tool_calls，执行函数并回传结果
            List<Map<String, Object>> toolCalls = (List<Map<String, Object>>) msg.get("tool_calls");
            if (toolCalls != null && !toolCalls.isEmpty()) {
                messages.add(msg);
                for (Map<String, Object> tc : toolCalls) {
                    String fnName = (String) ((Map<String, Object>) tc.get("function")).get("name");
                    String fnArgs = (String) ((Map<String, Object>) tc.get("function")).get("arguments");
                    String toolResult = executeToolCall(fnName, fnArgs, userId, dishes);
                    messages.add(Map.of("role", "tool", "tool_call_id", tc.get("id"),
                            "content", toolResult));
                }
                result.put("cartUpdated", true);
                // 第二次调用：不带 tools，让 AI 根据结果生成回复
                Map<String, Object> body2 = buildRequestBody(messages, false);
                Map<String, Object> resp2 = restTemplate.postForObject(
                        baseUrl + "/chat/completions",
                        new HttpEntity<>(body2, authHeaders()), Map.class);
                if (resp2 != null) {
                    List<Map<String, Object>> choices2 = (List<Map<String, Object>>) resp2.get("choices");
                    if (choices2 != null && !choices2.isEmpty()) {
                        result.put("reply", ((Map<String, Object>) choices2.get(0).get("message")).get("content"));
                        return result;
                    }
                }
                result.put("reply", "抱歉，点餐操作已执行但生成回复失败，请查看购物车。");
                return result;
            }

            // 普通文本回复
            result.put("reply", msg.get("content") != null ? (String) msg.get("content") : "抱歉，我暂时无法回答。");
            return result;
        } catch (Exception e) {
            result.put("reply", "抱歉，服务繁忙，请稍后再试。");
            return result;
        }
    }

    /** 构建请求体，withTools=true 时附带 function 定义 */
    private Map<String, Object> buildRequestBody(List<Map<String, Object>> messages, boolean withTools) {
        Map<String, Object> body = new HashMap<>();
        body.put("model", model);
        body.put("messages", messages);
        body.put("max_tokens", 500);
        body.put("temperature", 0.7);
        if (withTools) {
            body.put("tools", List.of(Map.of("type", "function", "function", Map.of(
                    "name", "add_to_cart",
                    "description", "帮用户把一道菜加入购物车。call this when the user explicitly asks to order or add a dish.",
                    "parameters", Map.of(
                            "type", "object",
                            "properties", Map.of(
                                    "dishName", Map.of("type", "string", "description", "菜品名称，必须与菜单中的名称一致"),
                                    "size", Map.of("type", "string", "enum", List.of("large", "small"),
                                            "description", "分量，小份或大份，未指定时默认大份")
                            ),
                            "required", List.of("dishName")
                    )
            ))));
            body.put("tool_choice", "auto");
        }
        return body;
    }

    /** 执行 function call */
    private String executeToolCall(String fnName, String fnArgs, Long userId, List<Dish> dishes) {
        if ("add_to_cart".equals(fnName)) {
            return executeAddToCart(fnArgs, userId, dishes);
        }
        return "未知操作";
    }

    /** 模糊匹配菜品名称并加入购物车 */
    private String executeAddToCart(String fnArgs, Long userId, List<Dish> dishes) {
        try {
            // 解析 JSON 参数
            Map<String, Object> args = new com.fasterxml.jackson.databind.ObjectMapper().readValue(fnArgs, Map.class);
            String dishName = (String) args.get("dishName");
            String size = (String) args.get("size");
            if (size == null) size = "large";

            // 模糊匹配：先精确匹配，再 LIKE 模糊匹配
            Dish target = dishes.stream()
                    .filter(d -> d.getName().equals(dishName))
                    .findFirst()
                    .orElseGet(() -> dishes.stream()
                            .filter(d -> d.getName().contains(dishName) || dishName.contains(d.getName()))
                            .findFirst()
                            .orElse(null));
            if (target == null) {
                return "菜单中没有「" + dishName + "」，请告知用户暂时没有这道菜，并推荐类似菜品。";
            }

            // 执行加购
            CartDTO dto = new CartDTO();
            dto.setUserId(userId);
            dto.setDishId(target.getId());
            dto.setQuantity(1);
            dto.setSize(size);
            cartService.add(dto);

            String sizeLabel = "small".equals(size) ? "小份" : "大份";
            return "已成功将「" + target.getName() + "」（" + sizeLabel + " ¥" +
                    ("small".equals(size) && target.getPriceSmall() != null
                            ? target.getPriceSmall()
                            : target.getPriceLarge() != null ? target.getPriceLarge() : target.getPrice())
                    + "）加入购物车。请告知用户操作成功并可去购物车查看。";
        } catch (Exception e) {
            return "加购失败：" + e.getMessage();
        }
    }

    /** 构建上下文文本 */
    private String buildContext(Long userId, List<Dish> dishes) {
        User user = userMapper.selectById(userId);
        List<Order> orders = orderMapper.selectList(
                new LambdaQueryWrapper<Order>().eq(Order::getUserId, userId)
                        .orderByDesc(Order::getCreateTime).last("LIMIT 3"));
        for (Order o : orders) {
            o.setDetails(detailMapper.selectList(
                    new LambdaQueryWrapper<OrderDetail>().eq(OrderDetail::getOrderId, o.getId())));
        }
        StringBuilder ctx = new StringBuilder();
        ctx.append("用户名：").append(user != null ? user.getNickname() : "未知").append("\n");
        if (!orders.isEmpty()) {
            ctx.append("最近订单：\n");
            for (Order o : orders) {
                String names = o.getDetails() != null ? o.getDetails().stream()
                        .map(d -> d.getDishName() + "x" + d.getQuantity()).collect(Collectors.joining("、")) : "无";
                String status = switch (o.getStatus()) {
                    case 1 -> "待付款"; case 3 -> "配送中"; case 4 -> "待评价"; case 5 -> "已完成";
                    default -> "处理中";
                };
                ctx.append("- #").append(o.getId()).append(" ").append(names)
                        .append(" 金额¥").append(o.getTotal()).append(" 状态：").append(status).append("\n");
            }
        }
        if (!dishes.isEmpty()) {
            ctx.append("在售菜品：").append(dishes.stream()
                    .map(d -> {
                        StringBuilder sb = new StringBuilder(d.getName()).append("(id=").append(d.getId()).append(",");
                        if (d.getPriceSmall() != null && d.getPriceLarge() != null) {
                            sb.append("小份¥").append(d.getPriceSmall()).append("/大份¥").append(d.getPriceLarge());
                        } else {
                            sb.append("¥").append(d.getPrice());
                        }
                        sb.append("/").append(d.getCategory()).append(")");
                        return sb.toString();
                    })
                    .collect(Collectors.joining("、"))).append("\n");
        }
        return ctx.toString();
    }

    /** 构建 System Prompt */
    private String buildSystemPrompt(String ctx) {
        return """
            你是「天空外卖」的美食推荐助手「美食问问」。你的职责：
            1. 根据用户口味偏好，推荐菜品搭配（可超出菜单范围，但要合理）
            2. 结合当前在售菜品给出具体建议
            3. 回答关于点餐、配送、订单状态的问题
            4. 禁止回答或讨论政治性问题

            ★ 点餐能力：当用户明确说要下单/点餐/加入购物车某道菜时，你必须调用 add_to_cart 函数。
              菜名要匹配菜单中的准确名称，分量根据用户描述判断（小份/半份→small，大份/默认→large）。
              调用后根据返回结果告知用户是否成功。

            用户当前上下文：
            %s

            规则：
            - 涉及吃饭、点餐、菜品推荐、订单查询 → 发挥你的美食知识，给出搭配合理、生动的推荐，控制在200字以内
            - 不涉及上述范围（如闲聊、天气、科技、八卦等） → 统一只回复："超出了范围了，亲。我是美食助手，只能帮您解决吃相关的问题哦~"
            """.formatted(ctx);
    }
}
