package com.sky.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.sky.dto.DishDTO;
import com.sky.entity.Dish;
import com.sky.service.DishService;
import com.sky.vo.Result;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 菜品控制器
 * <p>
 * 提供菜品的 CRUD 接口，支持分页查询和多条件筛选
 */
@RestController
@RequestMapping("/dish")
@RequiredArgsConstructor
public class DishController {

    private final DishService dishService;

    /**
     * 分页查询菜品列表
     *
     * @param page     当前页码，默认 1
     * @param size     每页条数，默认 10
     * @param name     菜品名称模糊搜索（可选）
     * @param category 分类筛选（可选）
     * @param status   状态筛选（可选，1=在售 0=停售）
     * @return 分页菜品数据
     */
    @GetMapping("/page")
    public Result<Page<Dish>> page(
            @RequestParam(name = "page", defaultValue = "1") int page,
            @RequestParam(name = "size", defaultValue = "10") int size,
            @RequestParam(name = "name", required = false) String name,
            @RequestParam(name = "category", required = false) String category,
            @RequestParam(name = "status", required = false) Integer status) {
        return Result.success(dishService.page(page, size, name, category, status));
    }

    /**
     * 根据 ID 查询单个菜品
     *
     * @param id 菜品 ID
     * @return 菜品信息
     */
    @GetMapping("/{id}")
    public Result<Dish> getById(@PathVariable Long id) {
        return Result.success(dishService.getById(id));
    }

    /**
     * 新增菜品
     *
     * @param dto 菜品信息（名称、分类、价格、图片、描述等）
     * @return 新增的菜品
     */
    @PostMapping
    public Result<Dish> save(@Valid @RequestBody DishDTO dto) {
        return Result.success(dishService.save(dto));
    }

    /**
     * 更新菜品信息
     *
     * @param dto 包含 id 和需要修改的字段
     * @return 更新后的菜品
     */
    @PutMapping
    public Result<Dish> update(@Valid @RequestBody DishDTO dto) {
        return Result.success(dishService.update(dto));
    }

    /**
     * 根据 ID 删除菜品
     *
     * @param id 菜品 ID
     */
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        dishService.delete(id);
        return Result.success();
    }
}
