package com.feijimiao.xianyuassistant.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.feijimiao.xianyuassistant.entity.XianyuGoodsAutoDeliveryConfig;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 商品自动发货配置Mapper
 */
@Mapper
public interface XianyuGoodsAutoDeliveryConfigMapper extends BaseMapper<XianyuGoodsAutoDeliveryConfig> {
    
    /**
     * 根据账号ID和商品ID查询配置
     *
     * @param xianyuAccountId 闲鱼账号ID
     * @param xyGoodsId 闲鱼商品ID
     * @return 自动发货配置
     */
    @Select("SELECT id, xianyu_account_id, xianyu_goods_id, xy_goods_id, delivery_mode, auto_delivery_content, kami_config_ids, kami_delivery_template, auto_confirm_shipment, " +
            "strftime('%Y-%m-%d %H:%M:%S', create_time) as create_time, " +
            "strftime('%Y-%m-%d %H:%M:%S', update_time) as update_time " +
            "FROM xianyu_goods_auto_delivery_config " +
            "WHERE xianyu_account_id = #{xianyuAccountId} AND xy_goods_id = #{xyGoodsId} " +
            "LIMIT 1")
    XianyuGoodsAutoDeliveryConfig findByAccountIdAndGoodsId(@Param("xianyuAccountId") Long xianyuAccountId, 
                                                           @Param("xyGoodsId") String xyGoodsId);
    
    /**
     * 根据账号ID查询所有配置
     *
     * @param xianyuAccountId 闲鱼账号ID
     * @return 自动发货配置列表
     */
    @Select("SELECT id, xianyu_account_id, xianyu_goods_id, xy_goods_id, delivery_mode, auto_delivery_content, kami_config_ids, kami_delivery_template, auto_confirm_shipment, " +
            "strftime('%Y-%m-%d %H:%M:%S', create_time) as create_time, " +
            "strftime('%Y-%m-%d %H:%M:%S', update_time) as update_time " +
            "FROM xianyu_goods_auto_delivery_config " +
            "WHERE xianyu_account_id = #{xianyuAccountId} " +
            "ORDER BY create_time DESC")
    List<XianyuGoodsAutoDeliveryConfig> findByAccountId(@Param("xianyuAccountId") Long xianyuAccountId);
    
    /**
     * 根据账号ID删除自动发货配置
     *
     * @param xianyuAccountId 闲鱼账号ID
     * @return 删除的记录数量
     */
    @Delete("DELETE FROM xianyu_goods_auto_delivery_config WHERE xianyu_account_id = #{xianyuAccountId}")
    int deleteByAccountId(@Param("xianyuAccountId") Long xianyuAccountId);
}