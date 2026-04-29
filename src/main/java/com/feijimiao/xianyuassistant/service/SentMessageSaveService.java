package com.feijimiao.xianyuassistant.service;

/**
 * 发送消息入库服务
 * 
 * <p>自己发送或AI自动回复的消息，闲鱼不会推送同步消息，需要主动入库</p>
 * 
 * <p>contentType标记：</p>
 * <ul>
 *   <li>999 - 手动回复（前端手动发送）</li>
 *   <li>888 - 自动回复（AI自动回复）</li>
 * </ul>
 */
public interface SentMessageSaveService {
    
    /**
     * 保存手动回复消息到数据库
     * contentType = 999，中文标签："手动回复"
     * 发送者名称通过UserContext获取当前登录用户名
     * 
     * @param accountId 闲鱼账号ID
     * @param cid 会话ID
     * @param toId 接收方用户ID
     * @param text 消息内容
     * @param xyGoodsId 闲鱼商品ID
     */
    void saveManualReply(Long accountId, String cid, String toId, String text, String xyGoodsId);
    
    /**
     * 保存AI助手回复消息到数据库
     * contentType = 888，中文标签："自动回复"，发送者："AI助手"
     * 
     * @param accountId 闲鱼账号ID
     * @param cid 会话ID
     * @param toId 接收方用户ID
     * @param text 消息内容
     * @param xyGoodsId 闲鱼商品ID
     */
    void saveAiAssistantReply(Long accountId, String cid, String toId, String text, String xyGoodsId);

    /**
     * 保存手动回复图片消息到数据库
     * contentType = 997，中文标签："图片回复"
     *
     * @param accountId 闲鱼账号ID
     * @param cid 会话ID
     * @param toId 接收方用户ID
     * @param imageUrl 图片URL
     * @param xyGoodsId 闲鱼商品ID
     */
    void saveManualImageReply(Long accountId, String cid, String toId, String imageUrl, String xyGoodsId);
}
