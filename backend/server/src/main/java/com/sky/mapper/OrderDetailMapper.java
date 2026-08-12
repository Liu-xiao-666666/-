package com.sky.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.sky.entity.OrderDetail;
import org.apache.ibatis.annotations.Mapper;

/**
 * 订单详情表 Mapper 接口
 * <p>
 * 继承 MyBatis-Plus BaseMapper，提供订单详情（菜品快照）的数据库访问能力
 */
@Mapper
public interface OrderDetailMapper extends BaseMapper<OrderDetail> {
}
