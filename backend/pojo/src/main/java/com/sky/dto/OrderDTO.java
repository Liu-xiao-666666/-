package com.sky.dto;

import lombok.Data;

@Data
public class OrderDTO {
    private Long userId;
    private String address;
    private String phone;
    private String remark;
}
