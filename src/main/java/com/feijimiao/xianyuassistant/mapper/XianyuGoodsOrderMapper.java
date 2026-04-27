package com.feijimiao.xianyuassistant.mapper;

import com.feijimiao.xianyuassistant.entity.XianyuGoodsOrder;
import org.apache.ibatis.annotations.*;

import java.util.List;

/**
 * 商品订单Mapper
 */
@Mapper
public interface XianyuGoodsOrderMapper {
    
    @Insert("INSERT INTO xianyu_goods_order (xianyu_account_id, xianyu_goods_id, xy_goods_id, pnm_id, order_id, buyer_user_id, buyer_user_name, content, state, confirm_state) " +
            "VALUES (#{xianyuAccountId}, #{xianyuGoodsId}, #{xyGoodsId}, #{pnmId}, #{orderId}, #{buyerUserId}, #{buyerUserName}, #{content}, #{state}, #{confirmState})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(XianyuGoodsOrder record);
    
    @Select("SELECT * FROM xianyu_goods_order WHERE xianyu_account_id = #{accountId} ORDER BY create_time DESC")
    List<XianyuGoodsOrder> selectByAccountId(@Param("accountId") Long accountId);
    
    @Delete("DELETE FROM xianyu_goods_order WHERE xianyu_account_id = #{accountId}")
    int deleteByAccountId(@Param("accountId") Long accountId);
    
    @Select("<script>" +
            "SELECT r.*, " +
            "g.title as goods_title " +
            "FROM xianyu_goods_order r " +
            "LEFT JOIN xianyu_goods g ON r.xy_goods_id = g.xy_good_id " +
            "WHERE r.xianyu_account_id = #{accountId} " +
            "<if test='xyGoodsId != null and xyGoodsId != \"\"'>" +
            "AND r.xy_goods_id = #{xyGoodsId} " +
            "</if>" +
            "ORDER BY r.create_time DESC " +
            "LIMIT #{limit} OFFSET #{offset}" +
            "</script>")
    @Results({
        @Result(property = "id", column = "id"),
        @Result(property = "xianyuAccountId", column = "xianyu_account_id"),
        @Result(property = "xianyuGoodsId", column = "xianyu_goods_id"),
        @Result(property = "xyGoodsId", column = "xy_goods_id"),
        @Result(property = "pnmId", column = "pnm_id"),
        @Result(property = "orderId", column = "order_id"),
        @Result(property = "buyerUserId", column = "buyer_user_id"),
        @Result(property = "buyerUserName", column = "buyer_user_name"),
        @Result(property = "content", column = "content"),
        @Result(property = "state", column = "state"),
        @Result(property = "confirmState", column = "confirm_state"),
        @Result(property = "createTime", column = "create_time"),
        @Result(property = "goodsTitle", column = "goods_title")
    })
    List<XianyuGoodsOrder> selectByAccountIdWithPage(
            @Param("accountId") Long accountId,
            @Param("xyGoodsId") String xyGoodsId,
            @Param("limit") int limit,
            @Param("offset") int offset);
    
    @Select("<script>" +
            "SELECT COUNT(*) FROM xianyu_goods_order " +
            "WHERE xianyu_account_id = #{accountId} " +
            "<if test='xyGoodsId != null and xyGoodsId != \"\"'>" +
            "AND xy_goods_id = #{xyGoodsId} " +
            "</if>" +
            "</script>")
    long countByAccountId(@Param("accountId") Long accountId, @Param("xyGoodsId") String xyGoodsId);
    
    @Update("UPDATE xianyu_goods_order SET state = #{state} WHERE id = #{id}")
    int updateState(@Param("id") Long id, @Param("state") Integer state);
    
    @Update("UPDATE xianyu_goods_order SET state = #{state}, content = #{content} WHERE id = #{id}")
    int updateStateAndContent(@Param("id") Long id, @Param("state") Integer state, @Param("content") String content);
    
    @Select("SELECT * FROM xianyu_goods_order WHERE xianyu_account_id = #{accountId} AND order_id = #{orderId}")
    XianyuGoodsOrder selectByOrderId(@Param("accountId") Long accountId, @Param("orderId") String orderId);
    
    @Update("UPDATE xianyu_goods_order SET confirm_state = 1 WHERE xianyu_account_id = #{accountId} AND order_id = #{orderId}")
    int updateConfirmState(@Param("accountId") Long accountId, @Param("orderId") String orderId);
    
    @Select("SELECT * FROM xianyu_goods_order WHERE xianyu_account_id = #{accountId} AND pnm_id = #{pnmId}")
    XianyuGoodsOrder selectByPnmId(@Param("accountId") Long accountId, @Param("pnmId") String pnmId);
}
