package com.feijimiao.xianyuassistant.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("xianyu_kami_usage_record")
public class XianyuKamiUsageRecord {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long kamiConfigId;

    private Long kamiItemId;

    private Long xianyuAccountId;

    private String xyGoodsId;

    private String orderId;

    private String buyerUserId;

    private String buyerUserName;

    private String kamiContent;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime = LocalDateTime.now();
}
