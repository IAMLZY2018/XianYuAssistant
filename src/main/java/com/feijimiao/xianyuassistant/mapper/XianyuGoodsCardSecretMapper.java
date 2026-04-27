package com.feijimiao.xianyuassistant.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.feijimiao.xianyuassistant.controller.dto.CardSecretRespDTO;
import com.feijimiao.xianyuassistant.entity.XianyuGoodsCardSecret;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface XianyuGoodsCardSecretMapper extends BaseMapper<XianyuGoodsCardSecret> {

    @Select("<script>" +
            "SELECT " +
            "  cs.id, " +
            "  cs.xianyu_account_id AS xianyuAccountId, " +
            "  cs.xy_goods_id AS xyGoodsId, " +
            "  gi.title AS goodsTitle, " +
            "  cs.card_content AS cardContent, " +
            "  cs.is_used AS isUsed, " +
            "  cs.order_id AS orderId, " +
            "  cs.create_time AS createTime, " +
            "  cs.update_time AS updateTime, " +
            "  a.account_note AS accountNote " +
            "FROM xianyu_goods_card_secret cs " +
            "LEFT JOIN xianyu_account a ON cs.xianyu_account_id = a.id " +
            "LEFT JOIN xianyu_goods_info gi ON cs.xy_goods_id = gi.item_id " +
            "WHERE 1=1 " +
            "<if test='accountId != null'> AND cs.xianyu_account_id = #{accountId} </if> " +
            "<if test='goodsId != null and goodsId != \"\"'> AND (cs.xy_goods_id LIKE '%' || #{goodsId} || '%' OR gi.title LIKE '%' || #{goodsId} || '%') </if> " +
            "<if test='isUsed != null'> AND cs.is_used = #{isUsed} </if> " +
            "<if test='cardContent != null and cardContent != \"\"'> AND cs.card_content LIKE '%' || #{cardContent} || '%' </if> " +
            "ORDER BY cs.id DESC" +
            "</script>")
    Page<CardSecretRespDTO> selectCardSecretPage(
            Page<CardSecretRespDTO> page,
            @Param("accountId") Long accountId,
            @Param("goodsId") String goodsId,
            @Param("isUsed") Integer isUsed,
            @Param("cardContent") String cardContent
    );
}
