package com.sky.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.sky.entity.Order;
import com.sky.entity.OrderDetail;
import com.sky.mapper.OrderDetailMapper;
import com.sky.mapper.OrderMapper;
import com.sky.vo.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 订单控制器（管理后台）
 * <p>
 * 提供订单查询功能，支持按手机号、订单号、状态筛选，
 * 查询结果自动关联订单详情（菜品快照）
 */
@RestController
@RequestMapping("/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderMapper orderMapper;
    private final OrderDetailMapper detailMapper;

    /**
     * 根据 ID 查询单个订单（含订单详情）
     *
     * @param id 订单 ID
     * @return 订单信息，包含详情列表
     */
    @GetMapping("/{id}")
    public Result<Order> getById(@PathVariable Long id) {
        Order order = orderMapper.selectById(id);
        if (order != null) {
            List<OrderDetail> details = detailMapper.selectList(
                    new LambdaQueryWrapper<OrderDetail>().eq(OrderDetail::getOrderId, id));
            order.setDetails(details);
        }
        return Result.success(order);
    }

    /**
     * 分页查询订单列表（管理后台）
     * <p>
     * 支持按手机号、订单号、状态筛选，
     * 结果中嵌入订单详情列表
     *
     * @param page    当前页码，默认 1
     * @param size    每页条数，默认 10
     * @param phone   手机号筛选（可选，模糊匹配）
     * @param orderNo 订单号筛选（可选，模糊匹配）
     * @param status  状态筛选（可选，精确匹配）
     * @return 分页订单数据
     */
    @GetMapping
    public Result<Page<Order>> list(
            @RequestParam(name = "page", defaultValue = "1") int page,
            @RequestParam(name = "size", defaultValue = "10") int size,
            @RequestParam(name = "phone", required = false) String phone,
            @RequestParam(name = "orderNo", required = false) String orderNo,
            @RequestParam(name = "status", required = false) Integer status) {
        LambdaQueryWrapper<Order> wrapper = new LambdaQueryWrapper<Order>()
                .eq(status != null, Order::getStatus, status)
                .like(StringUtils.hasText(orderNo), Order::getOrderNo, orderNo)
                .like(StringUtils.hasText(phone), Order::getPhone, phone)
                .orderByDesc(Order::getCreateTime);
        Page<Order> result = orderMapper.selectPage(new Page<>(page, size), wrapper);
        // 批量加载订单详情，避免 N+1 查询
        if (!result.getRecords().isEmpty()) {
            List<Long> orderIds = result.getRecords().stream().map(Order::getId).collect(Collectors.toList());
            List<OrderDetail> details = detailMapper.selectList(
                    new LambdaQueryWrapper<OrderDetail>().in(OrderDetail::getOrderId, orderIds));
            Map<Long, List<OrderDetail>> detailMap = details.stream()
                    .collect(Collectors.groupingBy(OrderDetail::getOrderId));
            result.getRecords().forEach(o -> o.setDetails(detailMap.get(o.getId())));
        }
        return Result.success(result);
    }

    /**
     * 处理售后 — 将售后状态标记为已处理
     *
     * @param body 包含 orderId 的 JSON
     */
    @PutMapping("/aftersale/resolve")
    public Result<Void> resolveAfterSale(@RequestBody Map<String, Long> body) {
        Long id = body.get("orderId");
        Order order = orderMapper.selectById(id);
        if (order != null && order.getAfterSaleStatus() != null && order.getAfterSaleStatus() == 1) {
            order.setAfterSaleStatus(2);
            orderMapper.updateById(order);
        }
        return Result.success();
    }
}
