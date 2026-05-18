package com.feijimiao.xianyuassistant.entity;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Data;

@Data
public class XianyuKeywordReplyContent {
    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;
    @JsonSerialize(using = ToStringSerializer.class)
    private Long ruleId;
    private String replyText;
    private String replyImageUrl;
    private String createTime;
}
