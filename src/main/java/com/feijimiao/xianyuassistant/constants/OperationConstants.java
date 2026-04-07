package com.feijimiao.xianyuassistant.constants;

/**
 * 操作常量定义
 * 用于操作日志记录
 */
public class OperationConstants {
    
    /**
     * 操作类型
     */
    public static class Type {
        /** 连接操作 */
        public static final String CONNECT = "CONNECT";
        /** 断开操作 */
        public static final String DISCONNECT = "DISCONNECT";
        /** 重连操作 */
        public static final String RECONNECT = "RECONNECT";
        /** 登录操作 */
        public static final String LOGIN = "LOGIN";
        /** 添加操作 */
        public static final String ADD = "ADD";
        /** 更新操作 */
        public static final String UPDATE = "UPDATE";
        /** 删除操作 */
        public static final String DELETE = "DELETE";
        /** 刷新操作 */
        public static final String REFRESH = "REFRESH";
        /** 发送操作 */
        public static final String SEND = "SEND";
        /** 接收操作 */
        public static final String RECEIVE = "RECEIVE";
        /** 同步操作 */
        public static final String SYNC = "SYNC";
        /** 验证操作 */
        public static final String VERIFY = "VERIFY";
    }
    
    /**
     * 操作模块
     */
    public static class Module {
        /** WebSocket模块 */
        public static final String WEBSOCKET = "WEBSOCKET";
        /** 账号模块 */
        public static final String ACCOUNT = "ACCOUNT";
        /** Cookie模块 */
        public static final String COOKIE = "COOKIE";
        /** Token模块 */
        public static final String TOKEN = "TOKEN";
        /** 消息模块 */
        public static final String MESSAGE = "MESSAGE";
        /** 订单模块 */
        public static final String ORDER = "ORDER";
        /** 商品模块 */
        public static final String GOODS = "GOODS";
        /** 自动发货模块 */
        public static final String AUTO_DELIVERY = "AUTO_DELIVERY";
        /** 二维码登录模块 */
        public static final String QR_LOGIN = "QR_LOGIN";
        /** 心跳模块 */
        public static final String HEARTBEAT = "HEARTBEAT";
    }
    
    /**
     * 操作状态
     */
    public static class Status {
        /** 成功 */
        public static final int SUCCESS = 1;
        /** 失败 */
        public static final int FAIL = 0;
        /** 部分成功 */
        public static final int PARTIAL = 2;
    }
    
    /**
     * 目标类型
     */
    public static class TargetType {
        /** 账号 */
        public static final String ACCOUNT = "ACCOUNT";
        /** 消息 */
        public static final String MESSAGE = "MESSAGE";
        /** 订单 */
        public static final String ORDER = "ORDER";
        /** 商品 */
        public static final String GOODS = "GOODS";
        /** Cookie */
        public static final String COOKIE = "COOKIE";
        /** Token */
        public static final String TOKEN = "TOKEN";
        /** WebSocket连接 */
        public static final String WEBSOCKET = "WEBSOCKET";
    }
}
