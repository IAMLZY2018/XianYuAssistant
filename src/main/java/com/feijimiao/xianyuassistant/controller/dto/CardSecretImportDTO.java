package com.feijimiao.xianyuassistant.controller.dto;

import lombok.Data;
import java.util.List;

@Data
public class CardSecretImportDTO {
    private Long xianyuAccountId;
    private String xyGoodsId;
    private List<String> secrets;
}
