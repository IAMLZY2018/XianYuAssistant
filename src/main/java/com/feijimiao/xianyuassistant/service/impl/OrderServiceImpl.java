package com.feijimiao.xianyuassistant.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.feijimiao.xianyuassistant.controller.dto.OrderQueryReqDTO;
import com.feijimiao.xianyuassistant.controller.vo.OrderVO;
import com.feijimiao.xianyuassistant.entity.XianyuOrder;
import com.feijimiao.xianyuassistant.mapper.XianyuOrderMapper;
import com.feijimiao.xianyuassistant.service.OrderService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * 订单服务实现
 */
@Slf4j
@Service
public class OrderServiceImpl implements OrderService {
    
    @Autowired
    private XianyuOrderMapper orderMapper;
    
    @Override
    public Long saveOrUpdateOrder(XianyuOrder order) {
        try {
            // 查询是否已存在
            XianyuOrder existingOrder = getOrderByAccountIdAndOrderId(
                    order.getXianyuAccountId(), 
                    order.getOrderId()
            );
            
            if (existingOrder != null) {
                // 更新已存在的订单
                order.setId(existingOrder.getId());
                order.setUpdateTime(LocalDateTime.now());
                orderMapper.updateById(order);
                log.info("更新订单成功: orderId={}, accountId={}", 
                        order.getOrderId(), order.getXianyuAccountId());
                return existingOrder.getId();
            } else {
                // 插入新订单
                order.setCreateTime(LocalDateTime.now());
                order.setUpdateTime(LocalDateTime.now());
                orderMapper.insert(order);
                log.info("保存新订单成功: orderId={}, accountId={}, id={}", 
                        order.getOrderId(), order.getXianyuAccountId(), order.getId());
                return order.getId();
            }
            
        } catch (Exception e) {
            log.error("保存或更新订单失败: orderId={}, accountId={}", 
                    order.getOrderId(), order.getXianyuAccountId(), e);
            return null;
        }
    }
    
    @Override
    public XianyuOrder getOrderByOrderId(String orderId) {
        try {
            return orderMapper.selectOne(
                    new LambdaQueryWrapper<XianyuOrder>()
                            .eq(XianyuOrder::getOrderId, orderId)
            );
        } catch (Exception e) {
            log.error("查询订单失败: orderId={}", orderId, e);
            return null;
        }
    }
    
    @Override
    public XianyuOrder getOrderByAccountIdAndOrderId(Long accountId, String orderId) {
        try {
            return orderMapper.selectOne(
                    new LambdaQueryWrapper<XianyuOrder>()
                            .eq(XianyuOrder::getXianyuAccountId, accountId)
                            .eq(XianyuOrder::getOrderId, orderId)
            );
        } catch (Exception e) {
            log.error("查询订单失败: accountId={}, orderId={}", accountId, orderId, e);
            return null;
        }
    }
    
    @Override
    public String confirmShipment(Long accountId, String orderId) {
        try {
            // 查询订单
            XianyuOrder order = getOrderByAccountIdAndOrderId(accountId, orderId);
            if (order == null) {
                log.warn("订单不存在: accountId={}, orderId={}", accountId, orderId);
                return null;
            }
            
            // 更新订单状态为已发货
            order.setOrderStatus(3);
            order.setOrderStatusText("已发货");
            order.setOrderDeliveryTime(System.currentTimeMillis());
            order.setUpdateTime(LocalDateTime.now());
            
            int result = orderMapper.updateById(order);
            if (result > 0) {
                log.info("确认发货成功: accountId={}, orderId={}", accountId, orderId);
                return "确认发货成功";
            } else {
                log.error("确认发货失败: accountId={}, orderId={}", accountId, orderId);
                return null;
            }
            
        } catch (Exception e) {
            log.error("确认发货异常: accountId={}, orderId={}", accountId, orderId, e);
            return null;
        }
    }
    
    @Override
    public Page<OrderVO> queryOrderList(OrderQueryReqDTO reqDTO) {
        try {
            // 创建分页对象
            Page<OrderVO> page = new Page<>(reqDTO.getPageNum(), reqDTO.getPageSize());
            
            // 调用Mapper联表查询
            Page<OrderVO> result = orderMapper.queryOrderList(
                    page,
                    reqDTO.getXianyuAccountId(),
                    reqDTO.getXyGoodsId(),
                    reqDTO.getOrderStatus()
            );
            
            log.info("查询订单列表成功: total={}, current={}, size={}", 
                    result.getTotal(), result.getCurrent(), result.getSize());
            
            return result;
            
        } catch (Exception e) {
            log.error("查询订单列表失败", e);
            return new Page<>();
        }
    }
}
