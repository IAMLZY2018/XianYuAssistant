package com.feijimiao.xianyuassistant.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("xianyu_goods_sku_property")
public class XianyuGoodsSkuProperty {

    @TableId(type = IdType.ASSIGN_ID)
    private String id;

    private String xyGoodsId;

    private Integer propertyId;

    private String propertyText;

    private Integer propertySortOrder;

    private Integer valueId;

    private String valueText;

    private Integer valueSortOrder;

    private Long xianyuAccountId;

    private String createdTime;

    private String updatedTime;
}
