package com.sky.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.sky.entity.User;
import org.apache.ibatis.annotations.Mapper;

/**
 * 用户表 Mapper 接口
 * <p>
 * 继承 MyBatis-Plus BaseMapper，自动获得 CRUD 方法，
 * 也可在 XML 或注解中添加自定义 SQL
 */
@Mapper
public interface UserMapper extends BaseMapper<User> {
}
