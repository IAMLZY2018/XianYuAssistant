package com.feijimiao.xianyuassistant.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("xianyu_goods_card_secret")
public class XianyuGoodsCardSecret {
    @TableId(type = IdType.AUTO)
    private Integer id;
    private Long xianyuAccountId;
    private String xyGoodsId;
    private String cardContent;
    private Integer isUsed;
    private String orderId;
    private String createTime;
    private String updateTime;
}
