package com.feijimiao.xianyuassistant.service.impl;

import com.feijimiao.xianyuassistant.config.WebSocketConfig;
import com.feijimiao.xianyuassistant.constants.OperationConstants;
import com.feijimiao.xianyuassistant.service.AccountService;
import com.feijimiao.xianyuassistant.service.OperationLogService;
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
import java.util.concurrent.atomic.AtomicInteger;

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
    
    @Autowired
    private OperationLogService operationLogService;
    
    @Autowired
    private com.feijimiao.xianyuassistant.service.CookieRefreshService cookieRefreshService;

    @Autowired(required = false)
    private com.feijimiao.xianyuassistant.service.EmailNotifyService emailNotifyService;

    @Autowired
    private com.feijimiao.xianyuassistant.mapper.XianyuAccountMapper xianyuAccountMapper;

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
    
    // 心跳发送时间记录（参考Python的last_heartbeat_time）
    private final Map<Long, Long> lastHeartbeatSendTimes = new ConcurrentHashMap<>();
    
    // Token刷新时间记录
    private final Map<Long, Long> lastTokenRefreshTimes = new ConcurrentHashMap<>();
    
    // 连接重启标志
    private final Map<Long, Boolean> connectionRestartFlags = new ConcurrentHashMap<>();
    
    // 重连任务（防止重复重连）
    private final Map<Long, Future<?>> reconnectTasks = new ConcurrentHashMap<>();
    
    // 重连执行器（参考Python的异步重连）
    private final ScheduledExecutorService reconnectExecutor = Executors.newScheduledThreadPool(2);
    
    // 重连次数记录（参考Python的无限重连但有退避）
    private final Map<Long, AtomicInteger> reconnectAttemptCounts = new ConcurrentHashMap<>();

    // 邮件通知防抖记录（避免频繁发送）
    private final Map<Long, Long> lastDisconnectNotifyTimes = new ConcurrentHashMap<>();

    // 重连失败达到此次数后触发邮件通知
    private static final int RECONNECT_NOTIFY_THRESHOLD = 3;

    // 邮件通知最小间隔（10分钟）
    private static final long NOTIFY_INTERVAL_MS = 10 * 60 * 1000;

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
            String accessToken = tokenService.getAccessToken(accountId);
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
            // 参考Python: Token失效时设置connection_restart_flag=True，关闭WebSocket触发重连
            client.setOnTokenExpired(() -> {
                log.warn("【账号{}】Token失效(401)，触发自动重连流程...", accountId);
                try {
                    // 参考Python: 设置连接重启标志
                    connectionRestartFlags.put(accountId, true);
                    
                    // 停止当前连接
                    stopWebSocket(accountId);
                    
                    // 清除旧Token
                    tokenService.clearToken(accountId);
                    
                    // 重新启动连接（会自动刷新Token）
                    log.info("【账号{}】重新启动WebSocket连接（自动刷新Token）", accountId);
                    boolean success = startWebSocket(accountId);
                    
                    if (success) {
                        log.info("【账号{}】✅ Token失效后自动重连成功", accountId);
                    } else {
                        log.error("【账号{}】❌ Token失效后自动重连失败，将通过重连机制继续尝试", accountId);
                        // 参考Python: 失败后重连机制会继续尝试
                    }
                } catch (Exception e) {
                    log.error("【账号{}】Token失效自动重连异常", accountId, e);
                    // 参考Python: 异常后外层while True会继续重试
                    scheduleReconnect(accountId, config.getReconnectDelay(), false);
                }
            });
            
            // 设置心跳响应回调（更新心跳响应时间）
            client.setOnHeartbeatResponse(() -> {
                updateHeartbeatResponseTime(accountId);
            });
            
            // 设置连接关闭回调（参考Python的finally块中重连逻辑）
            client.setOnConnectionClosed(() -> {
                log.warn("【账号{}】WebSocket连接被关闭，触发自动重连...", accountId);
                // 参考Python: 如果是主动重启，立即重连；否则等待5秒
                Boolean restartFlag = connectionRestartFlags.get(accountId);
                boolean isManualRestart = restartFlag != null && restartFlag;
                
                int delay = isManualRestart ? 0 : config.getReconnectDelay();
                scheduleReconnect(accountId, delay, isManualRestart);
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
                
                // 记录操作日志
                operationLogService.log(accountId, 
                    OperationConstants.Type.CONNECT, 
                    OperationConstants.Module.WEBSOCKET,
                    "WebSocket连接成功", 
                    OperationConstants.Status.SUCCESS,
                    OperationConstants.TargetType.WEBSOCKET, 
                    String.valueOf(accountId),
                    null, null, null, null);
                
                return true;
            } else {
                log.error("WebSocket连接失败: accountId={}", accountId);
                log.error("连接状态: isOpen={}, isClosed={}", 
                        client.isOpen(), client.isClosed());
                
                // 记录操作日志
                operationLogService.log(accountId, 
                    OperationConstants.Type.CONNECT, 
                    OperationConstants.Module.WEBSOCKET,
                    "WebSocket连接失败", 
                    OperationConstants.Status.FAIL,
                    OperationConstants.TargetType.WEBSOCKET, 
                    String.valueOf(accountId),
                    null, null, null, null);
                
                return false;
            }
        } catch (Exception e) {
            log.error("连接WebSocket异常: accountId={}", accountId, e);
            
            // 记录操作日志
            operationLogService.log(accountId, 
                OperationConstants.Type.CONNECT, 
                OperationConstants.Module.WEBSOCKET,
                "WebSocket连接异常: " + e.getMessage(), 
                OperationConstants.Status.FAIL,
                OperationConstants.TargetType.WEBSOCKET, 
                String.valueOf(accountId),
                null, null, null, null);
            
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
                // 标记为主动关闭，防止onClose回调触发自动重连
                // 重连由调用方自行决定是否需要
                client.setIntentionalClose(true);
                client.close();
                log.info("WebSocket连接已关闭: accountId={}", accountId);
                
                // 记录操作日志
                operationLogService.log(accountId, 
                    OperationConstants.Type.DISCONNECT, 
                    OperationConstants.Module.WEBSOCKET,
                    "WebSocket连接已关闭", 
                    OperationConstants.Status.SUCCESS,
                    OperationConstants.TargetType.WEBSOCKET, 
                    String.valueOf(accountId),
                    null, null, null, null);
                
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
     * 增强功能：心跳超时检测（完全对齐Python逻辑）
     * 
     * Python心跳逻辑：
     * 1. 每隔heartbeat_interval秒发送一次心跳
     * 2. 检查上次心跳响应时间，如果超过(heartbeat_interval + heartbeat_timeout)则认为连接断开
     * 3. 心跳循环break后，外层while True循环会自动重连
     */
    private void startHeartbeat(Long accountId, XianyuWebSocketClient client) {
        // 初始化心跳响应时间（秒级时间戳，对齐Python）
        long currentTime = System.currentTimeMillis() / 1000;
        lastHeartbeatResponseTimes.put(accountId, currentTime);
        lastHeartbeatSendTimes.put(accountId, currentTime);
        
        // 心跳发送任务（参考Python的heartbeat_loop）
        // 立即发送第一次心跳,防止连接空闲被关闭
        try {
            if (client.isConnected()) {
                client.sendHeartbeat();
                log.info("【账号{}】已发送初始心跳", accountId);
            }
        } catch (Exception e) {
            log.error("发送初始心跳失败: accountId={}", accountId, e);
        }
        
        // 参考Python: heartbeat_loop 中每1秒检查一次
        ScheduledFuture<?> heartbeatTask = heartbeatScheduler.scheduleAtFixedRate(
            () -> {
                try {
                    long now = System.currentTimeMillis() / 1000;
                    
                    // 参考Python: 每隔heartbeat_interval秒发送一次心跳
                    Long lastSendTime = lastHeartbeatSendTimes.get(accountId);
                    if (lastSendTime == null || now - lastSendTime >= config.getHeartbeatInterval()) {
                        if (client.isConnected()) {
                            client.sendHeartbeat();
                            lastHeartbeatSendTimes.put(accountId, now);
                            log.debug("【账号{}】心跳已发送", accountId);
                        }
                    }
                    
                    // 参考Python: 检查上次心跳响应时间
                    // Python: if (current_time - self.last_heartbeat_response) > (self.heartbeat_interval + self.heartbeat_timeout):
                    Long lastResponseTime = lastHeartbeatResponseTimes.get(accountId);
                    if (lastResponseTime != null) {
                        long timeout = config.getHeartbeatInterval() + config.getHeartbeatTimeout();
                        
                        if (now - lastResponseTime > timeout) {
                            log.warn("【账号{}】心跳响应超时（{}秒无响应，超时阈值{}秒），连接可能已断开",
                                    accountId, now - lastResponseTime, timeout);
                            // 参考Python: heartbeat_loop break，触发外层重连
                            handleConnectionLost(accountId);
                        }
                    }
                } catch (Exception e) {
                    log.error("【账号{}】心跳任务异常", accountId, e);
                }
            },
            1, 1, TimeUnit.SECONDS  // 参考Python: 每秒检查一次
        );
        
        heartbeatTasks.put(accountId, heartbeatTask);
        log.info("心跳任务已启动: accountId={}, 心跳间隔{}秒, 超时阈值{}+{}秒", 
                accountId, config.getHeartbeatInterval(), config.getHeartbeatInterval(), config.getHeartbeatTimeout());
        
        // 启动Token自动刷新任务（参考Python的token_refresh_loop）
        startTokenRefresh(accountId);
    }
    
    /**
     * 启动Token自动刷新任务
     * 参考Python的token_refresh_loop方法
     * 
     * Python逻辑：
     * 1. 每分钟检查一次
     * 2. 当 current_time - last_token_refresh_time >= token_refresh_interval 时刷新Token
     * 3. Token刷新成功后，设置connection_restart_flag=True，关闭WebSocket触发重连
     * 4. Token刷新失败后，在token_retry_interval秒后重试
     */
    private void startTokenRefresh(Long accountId) {
        // 初始化Token刷新时间
        long currentTime = System.currentTimeMillis() / 1000;
        lastTokenRefreshTimes.put(accountId, currentTime);
        
        // Token刷新任务（每分钟检查一次，参考Python）
        ScheduledFuture<?> tokenRefreshTask = tokenRefreshScheduler.scheduleAtFixedRate(
            () -> {
                try {
                    Long lastRefreshTime = lastTokenRefreshTimes.get(accountId);
                    if (lastRefreshTime == null) {
                        return;
                    }
                    
                    long now = System.currentTimeMillis() / 1000;
                    
                    // 参考Python: 检查是否需要刷新Token
                    if (now - lastRefreshTime >= config.getTokenRefreshInterval()) {
                        log.info("【账号{}】Token即将过期（已{}小时），准备刷新并重连...", 
                                accountId, (now - lastRefreshTime) / 3600);
                        
                        // 参考Python: 设置连接重启标志
                        connectionRestartFlags.put(accountId, true);
                        
                        // 参考Python: 刷新Token并重连（成功后关闭旧连接）
                        refreshTokenAndReconnect(accountId);
                    }
                } catch (Exception e) {
                    log.error("【账号{}】Token刷新检查失败", accountId, e);
                }
            },
            60, 60, TimeUnit.SECONDS  // 参考Python: 每分钟检查一次
        );
        
        tokenRefreshTasks.put(accountId, tokenRefreshTask);
        log.info("Token刷新任务已启动: accountId={}, 刷新间隔{}秒(1小时)", accountId, config.getTokenRefreshInterval());
    }
    
    /**
     * 刷新Token并重连
     * 参考Python的refresh_token和重连逻辑
     * 
     * Python逻辑：
     * 1. 刷新Token
     * 2. 设置connection_restart_flag = True
     * 3. 关闭当前WebSocket连接（触发重连）
     * 4. Token刷新失败时，在token_retry_interval后重试
     */
    private void refreshTokenAndReconnect(Long accountId) {
        try {
            log.info("【账号{}】开始刷新Token并重连...", accountId);
            
            // 停止当前连接
            stopWebSocket(accountId);
            
            // 清除旧Token
            tokenService.clearToken(accountId);
            
            // 重新启动连接（会自动获取新Token）
            boolean success = startWebSocket(accountId);
            
            if (success) {
                // 更新Token刷新时间
                lastTokenRefreshTimes.put(accountId, System.currentTimeMillis() / 1000);
                // 重置重连计数
                AtomicInteger attemptCount = reconnectAttemptCounts.get(accountId);
                if (attemptCount != null) {
                    attemptCount.set(0);
                }
                log.info("【账号{}】✅ Token刷新并重连成功", accountId);
            } else {
                log.error("【账号{}】❌ Token刷新并重连失败，将在{}秒后重试", 
                        accountId, config.getTokenRetryInterval());
                
                // 参考Python: Token刷新失败后，在token_retry_interval后重试
                reconnectExecutor.schedule(() -> {
                    log.info("【账号{}】Token刷新重试间隔已到，开始重试...", accountId);
                    refreshTokenAndReconnect(accountId);
                }, config.getTokenRetryInterval(), TimeUnit.SECONDS);
            }
        } catch (Exception e) {
            log.error("【账号{}】Token刷新并重连异常，将在{}秒后重试", 
                    accountId, config.getTokenRetryInterval(), e);
            
            // 参考Python: 异常后也要重试
            reconnectExecutor.schedule(() -> {
                log.info("【账号{}】Token刷新重试间隔已到，开始重试...", accountId);
                refreshTokenAndReconnect(accountId);
            }, config.getTokenRetryInterval(), TimeUnit.SECONDS);
        }
    }
    
    /**
     * 处理连接丢失
     * 参考Python的连接重连逻辑
     */
    private void handleConnectionLost(Long accountId) {
        log.warn("【账号{}】检测到连接丢失（心跳超时），准备重连...", accountId);
        scheduleReconnect(accountId, config.getReconnectDelay(), false);
    }
    
    /**
     * 调度重连任务
     * 参考Python的main()方法中while True无限重连循环
     * 
     * 关键改进（对齐Python逻辑）：
     * 1. 防止重复重连：同一账号同时只有一个重连任务
     * 2. 指数退避：重连失败后延迟逐渐增加
     * 3. 重连成功后重置计数
     * 
     * @param accountId 账号ID
     * @param delaySeconds 延迟秒数
     * @param isManualRestart 是否主动重启（Token刷新等）
     */
    private void scheduleReconnect(Long accountId, int delaySeconds, boolean isManualRestart) {
        // 取消已有的重连任务（防止重复）
        Future<?> existingTask = reconnectTasks.get(accountId);
        if (existingTask != null && !existingTask.isDone()) {
            log.debug("【账号{}】已有重连任务在执行，跳过", accountId);
            return;
        }
        
        // 重置重启标志
        if (isManualRestart) {
            connectionRestartFlags.put(accountId, false);
        }
        
        // 获取/初始化重连次数
        AtomicInteger attemptCount = reconnectAttemptCounts.computeIfAbsent(accountId, k -> new AtomicInteger(0));
        
        // 参考Python: 无限重连，但使用指数退避
        int currentAttempt = attemptCount.incrementAndGet();
        // 指数退避: 5s, 10s, 20s, 40s, 60s, 60s, ... 最大60秒
        int actualDelay = isManualRestart ? delaySeconds : 
                Math.min(delaySeconds * (int) Math.pow(2, Math.min(currentAttempt - 1, 4)), 60);
        
        log.info("【账号{}】计划{}秒后执行重连（第{}次尝试）...", accountId, actualDelay, currentAttempt);
        
        ScheduledFuture<?> reconnectTask = reconnectExecutor.schedule(() -> {
            try {
                reconnectTasks.remove(accountId);
                
                // 停止当前连接和心跳
                stopWebSocket(accountId);
                
                // 参考Python: 重连前先刷新Cookie（hasLogin保活）
                try {
                    if (cookieRefreshService != null) {
                        log.info("【账号{}】重连前先检查Cookie登录状态...", accountId);
                        boolean cookieOk = cookieRefreshService.checkLoginStatus(accountId);
                        if (!cookieOk) {
                            log.warn("【账号{}】Cookie已失效，重连可能失败，但仍尝试...", accountId);
                        }
                    }
                } catch (Exception e) {
                    log.warn("【账号{}】重连前Cookie检查异常，继续尝试重连: {}", accountId, e.getMessage());
                }
                
                // 重新启动连接
                boolean success = startWebSocket(accountId);
                
                if (success) {
                    // 重连成功，重置计数
                    attemptCount.set(0);
                    log.info("【账号{}】✅ 重连成功", accountId);
                    
                    operationLogService.log(accountId, 
                        OperationConstants.Type.RECONNECT, 
                        OperationConstants.Module.WEBSOCKET,
                        isManualRestart ? "主动重启连接成功" : "异常断开后重连成功", 
                        OperationConstants.Status.SUCCESS,
                        OperationConstants.TargetType.WEBSOCKET, 
                        String.valueOf(accountId),
                        null, null, null, null);
                } else {
                    log.error("【账号{}】❌ 重连失败（第{}次），将继续尝试...", accountId, currentAttempt);
                    
                    operationLogService.log(accountId, 
                        OperationConstants.Type.RECONNECT, 
                        OperationConstants.Module.WEBSOCKET,
                        "重连失败（第" + currentAttempt + "次）", 
                        OperationConstants.Status.FAIL,
                        OperationConstants.TargetType.WEBSOCKET, 
                        String.valueOf(accountId),
                        null, null, null, null);
                    
                    // 重连失败达到阈值时触发邮件通知
                    if (currentAttempt >= RECONNECT_NOTIFY_THRESHOLD) {
                        triggerWsDisconnectNotify(accountId);
                    }
                    
                    // 参考Python: 重连失败后继续尝试（while True循环）
                    scheduleReconnect(accountId, config.getReconnectDelay(), false);
                }
            } catch (Exception e) {
                log.error("【账号{}】重连异常，将继续尝试...", accountId, e);
                scheduleReconnect(accountId, config.getReconnectDelay(), false);
            }
        }, actualDelay, TimeUnit.SECONDS);
        
        reconnectTasks.put(accountId, reconnectTask);
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
        
        // 取消重连任务
        Future<?> reconnectTask = reconnectTasks.remove(accountId);
        if (reconnectTask != null) {
            reconnectTask.cancel(false);
            log.info("重连任务已取消: accountId={}", accountId);
        }
        
        // 清理状态
        lastHeartbeatResponseTimes.remove(accountId);
        lastHeartbeatSendTimes.remove(accountId);
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
        
        // 关闭重连调度器
        reconnectExecutor.shutdown();
    }

    private void triggerWsDisconnectNotify(Long accountId) {
        try {
            if (emailNotifyService == null || !emailNotifyService.isWsDisconnectNotifyEnabled()) {
                return;
            }
            // 防抖：10分钟内只发送一次
            Long lastNotifyTime = lastDisconnectNotifyTimes.get(accountId);
            long now = System.currentTimeMillis();
            if (lastNotifyTime != null && (now - lastNotifyTime) < NOTIFY_INTERVAL_MS) {
                log.debug("【账号{}】邮件通知防抖中，跳过本次发送", accountId);
                return;
            }
            lastDisconnectNotifyTimes.put(accountId, now);

            String accountNote = "";
            try {
                com.feijimiao.xianyuassistant.entity.XianyuAccount account = xianyuAccountMapper.selectById(accountId);
                if (account != null) {
                    accountNote = account.getAccountNote() != null ? account.getAccountNote() : "";
                }
            } catch (Exception e) {
                log.debug("获取账号备注失败: {}", e.getMessage());
            }
            emailNotifyService.sendWsDisconnectNotifyEmail(accountId, accountNote);
        } catch (Exception e) {
            log.warn("触发WebSocket断开连接邮件通知异常: {}", e.getMessage());
        }
    }
}
