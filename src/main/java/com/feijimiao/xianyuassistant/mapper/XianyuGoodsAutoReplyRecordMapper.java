package com.feijimiao.xianyuassistant.mapper;

import com.feijimiao.xianyuassistant.entity.XianyuGoodsAutoReplyRecord;
import org.apache.ibatis.annotations.*;

import java.util.List;

/**
 * 商品自动回复记录Mapper
 */
@Mapper
public interface XianyuGoodsAutoReplyRecordMapper {
    
    /**
     * 插入记录
     */
    @Insert("INSERT INTO xianyu_goods_auto_reply_record (xianyu_account_id, xianyu_goods_id, xy_goods_id, s_id, pnm_id, buyer_user_id, buyer_user_name, buyer_message, reply_content, reply_type, matched_keyword, state) " +
            "VALUES (#{xianyuAccountId}, #{xianyuGoodsId}, #{xyGoodsId}, #{sId}, #{pnmId}, #{buyerUserId}, #{buyerUserName}, #{buyerMessage}, #{replyContent}, #{replyType}, #{matchedKeyword}, #{state})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(XianyuGoodsAutoReplyRecord record);
    
    /**
     * 更新记录状态和回复内容
     */
    @Update("UPDATE xianyu_goods_auto_reply_record SET state = #{state}, reply_content = #{replyContent} WHERE id = #{id}")
    int updateStateAndContent(@Param("id") Long id, @Param("state") Integer state, @Param("replyContent") String replyContent);
    
    /**
     * 根据账号ID查询记录
     */
    @Select("SELECT * FROM xianyu_goods_auto_reply_record WHERE xianyu_account_id = #{accountId} ORDER BY create_time DESC")
    List<XianyuGoodsAutoReplyRecord> selectByAccountId(@Param("accountId") Long accountId);
    
    /**
     * 根据账号ID和会话ID查询最新记录
     */
    @Select("SELECT * FROM xianyu_goods_auto_reply_record WHERE xianyu_account_id = #{accountId} AND s_id = #{sId} ORDER BY create_time DESC LIMIT 1")
    XianyuGoodsAutoReplyRecord selectLatestByAccountIdAndSId(@Param("accountId") Long accountId, @Param("sId") String sId);
    
    /**
     * 根据账号ID删除记录
     */
    @Delete("DELETE FROM xianyu_goods_auto_reply_record WHERE xianyu_account_id = #{accountId}")
    int deleteByAccountId(@Param("accountId") Long accountId);
}