package com.sky.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.sky.entity.Dish;
import com.sky.entity.Order;
import com.sky.entity.User;
import com.sky.mapper.DishMapper;
import com.sky.mapper.OrderMapper;
import com.sky.mapper.UserMapper;
import com.sky.vo.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 管理后台仪表盘控制器
 * <p>
 * 提供运营统计数据：用户数、菜品数、订单数、
 * 当日收入、订单状态分布、热销 Top5、评分分布等
 */
@RestController
@RequestMapping("/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final UserMapper userMapper;
    private final DishMapper dishMapper;
    private final OrderMapper orderMapper;

    /**
     * 获取仪表盘统计数据
     * <p>
     * 汇总用户总数、在售菜品数、订单总数、今日订单数和收入、
     * 各订单状态数量、总收入、销量 Top5 菜品、评分分布
     *
     * @return 包含各项统计数据的 Map
     */
    @GetMapping
    public Result<Map<String, Object>> stats() {
        Map<String, Object> data = new LinkedHashMap<>();

        // 用户总数
        long userCount = userMapper.selectCount(null);
        // 在售菜品数
        long dishCount = dishMapper.selectCount(new LambdaQueryWrapper<Dish>().eq(Dish::getStatus, 1));
        // 订单总数
        long orderCount = orderMapper.selectCount(null);

        // 今日数据
        LocalDateTime todayStart = LocalDate.now().atStartOfDay();
        List<Order> todayOrders = orderMapper.selectList(
                new LambdaQueryWrapper<Order>().ge(Order::getCreateTime, todayStart));
        long todayOrderCount = todayOrders.size();
        BigDecimal todayRevenue = todayOrders.stream()
                .map(Order::getTotal).reduce(BigDecimal.ZERO, BigDecimal::add);

        data.put("userCount", userCount);
        data.put("dishCount", dishCount);
        data.put("orderCount", orderCount);
        data.put("todayOrderCount", todayOrderCount);
        data.put("todayRevenue", todayRevenue);

        // 各状态订单数量（1~5）
        Map<Integer, Long> statusCounts = new LinkedHashMap<>();
        for (int i = 1; i <= 5; i++) {
            statusCounts.put(i, orderMapper.selectCount(
                    new LambdaQueryWrapper<Order>().eq(Order::getStatus, i)));
        }
        data.put("statusCounts", statusCounts);

        // 总收入
        List<Order> allOrders = orderMapper.selectList(null);
        BigDecimal totalRevenue = allOrders.stream()
                .map(Order::getTotal).reduce(BigDecimal.ZERO, BigDecimal::add);
        data.put("totalRevenue", totalRevenue);

        // 销量 Top5 菜品
        List<Dish> topDishes = dishMapper.selectList(
                new LambdaQueryWrapper<Dish>().orderByDesc(Dish::getSales).last("LIMIT 5"));
        data.put("topDishes", topDishes);

        // 评分分布（1~5 星各有多少单）
        Map<Integer, Long> ratingDist = new LinkedHashMap<>();
        for (int i = 1; i <= 5; i++) {
            ratingDist.put(i, orderMapper.selectCount(
                    new LambdaQueryWrapper<Order>().eq(Order::getRating, i)));
        }
        data.put("ratingDist", ratingDist);

        return Result.success(data);
    }
}
