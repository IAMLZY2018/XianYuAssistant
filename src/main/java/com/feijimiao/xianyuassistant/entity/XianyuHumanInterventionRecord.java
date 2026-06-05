package com.feijimiao.xianyuassistant.entity;

import lombok.Data;

@Data
public class XianyuHumanInterventionRecord {

    private Long id;

    private Long xianyuAccountId;

    private String xyGoodsId;

    private String sId;

    private String endTime;

    private String createdTime;
}
