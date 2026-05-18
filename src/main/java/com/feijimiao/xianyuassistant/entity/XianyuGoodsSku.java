package com.feijimiao.xianyuassistant.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("xianyu_goods_sku")
public class XianyuGoodsSku {

    @TableId(type = IdType.ASSIGN_ID)
    private String id;

    private String xyGoodsId;

    private String skuId;

    private Integer price;

    private Integer quantity;

    private String propertyText;

    private Integer propertyId;

    private Integer valueId;

    private String valueText;

    private Integer propertySortOrder;

    private Integer valueSortOrder;

    private String features;

    private Long xianyuAccountId;

    private String createdTime;

    private String updatedTime;
}
