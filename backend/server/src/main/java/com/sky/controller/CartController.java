package com.sky.controller;

import com.sky.dto.CartDTO;
import com.sky.entity.Order;
import com.sky.dto.OrderDTO;
import com.sky.service.CartService;
import com.sky.service.OrderService;
import com.sky.vo.CartVO;
import com.sky.dto.ReviewDTO;
import com.sky.vo.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 购物车 & 订单操作控制器（移动端）
 * <p>
 * 统一处理移动端的购物车管理、下单、支付模拟、配送确认和评价操作，
 * 是用户端最核心的业务入口
 */
@RestController
@RequestMapping("/cart")
@RequiredArgsConstructor
public class CartController {

    private final CartService cartService;
    private final OrderService orderService;

    /**
     * 获取用户购物车列表（含菜品名称、图片、价格）
     *
     * @param userId 用户 ID
     * @return 购物车项列表
     */
    @GetMapping("/list")
    public Result<List<CartVO>> list(@RequestParam Long userId) {
        return Result.success(cartService.list(userId));
    }

    /**
     * 添加菜品到购物车（已存在则增加数量）
     *
     * @param dto 包含 userId、dishId、quantity
     */
    @PostMapping("/add")
    public Result<Void> add(@RequestBody CartDTO dto) {
        cartService.add(dto);
        return Result.success();
    }

    /**
     * 更新购物车项的数量
     *
     * @param id       购物车项 ID
     * @param quantity 新数量
     */
    @PutMapping("/{id}")
    public Result<Void> updateQuantity(@PathVariable Long id, @RequestParam Integer quantity) {
        cartService.updateQuantity(id, quantity);
        return Result.success();
    }

    /**
     * 删除购物车中的某一项
     *
     * @param id 购物车项 ID
     */
    @DeleteMapping("/{id}")
    public Result<Void> remove(@PathVariable Long id) {
        cartService.remove(id);
        return Result.success();
    }

    /**
     * 清空用户购物车
     *
     * @param userId 用户 ID
     */
    @DeleteMapping("/clear")
    public Result<Void> clear(@RequestParam Long userId) {
        cartService.clear(userId);
        return Result.success();
    }

    /**
     * 提交订单
     * <p>
     * 将购物车中的菜品转为订单，清空购物车，更新菜品销量
     *
     * @param dto 包含 userId、address、phone、remark
     * @return 生成的订单
     */
    @PostMapping("/submit")
    public Result<Order> submit(@RequestBody OrderDTO dto) {
        return Result.success(orderService.submit(dto));
    }

    /**
     * 查询用户的所有订单（含订单详情）
     *
     * @param userId 用户 ID
     * @return 订单列表
     */
    @GetMapping("/orders")
    public Result<List<Order>> orders(@RequestParam Long userId) {
        return Result.success(cartService.listOrders(userId));
    }

    /**
     * 删除订单（级联删除订单详情）
     *
     * @param id 订单 ID
     */
    @DeleteMapping("/orders/{id}")
    public Result<Void> deleteOrder(@PathVariable Long id) {
        cartService.deleteOrder(id);
        return Result.success();
    }

    /**
     * 模拟支付订单
     * <p>
     * 将订单状态设为"配送中"，设置配送时间和随机预计送达秒数
     *
     * @param id 订单 ID
     * @return 更新后的订单
     */
    @PutMapping("/orders/{id}/pay")
    public Result<Order> payOrder(@PathVariable Long id) {
        cartService.payOrder(id);
        Order order = orderService.getById(id);
        return Result.success(order);
    }

    /**
     * 确认送达
     * <p>
     * 将订单状态设为"待评价"
     *
     * @param id 订单 ID
     */
    @PutMapping("/orders/{id}/delivered")
    public Result<Void> delivered(@PathVariable Long id) {
        cartService.delivered(id);
        return Result.success();
    }

    /**
     * 提交评价
     * <p>
     * 将订单状态设为"已完成"，记录评分和评价内容
     *
     * @param id  订单 ID
     * @param dto 包含 rating（1-5）和 review（可选）
     */
    @PutMapping("/orders/{id}/review")
    public Result<Void> review(@PathVariable Long id, @RequestBody ReviewDTO dto) {
        cartService.review(id, dto.getRating(), dto.getReview());
        return Result.success();
    }

    /**
     * 提交售后申请
     * <p>
     * 对已完成的订单提交问题反馈，记录售后原因
     *
     * @param id   订单 ID
     * @param body 包含 reason 的 JSON
     */
    @PutMapping("/orders/{id}/aftersale")
    public Result<Void> afterSale(@PathVariable Long id, @RequestBody Map<String, String> body) {
        cartService.submitAfterSale(id, body.get("reason"));
        return Result.success();
    }
}
