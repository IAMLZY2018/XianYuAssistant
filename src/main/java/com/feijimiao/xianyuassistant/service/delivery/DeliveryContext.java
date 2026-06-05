package com.feijimiao.xianyuassistant.service.delivery;

import com.feijimiao.xianyuassistant.entity.XianyuGoodsAutoDeliveryConfig;
import lombok.Builder;
import lombok.Data;

/**
 * 发货上下文
 *
 * <p>封装发货过程中需要的所有信息，供 {@link DeliveryContentStrategy} 使用。</p>
 */
@Data
@Builder
public class DeliveryContext {
    private Long recordId;
    private Long accountId;
    private String xyGoodsId;
    private String sId;
    private String orderId;
    private String buyerUserName;
    private XianyuGoodsAutoDeliveryConfig deliveryConfig;
}
