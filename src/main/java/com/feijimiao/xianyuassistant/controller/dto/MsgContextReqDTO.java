package com.feijimiao.xianyuassistant.controller.dto;

import lombok.Data;

/**
 * 查询上下文消息请求DTO
 */
@Data
public class MsgContextReqDTO {
    
    /**
     * 会话ID
     */
    private String sid;
    
    /**
     * 限制条数（默认20）
     */
    private Integer limit;
    
    /**
     * 偏移量（默认0）
     */
    private Integer offset;
}
