package com.feijimiao.xianyuassistant.controller.dto;

import lombok.Data;

/**
 * @author IAMLZY
 * @date 2026/4/12 00:31
 * @description
 */
@Data
public class ChatWithAIReqDTO {
    /**
     * 消息内容
     */
    private String msg;
    /**
     * 商品id
     */
    private String goodsId;
}
