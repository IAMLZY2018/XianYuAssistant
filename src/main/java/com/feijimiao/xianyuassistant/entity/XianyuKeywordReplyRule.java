package com.feijimiao.xianyuassistant.entity;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Data;

@Data
public class XianyuKeywordReplyRule {
    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;
    @JsonSerialize(using = ToStringSerializer.class)
    private Long xianyuAccountId;
    private String xyGoodsId;
    private String keyword;
    private Integer matchMode;
    private Integer isFallback;
    private String createTime;
}
