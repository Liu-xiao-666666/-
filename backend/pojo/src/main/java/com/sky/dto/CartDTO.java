package com.sky.dto;

import lombok.Data;

@Data
public class CartDTO {
    private Long userId;
    private Long dishId;
    private Integer quantity;
    private String size;
}
