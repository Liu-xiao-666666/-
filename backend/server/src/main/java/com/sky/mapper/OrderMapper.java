package com.sky.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.sky.entity.Order;
import org.apache.ibatis.annotations.Mapper;

/**
 * 订单表 Mapper 接口
 * <p>
 * 继承 MyBatis-Plus BaseMapper，提供订单主表的数据库访问能力
 */
@Mapper
public interface OrderMapper extends BaseMapper<Order> {
}
