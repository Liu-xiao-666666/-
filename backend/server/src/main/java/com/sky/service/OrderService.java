package com.sky.service;

import cn.hutool.core.util.IdUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.sky.dto.OrderDTO;
import com.sky.entity.*;
import com.sky.mapper.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 订单业务服务
 * <p>
 * 处理订单提交和查询，订单提交包含购物车校验、金额计算、
 * 订单创建、详情快照生成、销量更新和购物车清空，整个过程使用事务保证一致性
 */
@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderMapper orderMapper;
    private final OrderDetailMapper detailMapper;
    private final CartMapper cartMapper;
    private final DishMapper dishMapper;

    /**
     * 提交订单（事务）
     * <p>
     * 执行流程：
     * 1. 查询用户购物车，空则抛异常
     * 2. 逐个校验购物车中的菜品是否在售，有下架则提示并阻止下单
     * 3. 计算订单总金额（单价 × 数量累加）
     * 4. 生成订单编号（UUID 前 16 位大写）
     * 5. 插入订单主表（状态=1 待付款）
     * 6. 为每个菜品生成订单详情快照（记录下单时菜品名和价格）
     * 7. 更新菜品销量
     * 8. 清空用户购物车
     *
     * @param dto 包含 userId、address、phone、remark 的订单信息
     * @return 生成的订单
     * @throws RuntimeException 购物车为空或菜品已下架时抛出
     */
    @Transactional
    public Order submit(OrderDTO dto) {
        // 查询购物车
        List<Cart> carts = cartMapper.selectList(
                new LambdaQueryWrapper<Cart>().eq(Cart::getUserId, dto.getUserId()));
        if (carts.isEmpty()) throw new RuntimeException("购物车为空");

        // 校验菜品状态并计算总金额（按分量定价）
        BigDecimal total = BigDecimal.ZERO;
        for (Cart cart : carts) {
            Dish dish = dishMapper.selectById(cart.getDishId());
            if (dish == null || dish.getStatus() == 0) {
                throw new RuntimeException("菜品「" + (dish != null ? dish.getName() : "未知") + "」已下架");
            }
            BigDecimal itemPrice = getSizePrice(dish, cart.getSize());
            total = total.add(itemPrice.multiply(BigDecimal.valueOf(cart.getQuantity())));
        }

        // 创建订单
        Order order = new Order();
        order.setUserId(dto.getUserId());
        order.setOrderNo(IdUtil.fastSimpleUUID().substring(0, 16).toUpperCase());
        order.setTotal(total);
        order.setStatus(1);
        order.setAddress(dto.getAddress());
        order.setPhone(dto.getPhone());
        order.setRemark(dto.getRemark());
        order.setCreateTime(LocalDateTime.now());
        order.setUpdateTime(LocalDateTime.now());
        orderMapper.insert(order);

        // 生成订单详情快照（含分量和对应价格） + 更新销量
        for (Cart cart : carts) {
            Dish dish = dishMapper.selectById(cart.getDishId());
            OrderDetail detail = new OrderDetail();
            detail.setOrderId(order.getId());
            detail.setDishId(cart.getDishId());
            detail.setDishName(dish.getName());
            detail.setPrice(getSizePrice(dish, cart.getSize()));
            detail.setQuantity(cart.getQuantity());
            detail.setSize(cart.getSize());
            detailMapper.insert(detail);

            dish.setSales(dish.getSales() + cart.getQuantity());
            dishMapper.updateById(dish);
        }

        // 清空购物车
        cartMapper.delete(new LambdaQueryWrapper<Cart>().eq(Cart::getUserId, dto.getUserId()));
        return order;
    }

    /**
     * 根据 ID 查询订单（含订单详情）
     *
     * @param id 订单 ID
     * @return 订单信息，包含详情列表
     */
    /**
     * 根据分量获取菜品价格
     *
     * @param dish 菜品
     * @param size 分量（"small" 或 "large"），为 null 时默认取 price
     * @return 对应分量的价格
     */
    private BigDecimal getSizePrice(Dish dish, String size) {
        if ("small".equals(size) && dish.getPriceSmall() != null) {
            return dish.getPriceSmall();
        }
        if ("large".equals(size) && dish.getPriceLarge() != null) {
            return dish.getPriceLarge();
        }
        return dish.getPrice();
    }

    public Order getById(Long id) {
        Order order = orderMapper.selectById(id);
        if (order != null) {
            List<OrderDetail> details = detailMapper.selectList(
                    new LambdaQueryWrapper<OrderDetail>().eq(OrderDetail::getOrderId, id));
            order.setDetails(details);
        }
        return order;
    }
}
