package com.feijimiao.xianyuassistant.mapper;

import com.feijimiao.xianyuassistant.entity.XianyuChatMessage;
import org.apache.ibatis.annotations.*;

import java.util.List;

/**
 * 闲鱼聊天消息Mapper
 */
@Mapper
public interface XianyuChatMessageMapper {
    
    /**
     * 插入聊天消息
     */
    @Insert("INSERT INTO xianyu_chat_message (" +
            "xianyu_account_id, lwp, pnm_id, s_id, " +
            "content_type, msg_content, " +
            "sender_user_name, sender_user_id, sender_app_v, sender_os_type, " +
            "reminder_url, xy_goods_id, complete_msg, message_time" +
            ") VALUES (" +
            "#{xianyuAccountId}, #{lwp}, #{pnmId}, #{sId}, " +
            "#{contentType}, #{msgContent}, " +
            "#{senderUserName}, #{senderUserId}, #{senderAppV}, #{senderOsType}, " +
            "#{reminderUrl}, #{xyGoodsId}, #{completeMsg}, #{messageTime}" +
            ")")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(XianyuChatMessage message);
    
    /**
     * 根据pnm_id查询（防止重复）
     */
    @Select("SELECT * FROM xianyu_chat_message " +
            "WHERE xianyu_account_id = #{accountId} AND pnm_id = #{pnmId}")
    XianyuChatMessage findByPnmId(@Param("accountId") Long accountId, 
                                  @Param("pnmId") String pnmId);
    
    /**
     * 查询账号的所有消息
     */
    @Select("SELECT * FROM xianyu_chat_message " +
            "WHERE xianyu_account_id = #{accountId} " +
            "ORDER BY message_time DESC " +
            "LIMIT #{limit} OFFSET #{offset}")
    List<XianyuChatMessage> findByAccountId(@Param("accountId") Long accountId,
                                            @Param("limit") int limit,
                                            @Param("offset") int offset);
    
    /**
     * 根据s_id查询会话的消息
     */
    @Select("SELECT * FROM xianyu_chat_message " +
            "WHERE s_id = #{sId} " +
            "ORDER BY message_time ASC")
    List<XianyuChatMessage> findBySId(@Param("sId") String sId);
    
    /**
     * 根据发送者ID查询消息
     */
    @Select("SELECT * FROM xianyu_chat_message " +
            "WHERE sender_user_id = #{senderUserId} " +
            "ORDER BY message_time DESC")
    List<XianyuChatMessage> findBySenderUserId(@Param("senderUserId") String senderUserId);
    
    /**
     * 根据账号ID删除消息
     */
    @Delete("DELETE FROM xianyu_chat_message WHERE xianyu_account_id = #{accountId}")
    int deleteByAccountId(@Param("accountId") Long accountId);
    
    /**
     * 分页查询消息（支持按xy_goods_id过滤和sender_user_id过滤）
     *
     * @param accountId 账号ID（必选）
     * @param xyGoodsId 商品ID（可选，为null时不过滤）
     * @param senderUserId 发送者用户ID（可选，为null时不过滤）
     * @param limit 每页数量
     * @param offset 偏移量
     * @return 消息列表
     */
    @Select("<script>" +
            "SELECT * FROM xianyu_chat_message " +
            "WHERE xianyu_account_id = #{accountId} " +
            "<if test='xyGoodsId != null and xyGoodsId != \"\"'>" +
            "AND xy_goods_id = #{xyGoodsId} " +
            "</if>" +
            "<if test='senderUserId != null and senderUserId != \"\"'>" +
            "AND sender_user_id != #{senderUserId} " +
            "</if>" +
            "ORDER BY message_time DESC " +
            "LIMIT #{limit} OFFSET #{offset}" +
            "</script>")
    List<XianyuChatMessage> findMessagesByPage(@Param("accountId") Long accountId,
                                               @Param("xyGoodsId") String xyGoodsId,
                                               @Param("senderUserId") String senderUserId,
                                               @Param("limit") int limit,
                                               @Param("offset") int offset);
    
    /**
     * 统计消息总数（支持按xy_goods_id过滤和sender_user_id过滤）
     *
     * @param accountId 账号ID（必选）
     * @param xyGoodsId 商品ID（可选，为null时不过滤）
     * @param senderUserId 发送者用户ID（可选，为null时不过滤）
     * @return 消息总数
     */
    @Select("<script>" +
            "SELECT COUNT(*) FROM xianyu_chat_message " +
            "WHERE xianyu_account_id = #{accountId} " +
            "<if test='xyGoodsId != null and xyGoodsId != \"\"'>" +
            "AND xy_goods_id = #{xyGoodsId} " +
            "</if>" +
            "<if test='senderUserId != null and senderUserId != \"\"'>" +
            "AND sender_user_id != #{senderUserId} " +
            "</if>" +
            "</script>")
    int countMessages(@Param("accountId") Long accountId,
                     @Param("xyGoodsId") String xyGoodsId,
                     @Param("senderUserId") String senderUserId);
    
    /**
     * 根据会话ID查询最近N条消息（支持分页）
     *
     * @param sId 会话ID
     * @param limit 限制条数
     * @param offset 偏移量
     * @return 消息列表
     */
    @Select("SELECT * FROM xianyu_chat_message " +
            "WHERE s_id = #{sId} " +
            "ORDER BY message_time DESC " +
            "LIMIT #{limit} OFFSET #{offset}")
    List<XianyuChatMessage> findRecentBySId(@Param("sId") String sId, @Param("limit") int limit, @Param("offset") int offset);
}