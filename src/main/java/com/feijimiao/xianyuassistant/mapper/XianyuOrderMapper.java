package com.feijimiao.xianyuassistant.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.feijimiao.xianyuassistant.controller.vo.OrderVO;
import com.feijimiao.xianyuassistant.entity.XianyuOrder;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 闲鱼订单Mapper
 */
@Mapper
public interface XianyuOrderMapper extends BaseMapper<XianyuOrder> {
    
    /**
     * 分页查询订单列表（联表查询账号备注和自动发货状态）
     * 
     * @param page 分页对象
     * @param xianyuAccountId 账号ID
     * @param xyGoodsId 商品ID
     * @param orderStatus 订单状态
     * @return 订单列表
     */
    @Select("<script>" +
            "SELECT " +
            "  o.id, " +
            "  a.account_note AS accountRemark, " +
            "  o.order_id AS orderId, " +
            "  o.goods_title AS goodsTitle, " +
            "  o.s_id AS sId, " +
            "  o.order_create_time AS createTime, " +
            "  CASE WHEN r.id IS NOT NULL AND r.state = 1 THEN 1 ELSE 0 END AS autoDeliverySuccess, " +
            "  o.order_status AS orderStatus, " +
            "  o.order_status_text AS orderStatusText, " +
            "  o.buyer_user_name AS buyerUserName, " +
            "  o.xy_goods_id AS xyGoodsId " +
            "FROM xianyu_order o " +
            "LEFT JOIN xianyu_account a ON o.xianyu_account_id = a.id " +
            "LEFT JOIN xianyu_goods_auto_delivery_record r ON o.order_id = r.order_id " +
            "WHERE 1=1 " +
            "<if test='xianyuAccountId != null'> " +
            "  AND o.xianyu_account_id = #{xianyuAccountId} " +
            "</if> " +
            "<if test='xyGoodsId != null and xyGoodsId != \"\"'> " +
            "  AND o.xy_goods_id = #{xyGoodsId} " +
            "</if> " +
            "<if test='orderStatus != null'> " +
            "  AND o.order_status = #{orderStatus} " +
            "</if> " +
            "ORDER BY o.order_create_time DESC " +
            "</script>")
    Page<OrderVO> queryOrderList(
            Page<OrderVO> page,
            @Param("xianyuAccountId") Long xianyuAccountId,
            @Param("xyGoodsId") String xyGoodsId,
            @Param("orderStatus") Integer orderStatus
    );
}
