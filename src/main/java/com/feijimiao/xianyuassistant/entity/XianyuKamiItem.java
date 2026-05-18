package com.feijimiao.xianyuassistant.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("xianyu_kami_item")
public class XianyuKamiItem {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long kamiConfigId;

    private String kamiContent;

    private Integer status;

    private String orderId;

    private LocalDateTime usedTime;

    private Integer sortOrder;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime = LocalDateTime.now();
}
