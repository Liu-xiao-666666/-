package com.sky.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.sky.dto.DishDTO;
import com.sky.entity.Dish;
import com.sky.mapper.DishMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * 菜品业务服务
 * <p>
 * 提供菜品的分页查询、新增、更新、删除和单条查询
 */
@Service
@RequiredArgsConstructor
public class DishService {

    private final DishMapper dishMapper;

    /**
     * 分页查询菜品列表
     *
     * @param page     当前页码
     * @param size     每页条数
     * @param name     菜品名称（可选，模糊匹配）
     * @param category 分类（可选，精确匹配）
     * @param status   状态（可选，1=在售 0=停售）
     * @return 分页菜品数据
     */
    public Page<Dish> page(int page, int size, String name, String category, Integer status) {
        LambdaQueryWrapper<Dish> wrapper = new LambdaQueryWrapper<Dish>()
                .like(StringUtils.hasText(name), Dish::getName, name)
                .eq(StringUtils.hasText(category), Dish::getCategory, category)
                .eq(status != null, Dish::getStatus, status)
                .orderByDesc(Dish::getCreateTime);
        return dishMapper.selectPage(new Page<>(page, size), wrapper);
    }

    /**
     * 新增菜品
     * <p>
     * 默认状态为在售、销量为 0
     *
     * @param dto 菜品信息
     * @return 新增的菜品
     */
    public Dish save(DishDTO dto) {
        Dish dish = new Dish();
        BeanUtils.copyProperties(dto, dish);
        if (dish.getStatus() == null) dish.setStatus(1);
        if (dish.getSales() == null) dish.setSales(0);
        dishMapper.insert(dish);
        return dish;
    }

    /**
     * 更新菜品信息
     *
     * @param dto 包含 id 和要修改字段的菜品信息
     * @return 更新后的菜品
     */
    public Dish update(DishDTO dto) {
        Dish dish = new Dish();
        BeanUtils.copyProperties(dto, dish);
        dishMapper.updateById(dish);
        return dishMapper.selectById(dto.getId());
    }

    /**
     * 根据 ID 删除菜品
     *
     * @param id 菜品 ID
     */
    public void delete(Long id) {
        dishMapper.deleteById(id);
    }

    /**
     * 根据 ID 查询单个菜品
     *
     * @param id 菜品 ID
     * @return 菜品信息
     */
    public Dish getById(Long id) {
        return dishMapper.selectById(id);
    }
}
