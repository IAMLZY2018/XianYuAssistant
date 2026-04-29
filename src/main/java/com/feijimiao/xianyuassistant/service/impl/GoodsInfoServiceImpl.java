package com.feijimiao.xianyuassistant.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.feijimiao.xianyuassistant.entity.XianyuGoodsInfo;
import com.feijimiao.xianyuassistant.mapper.XianyuGoodsInfoMapper;
import com.feijimiao.xianyuassistant.controller.dto.ItemDTO;
import com.feijimiao.xianyuassistant.service.GoodsInfoService;
import com.feijimiao.xianyuassistant.utils.ItemDetailUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * 商品信息服务实现类
 */
@Slf4j
@Service
public class GoodsInfoServiceImpl implements GoodsInfoService {

    @Autowired
    private XianyuGoodsInfoMapper goodsInfoMapper;
    
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    private static final DateTimeFormatter DATETIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    
    /**
     * 获取当前时间字符串
     */
    private String getCurrentTimeString() {
        return LocalDateTime.now().format(DATETIME_FORMATTER);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean saveOrUpdateGoodsInfo(ItemDTO itemDTO, Long xianyuAccountId) {
        try {
            if (itemDTO == null || itemDTO.getDetailParams() == null) {
                log.warn("商品信息为空，跳过保存");
                return false;
            }
            
            String xyGoodId = itemDTO.getDetailParams().getItemId();
            if (xyGoodId == null || xyGoodId.isEmpty()) {
                log.warn("商品ID为空，跳过保存");
                return false;
            }
            
            // 查询是否已存在
            LambdaQueryWrapper<XianyuGoodsInfo> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(XianyuGoodsInfo::getXyGoodId, xyGoodId);
            XianyuGoodsInfo existingGoods = goodsInfoMapper.selectOne(queryWrapper);
            
            // 构建商品信息
            XianyuGoodsInfo goodsInfo = new XianyuGoodsInfo();
            goodsInfo.setXyGoodId(xyGoodId);
            goodsInfo.setTitle(itemDTO.getDetailParams().getTitle());
            goodsInfo.setCoverPic(itemDTO.getDetailParams().getPicUrl());
            
            // 将图片信息JSON数组保存到info_pic字段
            String infoPic = itemDTO.getDetailParams().getImageInfos();
            goodsInfo.setInfoPic(infoPic);
            
            // 商品详情页URL
            goodsInfo.setDetailUrl(itemDTO.getDetailUrl());
            
            // 关联闲鱼账号ID
            goodsInfo.setXianyuAccountId(xianyuAccountId);
            
            // 价格信息
            if (itemDTO.getPriceInfo() != null) {
                goodsInfo.setSoldPrice(itemDTO.getPriceInfo().getPrice());
            }
            
            // 商品状态
            goodsInfo.setStatus(itemDTO.getItemStatus());
            
            if (existingGoods != null) {
                // 更新现有商品
                goodsInfo.setId(existingGoods.getId());
                goodsInfo.setUpdatedTime(getCurrentTimeString());
                int updated = goodsInfoMapper.updateById(goodsInfo);
                log.info("更新商品信息: xyGoodId={}, title={}, accountId={}", xyGoodId, goodsInfo.getTitle(), xianyuAccountId);
                return updated > 0;
            } else {
                // 新增商品（ID使用雪花算法自动生成）
                goodsInfo.setCreatedTime(getCurrentTimeString());
                goodsInfo.setUpdatedTime(getCurrentTimeString());
                int inserted = goodsInfoMapper.insert(goodsInfo);
                log.info("新增商品信息: xyGoodId={}, title={}, id={}, accountId={}", 
                        xyGoodId, goodsInfo.getTitle(), goodsInfo.getId(), xianyuAccountId);
                return inserted > 0;
            }
            
        } catch (Exception e) {
            log.error("保存或更新商品信息失败: itemId={}", 
                    itemDTO.getDetailParams() != null ? itemDTO.getDetailParams().getItemId() : "null", e);
            throw new RuntimeException("保存或更新商品信息失败: " + e.getMessage(), e);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int batchSaveOrUpdateGoodsInfo(List<ItemDTO> itemList, Long xianyuAccountId) {
        if (itemList == null || itemList.isEmpty()) {
            log.warn("商品列表为空，跳过批量保存");
            return 0;
        }
        
        int successCount = 0;
        for (ItemDTO itemDTO : itemList) {
            try {
                if (saveOrUpdateGoodsInfo(itemDTO, xianyuAccountId)) {
                    successCount++;
                }
            } catch (Exception e) {
                log.error("批量保存商品信息时出错: itemId={}", 
                        itemDTO.getDetailParams() != null ? itemDTO.getDetailParams().getItemId() : "null", e);
                // 继续处理下一个商品
            }
        }
        
        log.info("批量保存商品信息完成: 总数={}, 成功={}, accountId={}", itemList.size(), successCount, xianyuAccountId);
        return successCount;
    }

    @Override
    public XianyuGoodsInfo getByXyGoodId(String xyGoodId) {
        try {
            LambdaQueryWrapper<XianyuGoodsInfo> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(XianyuGoodsInfo::getXyGoodId, xyGoodId);
            return goodsInfoMapper.selectOne(queryWrapper);
        } catch (Exception e) {
            log.error("根据闲鱼商品ID查询商品信息失败: xyGoodId={}", xyGoodId, e);
            return null;
        }
    }

    @Override
    public List<XianyuGoodsInfo> listByStatus(Integer status) {
        try {
            LambdaQueryWrapper<XianyuGoodsInfo> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(XianyuGoodsInfo::getStatus, status);
            queryWrapper.orderByDesc(XianyuGoodsInfo::getUpdatedTime);
            return goodsInfoMapper.selectList(queryWrapper);
        } catch (Exception e) {
            log.error("根据状态查询商品列表失败: status={}", status, e);
            return null;
        }
    }
    
    @Override
    public List<XianyuGoodsInfo> listByStatusAndAccountId(Integer status, Long xianyuAccountId) {
        try {
            LambdaQueryWrapper<XianyuGoodsInfo> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(XianyuGoodsInfo::getStatus, status);
            if (xianyuAccountId != null) {
                queryWrapper.eq(XianyuGoodsInfo::getXianyuAccountId, xianyuAccountId);
            }
            queryWrapper.orderByDesc(XianyuGoodsInfo::getUpdatedTime);
            return goodsInfoMapper.selectList(queryWrapper);
        } catch (Exception e) {
            log.error("根据状态和账号ID查询商品列表失败: status={}, accountId={}", status, xianyuAccountId, e);
            return null;
        }
    }
    
    @Override
    public List<XianyuGoodsInfo> listByStatus(Integer status, int pageNum, int pageSize) {
        try {
            LambdaQueryWrapper<XianyuGoodsInfo> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(XianyuGoodsInfo::getStatus, status);
            queryWrapper.orderByDesc(XianyuGoodsInfo::getUpdatedTime);
            
            // 计算偏移量
            int offset = (pageNum - 1) * pageSize;
            
            // 使用MyBatis Plus的分页查询
            return goodsInfoMapper.selectList(queryWrapper.last("LIMIT " + offset + ", " + pageSize));
        } catch (Exception e) {
            log.error("根据状态查询商品列表失败: status={}, pageNum={}, pageSize={}", status, pageNum, pageSize, e);
            return new java.util.ArrayList<>(); // 返回空列表而不是null
        }
    }
    
    @Override
    public List<XianyuGoodsInfo> listByStatusAndAccountId(Integer status, Long xianyuAccountId, int pageNum, int pageSize) {
        try {
            LambdaQueryWrapper<XianyuGoodsInfo> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(XianyuGoodsInfo::getStatus, status);
            if (xianyuAccountId != null) {
                queryWrapper.eq(XianyuGoodsInfo::getXianyuAccountId, xianyuAccountId);
            }
            queryWrapper.orderByDesc(XianyuGoodsInfo::getUpdatedTime);
            
            // 计算偏移量
            int offset = (pageNum - 1) * pageSize;
            
            // 使用MyBatis Plus的分页查询
            return goodsInfoMapper.selectList(queryWrapper.last("LIMIT " + offset + ", " + pageSize));
        } catch (Exception e) {
            log.error("根据状态和账号ID查询商品列表失败: status={}, accountId={}, pageNum={}, pageSize={}", 
                    status, xianyuAccountId, pageNum, pageSize, e);
            return new java.util.ArrayList<>(); // 返回空列表而不是null
        }
    }
    
    @Override
    public int countByStatusAndAccountId(Integer status, Long xianyuAccountId) {
        try {
            LambdaQueryWrapper<XianyuGoodsInfo> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(XianyuGoodsInfo::getStatus, status);
            if (xianyuAccountId != null) {
                queryWrapper.eq(XianyuGoodsInfo::getXianyuAccountId, xianyuAccountId);
            }
            return Math.toIntExact(goodsInfoMapper.selectCount(queryWrapper));
        } catch (Exception e) {
            log.error("根据状态和账号ID统计商品数量失败: status={}, accountId={}", status, xianyuAccountId, e);
            return 0;
        }
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateDetailInfo(String xyGoodId, String detailInfo) {
        try {
            // 提取desc字段
            String extractedDesc = ItemDetailUtils.extractDescFromDetailJson(detailInfo);
            
            LambdaQueryWrapper<XianyuGoodsInfo> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(XianyuGoodsInfo::getXyGoodId, xyGoodId);
            XianyuGoodsInfo existingGoods = goodsInfoMapper.selectOne(queryWrapper);
            
            if (existingGoods == null) {
                log.warn("商品不存在，无法更新详情: xyGoodId={}", xyGoodId);
                return false;
            }
            
            existingGoods.setDetailInfo(extractedDesc);
            existingGoods.setUpdatedTime(getCurrentTimeString());
            int updated = goodsInfoMapper.updateById(existingGoods);
            
            log.info("更新商品详情成功: xyGoodId={}, 原始详情长度={}, 提取后长度={}", 
                    xyGoodId, detailInfo != null ? detailInfo.length() : 0, 
                    extractedDesc != null ? extractedDesc.length() : 0);
            return updated > 0;
        } catch (Exception e) {
            log.error("更新商品详情失败: xyGoodId={}", xyGoodId, e);
            throw new RuntimeException("更新商品详情失败: " + e.getMessage(), e);
        }
    }
    
    @Override
    public String getDetailInfoByGoodsId(String xyGoodId) {
        LambdaQueryWrapper<XianyuGoodsInfo> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(XianyuGoodsInfo::getXyGoodId, xyGoodId);
        XianyuGoodsInfo goods = goodsInfoMapper.selectOne(queryWrapper);
        return goods != null ? goods.getDetailInfo() : null;
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteGoodsInfo(Long xianyuAccountId, String xyGoodId) {
        try {
            // 查询商品信息
            LambdaQueryWrapper<XianyuGoodsInfo> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(XianyuGoodsInfo::getXyGoodId, xyGoodId);
            queryWrapper.eq(XianyuGoodsInfo::getXianyuAccountId, xianyuAccountId);
            XianyuGoodsInfo existingGoods = goodsInfoMapper.selectOne(queryWrapper);
            
            if (existingGoods == null) {
                log.warn("商品不存在，无法删除: xyGoodId={}, accountId={}", xyGoodId, xianyuAccountId);
                return false;
            }
            
            // 删除商品
            int deleted = goodsInfoMapper.deleteById(existingGoods.getId());
            
            log.info("删除商品成功: xyGoodId={}, title={}, id={}, accountId={}", 
                    xyGoodId, existingGoods.getTitle(), existingGoods.getId(), xianyuAccountId);
            return deleted > 0;
        } catch (Exception e) {
            log.error("删除商品失败: xyGoodId={}, accountId={}", xyGoodId, xianyuAccountId, e);
            throw new RuntimeException("删除商品失败: " + e.getMessage(), e);
        }
    }
}