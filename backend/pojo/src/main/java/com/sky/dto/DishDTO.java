package com.sky.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class DishDTO {
    private Long id;

    @NotBlank(message = "菜品名称不能为空")
    private String name;

    @NotBlank(message = "分类不能为空")
    private String category;

    @NotNull(message = "价格不能为空")
    private BigDecimal price;

    private BigDecimal priceSmall;
    private BigDecimal priceLarge;

    private String image;
    private String description;
    private Integer status;
}
