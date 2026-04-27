package com.feijimiao.xianyuassistant.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.feijimiao.xianyuassistant.entity.XianyuKamiItem;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

@Mapper
public interface XianyuKamiItemMapper extends BaseMapper<XianyuKamiItem> {

    @Select("SELECT * FROM xianyu_kami_item WHERE kami_config_id = #{kamiConfigId} AND status = 0 ORDER BY sort_order ASC LIMIT 1")
    XianyuKamiItem findNextUnused(@Param("kamiConfigId") Long kamiConfigId);

    @Select("SELECT * FROM xianyu_kami_item WHERE kami_config_id = #{kamiConfigId} AND status = 0 ORDER BY RANDOM() LIMIT 1")
    XianyuKamiItem findRandomUnused(@Param("kamiConfigId") Long kamiConfigId);

    @Select("SELECT * FROM xianyu_kami_item WHERE kami_config_id = #{kamiConfigId} ORDER BY sort_order ASC")
    List<XianyuKamiItem> findByConfigId(@Param("kamiConfigId") Long kamiConfigId);

    @Select("<script>" +
            "SELECT * FROM xianyu_kami_item WHERE kami_config_id = #{kamiConfigId} " +
            "<if test='status != null'>" +
            "AND status = #{status} " +
            "</if>" +
            "<if test='keyword != null and keyword != \"\"'>" +
            "AND kami_content LIKE '%' || #{keyword} || '%' " +
            "</if>" +
            "ORDER BY sort_order ASC" +
            "</script>")
    List<XianyuKamiItem> findByConfigIdWithFilter(
            @Param("kamiConfigId") Long kamiConfigId,
            @Param("status") Integer status,
            @Param("keyword") String keyword);

    @Select("SELECT COUNT(*) FROM xianyu_kami_item WHERE kami_config_id = #{kamiConfigId} AND status = 0")
    int countUnused(@Param("kamiConfigId") Long kamiConfigId);

    @Select("SELECT COUNT(*) FROM xianyu_kami_item WHERE kami_config_id = #{kamiConfigId} AND status = 1")
    int countUsed(@Param("kamiConfigId") Long kamiConfigId);

    @Select("SELECT COUNT(*) FROM xianyu_kami_item WHERE kami_config_id = #{kamiConfigId}")
    int countByConfigId(@Param("kamiConfigId") Long kamiConfigId);

    @Update("UPDATE xianyu_kami_item SET status = 1, order_id = #{orderId}, used_time = datetime('now', 'localtime') WHERE id = #{id} AND status = 0")
    int markUsed(@Param("id") Long id, @Param("orderId") String orderId);

    @Update("UPDATE xianyu_kami_item SET status = 0, order_id = NULL, used_time = NULL WHERE id = #{id} AND status = 1")
    int markUnused(@Param("id") Long id);
}
