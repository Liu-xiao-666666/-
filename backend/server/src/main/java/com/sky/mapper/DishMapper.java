package com.sky.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.sky.entity.Dish;
import org.apache.ibatis.annotations.Mapper;

/**
 * 菜品表 Mapper 接口
 * <p>
 * 继承 MyBatis-Plus BaseMapper，提供菜品信息的数据库访问能力
 */
@Mapper
public interface DishMapper extends BaseMapper<Dish> {
}
