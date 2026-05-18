package com.feijimiao.xianyuassistant.entity.bo;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Data;

import java.util.List;

@Data
public class KeywordReplyRuleBO {
    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;
    @JsonSerialize(using = ToStringSerializer.class)
    private Long xianyuAccountId;
    private String xyGoodsId;
    private String keyword;
    private Integer matchMode;
    private Integer isFallback;
    private List<KeywordReplyContentBO> contents;

    @Data
    public static class KeywordReplyContentBO {
        @JsonSerialize(using = ToStringSerializer.class)
        private Long id;
        @JsonSerialize(using = ToStringSerializer.class)
        private Long ruleId;
        private String replyText;
        private String replyImageUrl;
    }
}
