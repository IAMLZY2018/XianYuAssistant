package com.feijimiao.xianyuassistant.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.feijimiao.xianyuassistant.entity.XianyuKeywordReplyRule;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface XianyuKeywordReplyRuleMapper extends BaseMapper<XianyuKeywordReplyRule> {

    @Select("SELECT * FROM xianyu_keyword_reply_rule WHERE xianyu_account_id = #{accountId} AND xy_goods_id = #{xyGoodsId}")
    List<XianyuKeywordReplyRule> selectByAccountAndGoodsId(@Param("accountId") Long accountId, @Param("xyGoodsId") String xyGoodsId);

    @Select("SELECT * FROM xianyu_keyword_reply_rule WHERE xianyu_account_id = #{accountId} AND xy_goods_id = #{xyGoodsId} AND keyword = #{keyword} AND is_fallback = 0")
    XianyuKeywordReplyRule selectByKeyword(@Param("accountId") Long accountId, @Param("xyGoodsId") String xyGoodsId, @Param("keyword") String keyword);

    @Select("SELECT * FROM xianyu_keyword_reply_rule WHERE xianyu_account_id = #{accountId} AND xy_goods_id = #{xyGoodsId} AND is_fallback = 1")
    XianyuKeywordReplyRule selectFallback(@Param("accountId") Long accountId, @Param("xyGoodsId") String xyGoodsId);

    @Select("SELECT * FROM xianyu_keyword_reply_rule WHERE xianyu_account_id = #{accountId} AND xy_goods_id = #{xyGoodsId} AND match_mode = 2 AND keyword = #{message} AND is_fallback = 0")
    List<XianyuKeywordReplyRule> matchExact(@Param("accountId") Long accountId, @Param("xyGoodsId") String xyGoodsId, @Param("message") String message);

    @Select("SELECT * FROM xianyu_keyword_reply_rule WHERE xianyu_account_id = #{accountId} AND xy_goods_id = #{xyGoodsId} AND match_mode = 1 AND #{message} LIKE '%' || keyword || '%' AND is_fallback = 0")
    List<XianyuKeywordReplyRule> matchFuzzy(@Param("accountId") Long accountId, @Param("xyGoodsId") String xyGoodsId, @Param("message") String message);

    @Delete("DELETE FROM xianyu_keyword_reply_rule WHERE id = #{id}")
    int deleteById(@Param("id") Long id);
}
