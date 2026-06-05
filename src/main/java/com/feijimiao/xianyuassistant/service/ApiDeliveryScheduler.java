package com.feijimiao.xianyuassistant.service;

import com.feijimiao.xianyuassistant.entity.XianyuAccount;
import com.feijimiao.xianyuassistant.entity.XianyuGoodsConfig;
import com.feijimiao.xianyuassistant.entity.XianyuGoodsOrder;
import com.feijimiao.xianyuassistant.mapper.XianyuAccountMapper;
import com.feijimiao.xianyuassistant.mapper.XianyuGoodsConfigMapper;
import com.feijimiao.xianyuassistant.mapper.XianyuGoodsOrderMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

@Slf4j
@Service
public class ApiDeliveryScheduler {

    @Autowired
    private WebSocketService webSocketService;

    @Autowired
    private OrderService orderService;

    @Autowired
    private PendingOrderPollService pendingOrderPollService;

    @Autowired
    private XianyuAccountMapper accountMapper;

    @Autowired
    private XianyuGoodsConfigMapper goodsConfigMapper;

    @Autowired
    private XianyuGoodsOrderMapper orderMapper;

    private volatile boolean running = false;

    @Scheduled(fixedDelay = 25000, initialDelay = 60000)
    public void scheduledApiDelivery() {
        if (running) return;
        running = true;
        try {
            List<XianyuAccount> accounts = accountMapper.selectList(null);
            if (accounts == null || accounts.isEmpty()) return;

            for (XianyuAccount account : accounts) {
                try {
                    if (webSocketService.isConnected(account.getId())) {
                        continue;
                    }
                    pollAndDeliver(account.getId());
                } catch (Exception e) {
                    log.warn("【账号{}】API发货轮询异常: {}", account.getId(), e.getMessage());
                }
            }
        } finally {
            running = false;
        }
    }

    @SuppressWarnings("unchecked")
    private void pollAndDeliver(Long accountId) {
        List<Map<String, Object>> pendingOrders = orderService.queryPendingOrders(accountId);
        if (pendingOrders == null || pendingOrders.isEmpty()) return;

        pendingOrderPollService.syncOrdersToDb(accountId, pendingOrders);

        List<XianyuGoodsConfig> goodsConfigs = goodsConfigMapper.selectByAccountId(accountId);
        Set<String> autoDeliveryGoodsIds = new HashSet<>();
        if (goodsConfigs != null) {
            for (XianyuGoodsConfig config : goodsConfigs) {
                if (config.getXianyuAutoDeliveryOn() != null && config.getXianyuAutoDeliveryOn() == 1) {
                    autoDeliveryGoodsIds.add(config.getXyGoodsId());
                }
            }
        }
        if (autoDeliveryGoodsIds.isEmpty()) return;

        List<Map<String, Object>> toDeliver = new ArrayList<>();
        for (Map<String, Object> order : pendingOrders) {
            try {
                Object commonDataObj = order.get("commonData");
                if (!(commonDataObj instanceof Map)) continue;
                Map<String, Object> commonData = (Map<String, Object>) commonDataObj;

                String orderId = (String) commonData.get("orderId");
                String orderStatus = (String) commonData.get("orderStatus");
                String itemId = (String) commonData.get("itemId");

                if (orderId == null || !"待发货".equals(orderStatus)) continue;
                if (itemId == null || !autoDeliveryGoodsIds.contains(itemId)) continue;

                XianyuGoodsOrder existing = orderMapper.selectByAccountIdAndOrderId(accountId, orderId);
                if (existing != null && existing.getState() == 1) continue;

                toDeliver.add(order);
            } catch (Exception e) {
                log.warn("【账号{}】筛选待发货订单异常: {}", accountId, e.getMessage());
            }
        }

        if (toDeliver.isEmpty()) return;

        log.info("【账号{}】API发货: 发现{}笔待发货订单(WS未连接)", accountId, toDeliver.size());

        int randomDelay = 20 + ThreadLocalRandom.current().nextInt(11);
        log.info("【账号{}】随机延迟{}秒后开始发货", accountId, randomDelay);
        try { Thread.sleep(randomDelay * 1000L); } catch (InterruptedException e) { Thread.currentThread().interrupt(); return; }

        for (int i = 0; i < toDeliver.size(); i++) {
            Map<String, Object> order = toDeliver.get(i);
            try {
                Map<String, Object> commonData = (Map<String, Object>) order.get("commonData");
                String orderId = (String) commonData.get("orderId");
                String itemId = (String) commonData.get("itemId");

                log.info("【账号{}】API发货[{}/{}]: orderId={}, itemId={}", accountId, i + 1, toDeliver.size(), orderId, itemId);
                String result = orderService.consignDummyDeliveryWithConfig(accountId, itemId, orderId);

                if (result == null) {
                    log.error("【账号{}】API发货失败: orderId={}", accountId, orderId);
                }

                if (i < toDeliver.size() - 1) {
                    int delay = 5 + ThreadLocalRandom.current().nextInt(6);
                    log.info("【账号{}】发货间隔{}秒", accountId, delay);
                    try { Thread.sleep(delay * 1000L); } catch (InterruptedException e) { Thread.currentThread().interrupt(); return; }
                }
            } catch (Exception e) {
                log.warn("【账号{}】API发货异常: {}", accountId, e.getMessage());
            }
        }
    }
}