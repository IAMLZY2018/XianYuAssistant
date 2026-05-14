package com.feijimiao.xianyuassistant.controller.dto;

import lombok.Data;

import java.util.List;

@Data
public class SalesRevenueRespDTO {
    private List<String> labels;
    private List<Double> values;
}
