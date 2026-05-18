package com.feijimiao.xianyuassistant.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("xianyu_kami_config")
public class XianyuKamiConfig {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long xianyuAccountId;

    private String aliasName;

    private Integer alertEnabled;

    private Integer alertThresholdType;

    private Integer alertThresholdValue;

    private String alertEmail;

    private Integer totalCount;

    private Integer usedCount;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime = LocalDateTime.now();

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime = LocalDateTime.now();
}
