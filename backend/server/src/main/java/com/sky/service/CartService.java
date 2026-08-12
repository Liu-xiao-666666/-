package com.sky.service;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.sky.dto.CartDTO;
import com.sky.entity.Cart;
import com.sky.entity.Dish;
import com.sky.entity.Order;
import com.sky.entity.OrderDetail;
import com.sky.mapper.CartMapper;
import com.sky.mapper.DishMapper;
import com.sky.mapper.OrderDetailMapper;
import com.sky.mapper.OrderMapper;
import com.sky.vo.CartVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 购物车 & 订单流转业务服务
 * <p>
 * 管理移动端的购物车增删改查，以及订单的完整生命周期：
 * 支付模拟 → 配送 → 确认送达 → 评价，各操作直接更新订单状态
 */
@Service
@RequiredArgsConstructor
public class CartService {

    private final CartMapper cartMapper;
    private final DishMapper dishMapper;
    private final OrderMapper orderMapper;
    private final OrderDetailMapper detailMapper;

    /**
     * 添加菜品到购物车
     * <p>
     * 同一个用户对同一个菜品+同一分量重复添加时，累加数量而非新增记录；
     * 同一菜品不同分量视为两条独立的购物车记录
     *
     * @param dto 包含 userId、dishId、quantity、size
     */
    public void add(CartDTO dto) {
        String size = (dto.getSize() != null && !dto.getSize().isEmpty()) ? dto.getSize() : "large";
        System.err.println("[CartService.add] userId=" + dto.getUserId() + " dishId=" + dto.getDishId() + " size=" + size);
        Cart exist = cartMapper.selectOne(new LambdaQueryWrapper<Cart>()
                .eq(Cart::getUserId, dto.getUserId())
                .eq(Cart::getDishId, dto.getDishId())
                .eq(Cart::getSize, size));
        System.err.println("[CartService.add] exist=" + (exist != null ? "id=" + exist.getId() + " oldSize=" + exist.getSize() : "null → 新增"));
        if (exist != null) {
            exist.setQuantity(exist.getQuantity() + dto.getQuantity());
            cartMapper.updateById(exist);
        } else {
            Cart cart = new Cart();
            cart.setUserId(dto.getUserId());
            cart.setDishId(dto.getDishId());
            cart.setQuantity(dto.getQuantity());
            cart.setSize(size);
            try {
                int rows = cartMapper.insertCart(cart);
                System.err.println("[CartService.add] insertCart rows=" + rows + " id=" + cart.getId());
            } catch (Exception e) {
                System.err.println("[CartService.add] INSERT FAILED: " + e.getMessage());
                e.printStackTrace();
                throw new RuntimeException("添加购物车失败", e);
            }
        }
    }

    /**
     * 更新购物车某项的数量
     *
     * @param id       购物车项 ID
     * @param quantity 新数量
     */
    public void updateQuantity(Long id, Integer quantity) {
        Cart cart = cartMapper.selectById(id);
        if (cart != null) {
            cart.setQuantity(quantity);
            cartMapper.updateById(cart);
        }
    }

    /**
     * 删除购物车中的一项
     *
     * @param id 购物车项 ID
     */
    public void remove(Long id) {
        cartMapper.deleteById(id);
    }

    /**
     * 清空用户的全部购物车
     *
     * @param userId 用户 ID
     */
    public void clear(Long userId) {
        cartMapper.delete(new LambdaQueryWrapper<Cart>().eq(Cart::getUserId, userId));
    }

    /**
     * 获取用户购物车列表（含菜品详细信息）
     * <p>
     * 将 Cart 实体转为 CartVO，关联查询菜品名称、图片和价格，
     * 根据购物车选择的分量返回对应价格
     *
     * @param userId 用户 ID
     * @return 包含菜品信息的购物车视图对象列表
     */
    public List<CartVO> list(Long userId) {
        List<Cart> carts = cartMapper.selectList(
                new LambdaQueryWrapper<Cart>().eq(Cart::getUserId, userId));
        return carts.stream().map(cart -> {
            System.err.println("[CartService.list] cartId=" + cart.getId() + " dishId=" + cart.getDishId() + " size=" + cart.getSize());
            CartVO vo = BeanUtil.copyProperties(cart, CartVO.class);
            vo.setSize(cart.getSize());  // 确保分量字段被复制
            Dish dish = dishMapper.selectById(cart.getDishId());
            if (dish != null) {
                vo.setDishName(dish.getName());
                vo.setDishImage(dish.getImage());
                vo.setDishPrice(getSizePrice(dish, cart.getSize()));
            }
            System.err.println("[CartService.list] voSize=" + vo.getSize());
            return vo;
        }).collect(Collectors.toList());
    }

    /**
     * 根据分量获取菜品价格
     * <p>
     * 优先使用对应分量的定价，如果未设置则回退到默认 price 字段
     *
     * @param dish 菜品
     * @param size 分量（"small" 或 "large"）
     * @return 对应分量的价格
     */
    private java.math.BigDecimal getSizePrice(Dish dish, String size) {
        if ("small".equals(size) && dish.getPriceSmall() != null) {
            return dish.getPriceSmall();
        }
        if ("large".equals(size) && dish.getPriceLarge() != null) {
            return dish.getPriceLarge();
        }
        return dish.getPrice();
    }

    /**
     * 查询用户的所有订单（含订单详情）
     *
     * @param userId 用户 ID
     * @return 订单列表，每个订单包含详情子表数据
     */
    public List<Order> listOrders(Long userId) {
        List<Order> orders = orderMapper.selectList(
                new LambdaQueryWrapper<Order>().eq(Order::getUserId, userId)
                        .orderByDesc(Order::getCreateTime));
        orders.forEach(order -> {
            List<OrderDetail> details = detailMapper.selectList(
                    new LambdaQueryWrapper<OrderDetail>().eq(OrderDetail::getOrderId, order.getId()));
            order.setDetails(details);
        });
        return orders;
    }

    /**
     * 删除订单（级联删除订单详情）
     *
     * @param orderId 订单 ID
     */
    public void deleteOrder(Long orderId) {
        detailMapper.delete(new LambdaQueryWrapper<OrderDetail>().eq(OrderDetail::getOrderId, orderId));
        orderMapper.deleteById(orderId);
    }

    /**
     * 模拟支付订单
     * <p>
     * 状态 1 → 3（配送中），记录配送时间，
     * 随机生成 5~30 秒的预计送达倒计时
     *
     * @param orderId 订单 ID
     */
    public void payOrder(Long orderId) {
        int eta = 5 + (int)(Math.random() * 26);
        orderMapper.update(null, new LambdaUpdateWrapper<Order>()
                .eq(Order::getId, orderId)
                .set(Order::getStatus, 3)
                .set(Order::getDeliveryTime, LocalDateTime.now())
                .set(Order::getEta, eta));
    }

    /**
     * 确认送达
     * <p>
     * 状态 3 → 4（待评价）
     *
     * @param orderId 订单 ID
     */
    public void delivered(Long orderId) {
        orderMapper.update(null, new LambdaUpdateWrapper<Order>()
                .eq(Order::getId, orderId)
                .set(Order::getStatus, 4));
    }

    /**
     * 提交评价与评分
     * <p>
     * 状态 4 → 5（已完成），记录 1-5 星评分和可选文字评价
     *
     * @param orderId 订单 ID
     * @param rating  评分（1-5）
     * @param review  评价内容
     */
    public void review(Long orderId, Integer rating, String review) {
        orderMapper.update(null, new LambdaUpdateWrapper<Order>()
                .eq(Order::getId, orderId)
                .set(Order::getStatus, 5)
                .set(Order::getRating, rating)
                .set(Order::getReview, review));
    }

    /**
     * 提交售后申请
     * <p>
     * 对已完成的订单（status=5）提交售后，
     * 记录售后原因，状态置为处理中（afterSaleStatus=1）
     *
     * @param orderId 订单 ID
     * @param reason  售后原因描述
     */
    public void submitAfterSale(Long orderId, String reason) {
        orderMapper.update(null, new LambdaUpdateWrapper<Order>()
                .eq(Order::getId, orderId)
                .set(Order::getAfterSaleStatus, 1)
                .set(Order::getAfterSaleReason, reason));
    }
}
