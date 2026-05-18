package com.feijimiao.xianyuassistant.enums;

import lombok.Getter;

/**
 * 订单状态枚举
 */
@Getter
public enum OrderStatusEnum {
    
    /**
     * 等待买家付款
     */
    WAITING_PAYMENT(1, "等待买家付款"),
    
    /**
     * 等待卖家发货
     */
    WAITING_DELIVERY(2, "等待卖家发货"),
    
    /**
     * 已发货
     */
    DELIVERED(3, "已发货"),
    
    /**
     * 交易成功
     */
    COMPLETED(4, "交易成功"),
    
    /**
     * 交易关闭
     */
    CLOSED(5, "交易关闭");
    
    /**
     * 状态码
     */
    private final Integer code;
    
    /**
     * 状态描述
     */
    private final String description;
    
    OrderStatusEnum(Integer code, String description) {
        this.code = code;
        this.description = description;
    }
    
    /**
     * 根据状态码获取枚举
     *
     * @param code 状态码
     * @return 枚举对象，如果不存在则返回 null
     */
    public static OrderStatusEnum getByCode(Integer code) {
        if (code == null) {
            return null;
        }
        for (OrderStatusEnum status : values()) {
            if (status.getCode().equals(code)) {
                return status;
            }
        }
        return null;
    }
    
    /**
     * 根据状态码获取描述
     *
     * @param code 状态码
     * @return 状态描述，如果不存在则返回 "未知"
     */
    public static String getDescriptionByCode(Integer code) {
        OrderStatusEnum status = getByCode(code);
        return status != null ? status.getDescription() : "未知";
    }
    
    /**
     * 判断是否为有效的状态码
     *
     * @param code 状态码
     * @return true-有效，false-无效
     */
    public static boolean isValidCode(Integer code) {
        return getByCode(code) != null;
    }
}
