package com.feijimiao.xianyuassistant.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.feijimiao.xianyuassistant.entity.XianyuKeywordReplyContent;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface XianyuKeywordReplyContentMapper extends BaseMapper<XianyuKeywordReplyContent> {

    @Select("SELECT * FROM xianyu_keyword_reply_content WHERE rule_id = #{ruleId}")
    List<XianyuKeywordReplyContent> selectByRuleId(@Param("ruleId") Long ruleId);

    @Delete("DELETE FROM xianyu_keyword_reply_content WHERE rule_id = #{ruleId}")
    int deleteByRuleId(@Param("ruleId") Long ruleId);
}
