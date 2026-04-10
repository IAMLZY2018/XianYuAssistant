package com.feijimiao.xianyuassistant.service;

import com.feijimiao.xianyuassistant.entity.XianyuGoodsInfo;
import com.feijimiao.xianyuassistant.controller.dto.ItemDTO;

import java.util.List;

/**
 * 商品信息服务接口
 */
public interface GoodsInfoService {
    
    /**
     * 保存或更新商品信息
     *
     * @param itemDTO 商品信息DTO
     * @param xianyuAccountId 闲鱼账号ID
     * @return 是否保存成功
     */
    boolean saveOrUpdateGoodsInfo(ItemDTO itemDTO, Long xianyuAccountId);
    
    /**
     * 批量保存或更新商品信息
     *
     * @param itemList 商品信息列表
     * @param xianyuAccountId 闲鱼账号ID
     * @return 成功保存的商品数量
     */
    int batchSaveOrUpdateGoodsInfo(List<ItemDTO> itemList, Long xianyuAccountId);
    
    /**
     * 根据闲鱼商品ID获取商品信息
     *
     * @param xyGoodId 闲鱼商品ID
     * @return 商品信息
     */
    XianyuGoodsInfo getByXyGoodId(String xyGoodId);
    
    /**
     * 根据状态查询商品列表
     *
     * @param status 商品状态
     * @return 商品列表
     */
    List<XianyuGoodsInfo> listByStatus(Integer status);
    
    /**
     * 根据状态和账号ID查询商品列表
     *
     * @param status 商品状态
     * @param xianyuAccountId 闲鱼账号ID
     * @return 商品列表
     */
    List<XianyuGoodsInfo> listByStatusAndAccountId(Integer status, Long xianyuAccountId);
    
    /**
     * 根据状态查询商品列表（分页）
     *
     * @param status 商品状态
     * @param pageNum 页码
     * @param pageSize 每页数量
     * @return 商品列表
     */
    List<XianyuGoodsInfo> listByStatus(Integer status, int pageNum, int pageSize);
    
    /**
     * 根据状态和账号ID查询商品列表（分页）
     *
     * @param status 商品状态
     * @param xianyuAccountId 闲鱼账号ID
     * @param pageNum 页码
     * @param pageSize 每页数量
     * @return 商品列表
     */
    List<XianyuGoodsInfo> listByStatusAndAccountId(Integer status, Long xianyuAccountId, int pageNum, int pageSize);
    
    /**
     * 根据状态和账号ID统计商品数量
     *
     * @param status 商品状态
     * @param xianyuAccountId 闲鱼账号ID
     * @return 商品数量
     */
    int countByStatusAndAccountId(Integer status, Long xianyuAccountId);
    
    /**
     * 更新商品详情信息
     *
     * @param xyGoodId 闲鱼商品ID
     * @param detailInfo 商品详情信息
     * @return 是否更新成功
     */
    boolean updateDetailInfo(String xyGoodId, String detailInfo);
    
    /**
     * 删除商品信息
     *
     * @param xianyuAccountId 闲鱼账号ID
     * @param xyGoodId 闲鱼商品ID
     * @return 是否删除成功
     */
    boolean deleteGoodsInfo(Long xianyuAccountId, String xyGoodId);
}