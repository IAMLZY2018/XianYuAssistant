package com.feijimiao.xianyuassistant.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 闲鱼商品信息实体类
 */
@Data
@TableName("xianyu_goods")
public class XianyuGoodsInfo {
    
    /**
     * 主键ID（雪花ID）
     */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    
    /**
     * 闲鱼商品ID
     */
    private String xyGoodId;
    
    /**
     * 商品标题
     */
    private String title;
    
    /**
     * 封面图片URL
     */
    private String coverPic;
    
    /**
     * 商品详情图片（JSON数组）
     */
    private String infoPic;
    
    /**
     * 商品详情信息（预留字段）
     */
    private String detailInfo;
    
    /**
     * 商品详情页URL
     */
    private String detailUrl;
    
    /**
     * 关联的闲鱼账号ID
     */
    private Long xianyuAccountId;
    
    /**
     * 商品价格
     */
    private String soldPrice;
    
    private Integer skuCount;
    
    private Integer status;
    
    /**
     * 创建时间（SQLite存储为TEXT）
     */
    private String createdTime;
    
    /**
     * 更新时间（SQLite存储为TEXT）
     */
    private String updatedTime;
}
