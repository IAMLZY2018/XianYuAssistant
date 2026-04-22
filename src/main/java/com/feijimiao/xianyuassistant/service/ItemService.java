package com.feijimiao.xianyuassistant.service;

import com.feijimiao.xianyuassistant.common.ResultObject;
import com.feijimiao.xianyuassistant.controller.dto.*;

/**
 * 商品服务接口
 */
public interface ItemService {

    /**
     * 刷新商品数据
     * 从闲鱼API获取最新商品信息并更新到数据库
     *
     * @param reqDTO 请求参数
     * @return 刷新结果（包含更新成功的商品ID列表）
     */
    ResultObject<RefreshItemsRespDTO> refreshItems(AllItemsReqDTO reqDTO);
    
    /**
     * 从数据库获取商品列表
     *
     * @param reqDTO 请求参数
     * @return 商品列表
     */
    ResultObject<ItemListFromDbRespDTO> getItemsFromDb(ItemListFromDbReqDTO reqDTO);
    
    /**
     * 获取商品详情
     *
     * @param reqDTO 请求参数
     * @return 商品详情
     */
    ResultObject<ItemDetailRespDTO> getItemDetail(ItemDetailReqDTO reqDTO);
    
    /**
     * 更新商品自动发货状态
     *
     * @param reqDTO 请求参数
     * @return 更新结果
     */
    ResultObject<UpdateAutoDeliveryRespDTO> updateAutoDeliveryStatus(UpdateAutoDeliveryReqDTO reqDTO);
    
    /**
     * 更新商品自动回复状态
     *
     * @param reqDTO 请求参数
     * @return 更新结果
     */
    ResultObject<UpdateAutoReplyRespDTO> updateAutoReplyStatus(UpdateAutoReplyReqDTO reqDTO);
    
    /**
     * 删除商品
     *
     * @param reqDTO 请求参数
     * @return 删除结果
     */
    ResultObject<DeleteItemRespDTO> deleteItem(DeleteItemReqDTO reqDTO);
    
    /**
     * 获取RAG自动回复配置
     *
     * @param reqDTO 请求参数
     * @return RAG配置
     */
    ResultObject<RagAutoReplyConfigRespDTO> getRagAutoReplyConfig(RagAutoReplyConfigReqDTO reqDTO);
    
    /**
     * 更新RAG自动回复配置
     *
     * @param reqDTO 请求参数
     * @return 更新结果
     */
    ResultObject<?> updateRagAutoReplyConfig(UpdateRagAutoReplyConfigReqDTO reqDTO);
}