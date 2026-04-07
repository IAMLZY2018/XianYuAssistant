package com.feijimiao.xianyuassistant.service.impl;

import com.feijimiao.xianyuassistant.config.WebSocketConfig;
import com.feijimiao.xianyuassistant.service.AccountService;
import com.feijimiao.xianyuassistant.service.WebSocketService;
import com.feijimiao.xianyuassistant.service.WebSocketTokenService;
import com.feijimiao.xianyuassistant.utils.XianyuSignUtils;
import com.feijimiao.xianyuassistant.websocket.WebSocketInitializer;
import com.feijimiao.xianyuassistant.websocket.WebSocketMessageHandler;
import com.feijimiao.xianyuassistant.websocket.XianyuWebSocketClient;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.*;

/**
 * WebSocket服务实现类
 * 参考Python代码的XianyuAutoAsync类
 * 增强功能：
 * 1. Token自动刷新机制
 * 2. 心跳超时检测
 * 3. 连接重连机制
 */
@Slf4j
@Service
public class WebSocketServiceImpl implements WebSocketService {

    @Autowired
    private AccountService accountService;
    
    @Autowired
    private WebSocketMessageHandler messageHandler;
    
    @Autowired
    private WebSocketTokenService tokenService;
    
    @Autowired
    private WebSocketInitializer initializer;
    
    @Autowired
    private WebSocketConfig config;
    
    @Autowired
    private com.feijimiao.xianyuassistant.utils.AccountDisplayNameUtils displayNameUtils;

    // 存储WebSocket客户端
    private final Map<Long, XianyuWebSocketClient> webSocketClients = new ConcurrentHashMap<>();
    
    // 心跳定时器
    private final ScheduledExecutorService heartbeatScheduler = Executors.newScheduledThreadPool(2);
    
    // 心跳任务
    private final Map<Long, ScheduledFuture<?>> heartbeatTasks = new ConcurrentHashMap<>();
    
    // Token刷新定时器
    private final ScheduledExecutorService tokenRefreshScheduler = Executors.newScheduledThreadPool(1);
    
    // Token刷新任务
    private final Map<Long, ScheduledFuture<?>> tokenRefreshTasks = new ConcurrentHashMap<>();
    
    // 心跳响应时间记录
    private final Map<Long, Long> lastHeartbeatResponseTimes = new ConcurrentHashMap<>();
    
    // Token刷新时间记录
    private final Map<Long, Long> lastTokenRefreshTimes = new ConcurrentHashMap<>();
    
    // 连接重启标志
    private final Map<Long, Boolean> connectionRestartFlags = new ConcurrentHashMap<>();

    /**
     * 闲鱼WebSocket URL
     * 参考Python代码：wss://wss-goofish.dingtalk.com/
     */
    private static final String WEBSOCKET_URL = "wss://wss-goofish.dingtalk.com/";

    @Override
    public boolean startWebSocket(Long accountId) {
        try {
            log.info("启动WebSocket连接: accountId={}", accountId);

            // 检查是否已经连接
            if (webSocketClients.containsKey(accountId)) {
                XianyuWebSocketClient existingClient = webSocketClients.get(accountId);
                if (existingClient.isConnected()) {
                    log.info("WebSocket已连接: accountId={}", accountId);
                    return true;
                } else {
                    // 关闭旧连接
                    stopWebSocket(accountId);
                }
            }

            // 获取Cookie
            String cookieStr = accountService.getCookieByAccountId(accountId);
            if (cookieStr == null || cookieStr.isEmpty()) {
                log.error("未找到账号Cookie: accountId={}", accountId);
                throw new com.feijimiao.xianyuassistant.exception.CookieNotFoundException("未找到账号Cookie，请先配置Cookie");
            }

            // 解析Cookie
            Map<String, String> cookies = XianyuSignUtils.parseCookies(cookieStr);
            
            // 生成设备ID（参考Python的generate_device_id）
            String unb = cookies.get("unb");
            if (unb == null || unb.isEmpty()) {
                log.error("Cookie中缺少unb字段: accountId={}", accountId);
                throw new com.feijimiao.xianyuassistant.exception.CookieExpiredException("Cookie中缺少unb字段，Cookie可能已过期或无效");
            }
            // 使用持久化的设备ID（如果数据库中已有则使用，否则生成新的并保存）
            String deviceId = accountService.getOrGenerateDeviceId(accountId, unb);
            if (deviceId == null || deviceId.isEmpty()) {
                log.error("获取或生成设备ID失败: accountId={}", accountId);
                throw new RuntimeException("无法获取或生成设备ID");
            }
            log.info("使用设备ID: accountId={}, deviceId={}", accountId, deviceId);
            
            // 获取accessToken（参考Python的refresh_token）
            log.info("正在获取accessToken: accountId={}", accountId);
            String accessToken = tokenService.getAccessToken(accountId, cookieStr, deviceId);
            if (accessToken == null || accessToken.isEmpty()) {
                log.error("获取accessToken失败: accountId={}", accountId);
                log.error("无法继续WebSocket连接，请检查Cookie是否有效");
                throw new com.feijimiao.xianyuassistant.exception.TokenInvalidException("无法获取WebSocket Token，请检查Cookie是否有效");
            }
            log.info("accessToken获取成功: accountId={}, token长度={}", accountId, accessToken.length());
            
            // 调用通用连接方法
            return connectWebSocket(accountId, cookieStr, deviceId, accessToken, unb);

        } catch (com.feijimiao.xianyuassistant.exception.CaptchaRequiredException e) {
            log.warn("启动WebSocket需要滑块验证: accountId={}, url={}", accountId, e.getCaptchaUrl());
            throw e; // 重新抛出，让Controller处理
        } catch (Exception e) {
            log.error("启动WebSocket失败: accountId={}", accountId, e);
            return false;
        }
    }

    @Override
    public boolean startWebSocketWithToken(Long accountId, String accessToken) {
        try {
            log.info("========== 使用手动Token启动WebSocket连接 ==========");
            log.info("【账号{}】accountId={}", accountId, accountId);
            log.info("【账号{}】accessToken长度={}", accountId, accessToken != null ? accessToken.length() : 0);
            log.info("【账号{}】accessToken前50字符={}", accountId, 
                    accessToken != null && accessToken.length() > 50 ? accessToken.substring(0, 50) + "..." : accessToken);

            // 检查是否已经连接
            if (webSocketClients.containsKey(accountId)) {
                XianyuWebSocketClient existingClient = webSocketClients.get(accountId);
                if (existingClient.isConnected()) {
                    log.info("【账号{}】WebSocket已连接", accountId);
                    return true;
                } else {
                    // 关闭旧连接
                    log.info("【账号{}】关闭旧连接", accountId);
                    stopWebSocket(accountId);
                }
            }

            // 获取Cookie
            String cookieStr = accountService.getCookieByAccountId(accountId);
            if (cookieStr == null || cookieStr.isEmpty()) {
                log.error("【账号{}】未找到账号Cookie", accountId);
                throw new com.feijimiao.xianyuassistant.exception.CookieNotFoundException("未找到账号Cookie，请先配置Cookie");
            }
            log.info("【账号{}】Cookie长度={}", accountId, cookieStr.length());

            // 解析Cookie
            Map<String, String> cookies = XianyuSignUtils.parseCookies(cookieStr);
            log.info("【账号{}】解析到{}个Cookie字段", accountId, cookies.size());
            
            // 生成设备ID
            String unb = cookies.get("unb");
            if (unb == null || unb.isEmpty()) {
                log.error("【账号{}】Cookie中缺少unb字段", accountId);
                throw new com.feijimiao.xianyuassistant.exception.CookieExpiredException("Cookie中缺少unb字段，Cookie可能已过期或无效");
            }
            // 使用持久化的设备ID
            String deviceId = accountService.getOrGenerateDeviceId(accountId, unb);
            if (deviceId == null || deviceId.isEmpty()) {
                log.error("【账号{}】获取或生成设备ID失败", accountId);
                throw new RuntimeException("无法获取或生成设备ID");
            }
            log.info("【账号{}】设备ID={}", accountId, deviceId);
            
            log.info("【账号{}】准备调用通用连接方法（Token将在注册成功后保存）...", accountId);
            
            // 调用通用连接方法
            boolean result = connectWebSocket(accountId, cookieStr, deviceId, accessToken, unb);
            
            log.info("【账号{}】连接结果={}", accountId, result);
            log.info("========== 手动Token启动流程结束 ==========");
            
            return result;

        } catch (Exception e) {
            log.error("【账号{}】使用手动Token启动WebSocket失败", accountId, e);
            return false;
        }
    }

    /**
     * 通用WebSocket连接方法
     */
    private boolean connectWebSocket(Long accountId, String cookieStr, String deviceId, String accessToken, String unb) throws Exception {
        try {
            // 构建WebSocket请求头（参考Python的WEBSOCKET_HEADERS配置）
            Map<String, String> headers = new HashMap<>();
            headers.put("Cookie", cookieStr);
            headers.put("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/133.0.0.0 Safari/537.36");
            headers.put("Origin", "https://www.goofish.com");
            headers.put("Host", "wss-goofish.dingtalk.com");
            headers.put("Accept-Encoding", "gzip, deflate, br, zstd");
            headers.put("Accept-Language", "zh-CN,zh;q=0.9");
            headers.put("Cache-Control", "no-cache");
            headers.put("Pragma", "no-cache");
            headers.put("Connection", "Upgrade");
            headers.put("Upgrade", "websocket");

            // 创建WebSocket客户端（参考Python的_create_websocket_connection）
            URI serverUri = new URI(WEBSOCKET_URL);
            XianyuWebSocketClient client = new XianyuWebSocketClient(serverUri, headers, String.valueOf(accountId), displayNameUtils);
            
            // 设置当前用户ID（从Cookie的unb字段获取）
            client.setMyUserId(unb);
            
            // 设置消息处理器
            client.setMessageHandler(messageHandler);
            
            // 设置注册成功回调（保存Token）
            final String finalAccessToken = accessToken;
            client.setOnRegistrationSuccess(() -> {
                log.info("【账号{}】注册成功回调被触发，开始保存Token到数据库", accountId);
                tokenService.saveToken(accountId, finalAccessToken);
                log.info("【账号{}】✅ Token已成功保存到数据库", accountId);
            });
            
            // 设置Token失效回调（自动重连）
            client.setOnTokenExpired(() -> {
                log.warn("【账号{}】Token失效，开始自动重连流程...", accountId);
                try {
                    // 停止当前连接
                    stopWebSocket(accountId);
                    
                    // 等待1秒
                    Thread.sleep(1000);
                    
                    // 强制刷新Token（清除数据库中的无效token）
                    log.info("【账号{}】强制刷新Token（清除数据库缓存）", accountId);
                    tokenService.clearToken(accountId);
                    
                    // 重新启动连接（会自动刷新Token）
                    log.info("【账号{}】重新启动WebSocket连接（自动刷新Token）", accountId);
                    boolean success = startWebSocket(accountId);
                    
                    if (success) {
                        log.info("【账号{}】✅ 自动重连成功", accountId);
                    } else {
                        log.error("【账号{}】❌ 自动重连失败", accountId);
                    }
                } catch (Exception e) {
                    log.error("【账号{}】自动重连异常", accountId, e);
                }
            });
            
            // 设置心跳响应回调（更新心跳响应时间）
            client.setOnHeartbeatResponse(() -> {
                updateHeartbeatResponseTime(accountId);
            });

            // 连接WebSocket（参考Python的connect方法）
            log.info("正在连接WebSocket: {}", WEBSOCKET_URL);
            log.info("请求头: {}", headers);
            
            boolean connected = client.connectBlocking(10, TimeUnit.SECONDS);
            
            if (connected) {
                webSocketClients.put(accountId, client);
                
                // 执行WebSocket初始化流程（参考Python的init方法）
                log.info("开始WebSocket初始化流程: accountId={}", accountId);
                initializer.initialize(client, accessToken, deviceId, String.valueOf(accountId));
                
                // 启动心跳任务
                startHeartbeat(accountId, client);
                
                log.info("WebSocket连接成功: accountId={}", accountId);
                log.info("连接状态: isOpen={}, isClosed={}", 
                        client.isOpen(), client.isClosed());
                return true;
            } else {
                log.error("WebSocket连接失败: accountId={}", accountId);
                log.error("连接状态: isOpen={}, isClosed={}", 
                        client.isOpen(), client.isClosed());
                return false;
            }
        } catch (Exception e) {
            log.error("连接WebSocket异常: accountId={}", accountId, e);
            throw e;
        }
    }

    @Override
    public boolean stopWebSocket(Long accountId) {
        try {
            log.info("停止WebSocket连接: accountId={}", accountId);

            // 停止心跳任务
            stopHeartbeat(accountId);

            // 关闭WebSocket连接
            XianyuWebSocketClient client = webSocketClients.remove(accountId);
            if (client != null) {
                client.close();
                log.info("WebSocket连接已关闭: accountId={}", accountId);
                return true;
            } else {
                log.warn("WebSocket连接不存在: accountId={}", accountId);
                return false;
            }

        } catch (Exception e) {
            log.error("停止WebSocket失败: accountId={}", accountId, e);
            return false;
        }
    }

    @Override
    public boolean isConnected(Long accountId) {
        XianyuWebSocketClient client = webSocketClients.get(accountId);
        return client != null && client.isConnected();
    }

    @Override
    public void stopAllWebSockets() {
        log.info("停止所有WebSocket连接");
        
        for (Long accountId : webSocketClients.keySet()) {
            stopWebSocket(accountId);
        }
        
        // 关闭心跳调度器
        heartbeatScheduler.shutdown();
    }

    /**
     * 启动心跳任务
     * 增强功能：心跳超时检测
     */
    private void startHeartbeat(Long accountId, XianyuWebSocketClient client) {
        // 初始化心跳响应时间
        long currentTime = System.currentTimeMillis() / 1000;
        lastHeartbeatResponseTimes.put(accountId, currentTime);
        
        // 心跳发送任务（参考Python的heartbeat_loop）
        ScheduledFuture<?> heartbeatTask = heartbeatScheduler.scheduleAtFixedRate(
            () -> {
                try {
                    if (client.isConnected()) {
                        client.sendHeartbeat();
                        
                        // 检查心跳响应超时（参考Python的心跳超时检测）
                        Long lastResponseTime = lastHeartbeatResponseTimes.get(accountId);
                        if (lastResponseTime != null) {
                            long now = System.currentTimeMillis() / 1000;
                            long timeout = config.getHeartbeatInterval() + config.getHeartbeatTimeout();
                            
                            if (now - lastResponseTime > timeout) {
                                log.warn("【账号{}】心跳响应超时，可能连接已断开", accountId);
                                // 触发重连
                                handleConnectionLost(accountId);
                            }
                        }
                    } else {
                        log.debug("WebSocket连接断开: accountId={}", accountId);
                    }
                } catch (Exception e) {
                    log.error("发送心跳失败: accountId={}", accountId, e);
                }
            },
            config.getHeartbeatInterval(), config.getHeartbeatInterval(), TimeUnit.SECONDS
        );
        
        heartbeatTasks.put(accountId, heartbeatTask);
        log.info("心跳任务已启动: accountId={}, 间隔{}秒", accountId, config.getHeartbeatInterval());
        
        // 启动Token自动刷新任务（参考Python的token_refresh_loop）
        startTokenRefresh(accountId);
    }
    
    /**
     * 启动Token自动刷新任务
     * 参考Python的token_refresh_loop方法
     */
    private void startTokenRefresh(Long accountId) {
        // 初始化Token刷新时间
        long currentTime = System.currentTimeMillis() / 1000;
        lastTokenRefreshTimes.put(accountId, currentTime);
        
        // Token刷新任务（每分钟检查一次）
        ScheduledFuture<?> tokenRefreshTask = tokenRefreshScheduler.scheduleAtFixedRate(
            () -> {
                try {
                    Long lastRefreshTime = lastTokenRefreshTimes.get(accountId);
                    if (lastRefreshTime == null) {
                        return;
                    }
                    
                    long now = System.currentTimeMillis() / 1000;
                    
                    // 检查是否需要刷新Token（参考Python的token_refresh_interval）
                    if (now - lastRefreshTime >= config.getTokenRefreshInterval()) {
                        log.info("【账号{}】Token即将过期，准备刷新...", accountId);
                        
                        // 设置连接重启标志
                        connectionRestartFlags.put(accountId, true);
                        
                        // 刷新Token并重连
                        refreshTokenAndReconnect(accountId);
                    }
                } catch (Exception e) {
                    log.error("【账号{}】Token刷新检查失败", accountId, e);
                }
            },
            60, 60, TimeUnit.SECONDS  // 每分钟检查一次
        );
        
        tokenRefreshTasks.put(accountId, tokenRefreshTask);
        log.info("Token刷新任务已启动: accountId={}, 刷新间隔{}秒", accountId, config.getTokenRefreshInterval());
    }
    
    /**
     * 刷新Token并重连
     * 参考Python的refresh_token和重连逻辑
     */
    private void refreshTokenAndReconnect(Long accountId) {
        try {
            log.info("【账号{}】开始刷新Token并重连...", accountId);
            
            // 停止当前连接
            stopWebSocket(accountId);
            
            // 等待1秒
            Thread.sleep(1000);
            
            // 清除旧Token
            tokenService.clearToken(accountId);
            
            // 重新启动连接（会自动获取新Token）
            boolean success = startWebSocket(accountId);
            
            if (success) {
                // 更新Token刷新时间
                lastTokenRefreshTimes.put(accountId, System.currentTimeMillis() / 1000);
                log.info("【账号{}】✅ Token刷新并重连成功", accountId);
            } else {
                log.error("【账号{}】❌ Token刷新并重连失败，将在{}分钟后重试", 
                        accountId, config.getTokenRetryInterval() / 60);
            }
        } catch (Exception e) {
            log.error("【账号{}】Token刷新并重连异常", accountId, e);
        }
    }
    
    /**
     * 处理连接丢失
     * 参考Python的连接重连逻辑
     */
    private void handleConnectionLost(Long accountId) {
        try {
            log.warn("【账号{}】检测到连接丢失，准备重连...", accountId);
            
            // 检查是否是主动重启
            Boolean restartFlag = connectionRestartFlags.get(accountId);
            if (restartFlag != null && restartFlag) {
                log.info("【账号{}】主动重启连接，立即重连...", accountId);
            } else {
                log.info("【账号{}】等待{}秒后重连...", accountId, config.getReconnectDelay());
                Thread.sleep(config.getReconnectDelay() * 1000L);
            }
            
            // 重置重启标志
            connectionRestartFlags.put(accountId, false);
            
            // 停止当前连接
            stopWebSocket(accountId);
            
            // 重新启动连接
            boolean success = startWebSocket(accountId);
            
            if (success) {
                log.info("【账号{}】✅ 重连成功", accountId);
            } else {
                log.error("【账号{}】❌ 重连失败", accountId);
            }
        } catch (Exception e) {
            log.error("【账号{}】重连异常", accountId, e);
        }
    }
    
    /**
     * 更新心跳响应时间
     * 由消息处理器调用
     */
    public void updateHeartbeatResponseTime(Long accountId) {
        lastHeartbeatResponseTimes.put(accountId, System.currentTimeMillis() / 1000);
    }

    /**
     * 停止心跳任务
     */
    private void stopHeartbeat(Long accountId) {
        // 停止心跳任务
        ScheduledFuture<?> heartbeatTask = heartbeatTasks.remove(accountId);
        if (heartbeatTask != null) {
            heartbeatTask.cancel(false);
            log.info("心跳任务已停止: accountId={}", accountId);
        }
        
        // 停止Token刷新任务
        ScheduledFuture<?> tokenRefreshTask = tokenRefreshTasks.remove(accountId);
        if (tokenRefreshTask != null) {
            tokenRefreshTask.cancel(false);
            log.info("Token刷新任务已停止: accountId={}", accountId);
        }
        
        // 清理状态
        lastHeartbeatResponseTimes.remove(accountId);
        lastTokenRefreshTimes.remove(accountId);
        connectionRestartFlags.remove(accountId);
    }

    @Override
    public boolean sendMessage(Long accountId, String cid, String toId, String text) {
        try {
            log.info("发送消息: accountId={}, cid={}, toId={}, text={}", accountId, cid, toId, text);
            
            // 获取WebSocket客户端
            XianyuWebSocketClient client = webSocketClients.get(accountId);
            if (client == null) {
                log.error("WebSocket客户端不存在: accountId={}", accountId);
                return false;
            }
            
            if (!client.isConnected()) {
                log.error("WebSocket未连接: accountId={}", accountId);
                return false;
            }
            
            // 发送消息
            client.sendMessage(cid, toId, text);
            return true;
            
        } catch (Exception e) {
            log.error("发送消息失败: accountId={}, cid={}, toId={}", accountId, cid, toId, e);
            return false;
        }
    }

    /**
     * 应用关闭时清理资源
     */
    @PreDestroy
    public void cleanup() {
        log.info("应用关闭，清理WebSocket资源");
        stopAllWebSockets();
        
        // 关闭Token刷新调度器
        tokenRefreshScheduler.shutdown();
    }
}
