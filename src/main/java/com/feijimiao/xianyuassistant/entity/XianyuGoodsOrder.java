package com.feijimiao.xianyuassistant.entity;

import lombok.Data;

/**
 * 商品订单实体类
 */
@Data
public class XianyuGoodsOrder {
    
    private Long id;
    
    private Long xianyuAccountId;
    
    private Long xianyuGoodsId;
    
    private String xyGoodsId;
    
    private String pnmId;
    
    private String orderId;
    
    private String buyerUserId;
    
    private String buyerUserName;
    
    private String content;
    
    private Integer state;
    
    private Integer confirmState;
    
    private String createTime;
    
    private String goodsTitle;
}
