package com.sky.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.sky.entity.Cart;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;

/**
 * 购物车表 Mapper 接口
 * <p>
 * 继承 MyBatis-Plus BaseMapper，提供购物车增删改查能力
 */
@Mapper
public interface CartMapper extends BaseMapper<Cart> {

    /** 原生 INSERT，强制写入全字段，不依赖 MyBatis-Plus 字段策略 */
    @Insert("INSERT INTO cart (user_id, dish_id, quantity, size) VALUES (#{userId}, #{dishId}, #{quantity}, #{size})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insertCart(Cart cart);
}
