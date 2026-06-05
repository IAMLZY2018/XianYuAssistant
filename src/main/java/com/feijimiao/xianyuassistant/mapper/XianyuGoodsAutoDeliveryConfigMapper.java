package com.feijimiao.xianyuassistant.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.feijimiao.xianyuassistant.entity.XianyuGoodsAutoDeliveryConfig;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface XianyuGoodsAutoDeliveryConfigMapper extends BaseMapper<XianyuGoodsAutoDeliveryConfig> {
    
    @Select("SELECT id, xianyu_account_id, xianyu_goods_id, xy_goods_id, delivery_mode, sku_id, sku_name, auto_delivery_content, kami_config_ids, kami_delivery_template, auto_delivery_image_url, auto_confirm_shipment, rag_delay_seconds, " +
            "strftime('%Y-%m-%d %H:%M:%S', create_time) as create_time, " +
            "strftime('%Y-%m-%d %H:%M:%S', update_time) as update_time " +
            "FROM xianyu_goods_auto_delivery_config " +
            "WHERE xianyu_account_id = #{xianyuAccountId} AND xy_goods_id = #{xyGoodsId} AND sku_id = #{skuId} " +
            "LIMIT 1")
    XianyuGoodsAutoDeliveryConfig findByAccountIdAndGoodsIdAndSkuId(@Param("xianyuAccountId") Long xianyuAccountId, 
                                                                     @Param("xyGoodsId") String xyGoodsId,
                                                                     @Param("skuId") String skuId);
    
    @Select("SELECT id, xianyu_account_id, xianyu_goods_id, xy_goods_id, delivery_mode, sku_id, sku_name, auto_delivery_content, kami_config_ids, kami_delivery_template, auto_delivery_image_url, auto_confirm_shipment, rag_delay_seconds, " +
            "strftime('%Y-%m-%d %H:%M:%S', create_time) as create_time, " +
            "strftime('%Y-%m-%d %H:%M:%S', update_time) as update_time " +
            "FROM xianyu_goods_auto_delivery_config " +
            "WHERE xianyu_account_id = #{xianyuAccountId} AND xy_goods_id = #{xyGoodsId} AND sku_id IS NULL " +
            "LIMIT 1")
    XianyuGoodsAutoDeliveryConfig findByAccountIdAndGoodsIdNoSku(@Param("xianyuAccountId") Long xianyuAccountId,
                                                                  @Param("xyGoodsId") String xyGoodsId);
    
    @Select("SELECT id, xianyu_account_id, xianyu_goods_id, xy_goods_id, delivery_mode, sku_id, sku_name, auto_delivery_content, kami_config_ids, kami_delivery_template, auto_delivery_image_url, auto_confirm_shipment, rag_delay_seconds, " +
            "strftime('%Y-%m-%d %H:%M:%S', create_time) as create_time, " +
            "strftime('%Y-%m-%d %H:%M:%S', update_time) as update_time " +
            "FROM xianyu_goods_auto_delivery_config " +
            "WHERE xianyu_account_id = #{xianyuAccountId} AND xy_goods_id = #{xyGoodsId} " +
            "ORDER BY sku_id ASC")
    List<XianyuGoodsAutoDeliveryConfig> findByAccountIdAndGoodsId(@Param("xianyuAccountId") Long xianyuAccountId,
                                                                   @Param("xyGoodsId") String xyGoodsId);
    
    @Select("SELECT id, xianyu_account_id, xianyu_goods_id, xy_goods_id, delivery_mode, sku_id, sku_name, auto_delivery_content, kami_config_ids, kami_delivery_template, auto_delivery_image_url, auto_confirm_shipment, rag_delay_seconds, " +
            "strftime('%Y-%m-%d %H:%M:%S', create_time) as create_time, " +
            "strftime('%Y-%m-%d %H:%M:%S', update_time) as update_time " +
            "FROM xianyu_goods_auto_delivery_config " +
            "WHERE xianyu_account_id = #{xianyuAccountId} " +
            "ORDER BY create_time DESC")
    List<XianyuGoodsAutoDeliveryConfig> findByAccountId(@Param("xianyuAccountId") Long xianyuAccountId);
    
    @Delete("DELETE FROM xianyu_goods_auto_delivery_config WHERE xianyu_account_id = #{xianyuAccountId}")
    int deleteByAccountId(@Param("xianyuAccountId") Long xianyuAccountId);
}
