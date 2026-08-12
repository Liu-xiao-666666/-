package com.sky.vo;

import com.sky.entity.Cart;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

@Data
@EqualsAndHashCode(callSuper = true)
public class CartVO extends Cart {
    private String dishName;
    private String dishImage;
    private BigDecimal dishPrice;
}
