package com.feijimiao.xianyuassistant.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.feijimiao.xianyuassistant.controller.dto.OrderQueryReqDTO;
import com.feijimiao.xianyuassistant.controller.vo.OrderVO;
import com.feijimiao.xianyuassistant.entity.XianyuOrder;
import com.feijimiao.xianyuassistant.mapper.XianyuOrderMapper;
import com.feijimiao.xianyuassistant.service.AccountService;
import com.feijimiao.xianyuassistant.service.OrderService;
import com.feijimiao.xianyuassistant.utils.XianyuApiCallUtils;
import com.feijimiao.xianyuassistant.utils.XianyuApiUtils;
import com.feijimiao.xianyuassistant.utils.XianyuSignUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 订单服务实现
 */
@Slf4j
@Service
public class OrderServiceImpl implements OrderService {
    
    @Autowired
    private XianyuOrderMapper orderMapper;
    
    @Autowired
    private AccountService accountService;
    
    @Autowired
    private com.feijimiao.xianyuassistant.mapper.XianyuGoodsAutoDeliveryRecordMapper autoDeliveryRecordMapper;
    
    @Autowired
    private XianyuApiCallUtils xianyuApiCallUtils;
    
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();
    
    /**
     * 确认发货API URL
     */
    private static final String CONFIRM_SHIPMENT_URL = "https://h5api.m.goofish.com/h5/mtop.taobao.idle.logistic.consign.dummy/1.0/";
    
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
        // 调用闲鱼API确认发货
        return confirmShipmentToXianyu(accountId, orderId);
    }
    
    @Override
    public String confirmShipmentToXianyu(Long accountId, String orderId) {
        try {
            log.info("【账号{}】开始调用闲鱼API确认发货: orderId={}", accountId, orderId);
            
            // 获取Cookie
            String cookieStr = accountService.getCookieByAccountId(accountId);
            if (cookieStr == null || cookieStr.isEmpty()) {
                log.error("【账号{}】未找到Cookie", accountId);
                return null;
            }
            
            // 构造data参数（参考Python代码）
            Map<String, Object> dataMap = new HashMap<>();
            dataMap.put("orderId", orderId);
            dataMap.put("tradeText", "");
            dataMap.put("picList", new String[0]);
            dataMap.put("newUnconsign", true);
            
            log.info("【账号{}】data参数: {}", accountId, dataMap);
            
            // 使用统一的API调用工具（带自动刷新机制）
            XianyuApiCallUtils.ApiCallResult result = xianyuApiCallUtils.callApiWithRetry(
                    accountId, 
                    "mtop.taobao.idle.logistic.consign.dummy", 
                    dataMap, 
                    cookieStr
            );
            
            if (!result.isSuccess()) {
                log.error("【账号{}】❌ 闲鱼API确认发货失败: {}", accountId, result.getErrorMessage());
                
                // 如果是令牌过期，返回特定错误信息
                if (result.isTokenExpired()) {
                    return "令牌过期，请稍后重试或手动更新Cookie";
                }
                
                return null;
            }
            
            // 解析响应
            Map<String, Object> responseData = result.extractData();
            if (responseData != null) {
                log.info("【账号{}】✅ 闲鱼API确认发货成功: orderId={}", accountId, orderId);
                // 更新本地数据库
                return updateOrderStatusToShipped(accountId, orderId, "确认发货成功");
            } else {
                log.error("【账号{}】响应数据格式错误", accountId);
                return null;
            }
            
        } catch (Exception e) {
            log.error("【账号{}】调用闲鱼API确认发货异常: orderId={}", accountId, orderId, e);
            return null;
        }
    }
    
    /**
     * 更新订单状态为已发货
     * 同时同步自动发货记录的状态
     */
    private String updateOrderStatusToShipped(Long accountId, String orderId, String successMessage) {
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
                log.info("【账号{}】更新订单状态成功: orderId={}, orderStatus=3(已发货)", accountId, orderId);
                
                // 同步自动发货记录的状态
                syncAutoDeliveryRecordStatus(accountId, orderId);
                
                return successMessage;
            } else {
                log.error("【账号{}】更新订单状态失败: orderId={}", accountId, orderId);
                return null;
            }
            
        } catch (Exception e) {
            log.error("【账号{}】更新订单状态异常: orderId={}", accountId, orderId, e);
            return null;
        }
    }
    
    /**
     * 同步自动发货记录的状态
     * 当订单状态更新为"已发货"(order_status=3)时,自动发货记录的状态也应该同步
     * 
     * @param accountId 账号ID
     * @param orderId 订单ID
     */
    private void syncAutoDeliveryRecordStatus(Long accountId, String orderId) {
        try {
            // 查询自动发货记录
            com.feijimiao.xianyuassistant.entity.XianyuGoodsAutoDeliveryRecord record = 
                autoDeliveryRecordMapper.selectByOrderId(accountId, orderId);
            
            if (record != null) {
                log.info("【账号{}】找到自动发货记录: recordId={}, orderId={}, 当前state={}", 
                        accountId, record.getId(), orderId, record.getState());
                
                // 如果自动发货记录存在,且发货成功(state=1),则记录日志
                // 订单状态已通过xianyu_order.order_status=3表示已确认发货
                if (record.getState() == 1) {
                    log.info("【账号{}】✅ 订单已确认发货: orderId={}, 自动发货记录ID={}, 发货状态=成功", 
                            accountId, orderId, record.getId());
                } else {
                    log.warn("【账号{}】⚠️ 订单已确认发货,但自动发货记录显示失败: orderId={}, recordId={}, state={}", 
                            accountId, orderId, record.getId(), record.getState());
                }
            } else {
                log.debug("【账号{}】未找到对应的自动发货记录: orderId={}", accountId, orderId);
            }
            
        } catch (Exception e) {
            log.error("【账号{}】同步自动发货记录状态失败: orderId={}", accountId, orderId, e);
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
