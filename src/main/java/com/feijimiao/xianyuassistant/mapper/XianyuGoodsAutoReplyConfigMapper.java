package com.feijimiao.xianyuassistant.mapper;

import com.feijimiao.xianyuassistant.entity.XianyuGoodsAutoReplyConfig;
import org.apache.ibatis.annotations.*;

import java.util.List;

/**
 * 商品自动回复配置Mapper
 */
@Mapper
public interface XianyuGoodsAutoReplyConfigMapper {
    
    /**
     * 根据账号ID和商品ID查询配置
     */
    @Select("SELECT * FROM xianyu_goods_auto_reply_config WHERE xianyu_account_id = #{accountId} AND xy_goods_id = #{xyGoodsId}")
    XianyuGoodsAutoReplyConfig selectByAccountAndGoodsId(@Param("accountId") Long accountId, @Param("xyGoodsId") String xyGoodsId);
    
    /**
     * 根据账号ID查询所有配置
     */
    @Select("SELECT * FROM xianyu_goods_auto_reply_config WHERE xianyu_account_id = #{accountId}")
    List<XianyuGoodsAutoReplyConfig> selectByAccountId(@Param("accountId") Long accountId);
    
    /**
     * 插入配置
     */
    @Insert("INSERT INTO xianyu_goods_auto_reply_config (xianyu_account_id, xianyu_goods_id, xy_goods_id, keyword, reply_content, match_type, rag_delay_seconds) " +
            "VALUES (#{xianyuAccountId}, #{xianyuGoodsId}, #{xyGoodsId}, #{keyword}, #{replyContent}, #{matchType}, #{ragDelaySeconds})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(XianyuGoodsAutoReplyConfig config);
    
    /**
     * 更新配置
     */
    @Update("UPDATE xianyu_goods_auto_reply_config SET keyword = #{keyword}, reply_content = #{replyContent}, match_type = #{matchType}, " +
            "rag_delay_seconds = #{ragDelaySeconds} WHERE id = #{id}")
    int update(XianyuGoodsAutoReplyConfig config);
    
    /**
     * 根据账号ID删除配置
     */
    @Delete("DELETE FROM xianyu_goods_auto_reply_config WHERE xianyu_account_id = #{accountId}")
    int deleteByAccountId(@Param("accountId") Long accountId);
}