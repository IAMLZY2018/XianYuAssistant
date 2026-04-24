-- 系统用户表
CREATE TABLE IF NOT EXISTS sys_user (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    username VARCHAR(50) NOT NULL UNIQUE,              -- 用户名
    password VARCHAR(200) NOT NULL,                    -- 密码（BCrypt加密）
    status TINYINT DEFAULT 1,                          -- 状态 1:正常 0:禁用
    last_login_time DATETIME,                          -- 最后登录时间
    last_login_ip VARCHAR(50),                         -- 最后登录IP
    created_time DATETIME DEFAULT (datetime('now', 'localtime')),  -- 创建时间
    updated_time DATETIME DEFAULT (datetime('now', 'localtime'))   -- 更新时间
);

-- 登录Token表
CREATE TABLE IF NOT EXISTS sys_login_token (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    user_id BIGINT NOT NULL,                           -- 关联用户ID
    token VARCHAR(500) NOT NULL,                       -- JWT Token
    device_id VARCHAR(100),                            -- 设备标识（User-Agent哈希）
    login_ip VARCHAR(50),                              -- 登录IP
    expire_time DATETIME NOT NULL,                     -- 过期时间
    created_time DATETIME DEFAULT (datetime('now', 'localtime')),  -- 创建时间
    updated_time DATETIME DEFAULT (datetime('now', 'localtime')),  -- 更新时间
    FOREIGN KEY (user_id) REFERENCES sys_user(id)
);

-- 索引
CREATE INDEX IF NOT EXISTS idx_sys_user_username ON sys_user(username);
CREATE INDEX IF NOT EXISTS idx_sys_login_token_user_id ON sys_login_token(user_id);
CREATE INDEX IF NOT EXISTS idx_sys_login_token_token ON sys_login_token(token);
CREATE INDEX IF NOT EXISTS idx_sys_login_token_expire_time ON sys_login_token(expire_time);

-- 触发器
CREATE TRIGGER IF NOT EXISTS update_sys_user_time
AFTER UPDATE ON sys_user
BEGIN
    UPDATE sys_user SET updated_time = CURRENT_TIMESTAMP WHERE id = NEW.id;
END;
CREATE TRIGGER IF NOT EXISTS update_sys_login_token_time
AFTER UPDATE ON sys_login_token
BEGIN
    UPDATE sys_login_token SET updated_time = CURRENT_TIMESTAMP WHERE id = NEW.id;
END;
-- 闲鱼账号表
CREATE TABLE IF NOT EXISTS xianyu_account (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    account_note VARCHAR(100),                    -- 闲鱼账号备注
    unb VARCHAR(100),                             -- UNB标识
    device_id VARCHAR(100),                       -- 设备ID（UUID格式-用户ID，用于WebSocket连接）
    status TINYINT DEFAULT 1,                     -- 账号状态 1:正常 -1:需要手机号验证
    created_time DATETIME DEFAULT (datetime('now', 'localtime')),  -- 创建时间
    updated_time DATETIME DEFAULT (datetime('now', 'localtime'))   -- 更新时间
);

-- 闲鱼Cookie表
CREATE TABLE IF NOT EXISTS xianyu_cookie (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    xianyu_account_id BIGINT NOT NULL,            -- 关联的闲鱼账号ID
    cookie_text TEXT,                             -- 完整的Cookie字符串
    m_h5_tk VARCHAR(500),                         -- _m_h5_tk token（用于API签名）
    cookie_status TINYINT DEFAULT 1,              -- Cookie状态 1:有效 2:过期 3:失效
    expire_time DATETIME,                         -- 过期时间
    websocket_token TEXT,                         -- WebSocket accessToken
    token_expire_time INTEGER,                    -- Token过期时间戳（毫秒）
    created_time DATETIME DEFAULT (datetime('now', 'localtime')),  -- 创建时间
    updated_time DATETIME DEFAULT (datetime('now', 'localtime')),  -- 更新时间
    FOREIGN KEY (xianyu_account_id) REFERENCES xianyu_account(id)
);

-- 创建索引
CREATE INDEX IF NOT EXISTS idx_account_unb ON xianyu_account(unb);
CREATE INDEX IF NOT EXISTS idx_cookie_account_id ON xianyu_cookie(xianyu_account_id);
CREATE INDEX IF NOT EXISTS idx_cookie_status ON xianyu_cookie(cookie_status);
CREATE INDEX IF NOT EXISTS idx_token_expire_time ON xianyu_cookie(token_expire_time);

-- 创建更新时间触发器（SQLite不支持ON UPDATE CURRENT_TIMESTAMP，需要用触发器）
-- 注意：触发器使用特殊分隔符 $$
CREATE TRIGGER IF NOT EXISTS update_xianyu_account_time 
AFTER UPDATE ON xianyu_account
BEGIN
    UPDATE xianyu_account SET updated_time = CURRENT_TIMESTAMP WHERE id = NEW.id;
END;
CREATE TRIGGER IF NOT EXISTS update_xianyu_cookie_time 
AFTER UPDATE ON xianyu_cookie
BEGIN
    UPDATE xianyu_cookie SET updated_time = CURRENT_TIMESTAMP WHERE id = NEW.id;
END;
-- 闲鱼商品信息表
CREATE TABLE IF NOT EXISTS xianyu_goods (
    id BIGINT PRIMARY KEY,                        -- 表ID（使用雪花ID）
    xy_good_id VARCHAR(100) NOT NULL,             -- 闲鱼商品ID
    xianyu_account_id BIGINT,                     -- 关联的闲鱼账号ID
    title VARCHAR(500),                           -- 商品标题
    cover_pic TEXT,                               -- 封面图片URL
    info_pic TEXT,                                -- 商品详情图片（JSON数组）
    detail_info TEXT,                             -- 商品详情信息（预留字段）
    detail_url TEXT,                              -- 商品详情页URL
    sold_price VARCHAR(50),                       -- 商品价格
    status TINYINT DEFAULT 0,                     -- 商品状态 0:在售 1:已下架 2:已售出
    created_time DATETIME DEFAULT (datetime('now', 'localtime')),  -- 创建时间
    updated_time DATETIME DEFAULT (datetime('now', 'localtime')),  -- 更新时间
    FOREIGN KEY (xianyu_account_id) REFERENCES xianyu_account(id)
);

-- 创建商品表索引
CREATE UNIQUE INDEX IF NOT EXISTS idx_goods_xy_good_id ON xianyu_goods(xy_good_id);
CREATE INDEX IF NOT EXISTS idx_goods_status ON xianyu_goods(status);
CREATE INDEX IF NOT EXISTS idx_goods_account_id ON xianyu_goods(xianyu_account_id);

-- 创建商品表更新时间触发器
CREATE TRIGGER IF NOT EXISTS update_xianyu_goods_time
AFTER UPDATE ON xianyu_goods
BEGIN
    UPDATE xianyu_goods SET updated_time = CURRENT_TIMESTAMP WHERE id = NEW.id;
END;
-- 闲鱼聊天消息表
CREATE TABLE IF NOT EXISTS xianyu_chat_message (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    
    -- 关联信息
    xianyu_account_id BIGINT NOT NULL,            -- 关联的闲鱼账号ID
    
    -- WebSocket消息字段
    lwp VARCHAR(50),                              -- websocket消息类型，比如："/s/para"
    pnm_id VARCHAR(100) NOT NULL,                 -- 对应的消息pnmid，比如："3813496236127.PNM"（字段1.3）
    s_id VARCHAR(100),                            -- 消息聊天框id，比如："55435931514@goofish"（字段1.2）
    
    -- 消息内容
    content_type INTEGER,                         -- 消息类别，contentType=1用户消息，32系统消息（字段1.6.3.5中的contentType）
    msg_content TEXT,                             -- 消息内容，对应1.10.reminderContent
    
    -- 发送者信息
    sender_user_name VARCHAR(200),                -- 发送者用户名称，对应1.10.reminderTitle
    sender_user_id VARCHAR(100),                  -- 发送者用户id，对应1.10.senderUserId
    sender_app_v VARCHAR(50),                     -- 发送者app版本，对应1.10._appVersion
    sender_os_type VARCHAR(20),                   -- 发送者系统版本，对应1.10._platform
    
    -- 消息链接
    reminder_url TEXT,                            -- 消息链接，对应1.10.reminderUrl
    xy_goods_id VARCHAR(100),                     -- 闲鱼商品ID，从reminder_url中的itemId参数解析
    
    -- 完整消息体
    complete_msg TEXT NOT NULL,                   -- 完整的消息体JSON
    
    -- 时间信息
    message_time BIGINT,                          -- 消息时间戳（毫秒，字段1.5）
    create_time DATETIME DEFAULT (datetime('now', 'localtime')),  -- 创建时间
    
    -- 外键约束
    FOREIGN KEY (xianyu_account_id) REFERENCES xianyu_account(id)
);

-- 创建聊天消息表索引
CREATE INDEX IF NOT EXISTS idx_chat_message_account_id ON xianyu_chat_message(xianyu_account_id);
CREATE INDEX IF NOT EXISTS idx_chat_message_pnm_id ON xianyu_chat_message(pnm_id);
CREATE INDEX IF NOT EXISTS idx_chat_message_s_id ON xianyu_chat_message(s_id);
CREATE INDEX IF NOT EXISTS idx_chat_message_sender_user_id ON xianyu_chat_message(sender_user_id);
CREATE INDEX IF NOT EXISTS idx_chat_message_content_type ON xianyu_chat_message(content_type);
CREATE INDEX IF NOT EXISTS idx_chat_message_time ON xianyu_chat_message(message_time);
CREATE INDEX IF NOT EXISTS idx_chat_message_goods_id ON xianyu_chat_message(xy_goods_id);

-- 创建唯一索引，防止重复消息
CREATE UNIQUE INDEX IF NOT EXISTS idx_chat_message_unique 
ON xianyu_chat_message(xianyu_account_id, pnm_id);

-- 商品配置表
CREATE TABLE IF NOT EXISTS xianyu_goods_config (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    xianyu_account_id BIGINT NOT NULL,                -- 闲鱼账号ID
    xianyu_goods_id BIGINT,                           -- 本地闲鱼商品ID
    xy_goods_id VARCHAR(100) NOT NULL,                -- 闲鱼的商品ID
    xianyu_auto_delivery_on TINYINT DEFAULT 0,        -- 自动发货开关：1-开启，0-关闭，默认关闭
    xianyu_auto_reply_on TINYINT DEFAULT 0,           -- 自动回复开关：1-开启，0-关闭，默认关闭
    create_time DATETIME DEFAULT (datetime('now', 'localtime')),   -- 创建时间
    update_time DATETIME DEFAULT (datetime('now', 'localtime')),   -- 更新时间
    FOREIGN KEY (xianyu_account_id) REFERENCES xianyu_account(id)
);

-- 创建商品配置表索引
CREATE INDEX IF NOT EXISTS idx_goods_config_account_id ON xianyu_goods_config(xianyu_account_id);
CREATE INDEX IF NOT EXISTS idx_goods_config_xy_goods_id ON xianyu_goods_config(xy_goods_id);
CREATE UNIQUE INDEX IF NOT EXISTS idx_goods_config_unique ON xianyu_goods_config(xianyu_account_id, xy_goods_id);

-- 创建商品配置表更新时间触发器
CREATE TRIGGER IF NOT EXISTS update_xianyu_goods_config_time
AFTER UPDATE ON xianyu_goods_config
BEGIN
    UPDATE xianyu_goods_config SET update_time = CURRENT_TIMESTAMP WHERE id = NEW.id;
END;
-- 商品自动发货配置表
CREATE TABLE IF NOT EXISTS xianyu_goods_auto_delivery_config (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    xianyu_account_id BIGINT NOT NULL,                -- 闲鱼账号ID
    xianyu_goods_id BIGINT,                           -- 本地闲鱼商品ID
    xy_goods_id VARCHAR(100) NOT NULL,                -- 闲鱼的商品ID
    type TINYINT DEFAULT 1,                           -- 发货类型（1-文本，2-自定义）
    auto_delivery_content TEXT,                       -- 自动发货的文本内容
    auto_confirm_shipment TINYINT DEFAULT 0,          -- 自动确认发货开关：0-关闭，1-开启
    create_time DATETIME DEFAULT (datetime('now', 'localtime')),   -- 创建时间
    update_time DATETIME DEFAULT (datetime('now', 'localtime')),   -- 更新时间
    FOREIGN KEY (xianyu_account_id) REFERENCES xianyu_account(id)
);

-- 创建自动发货配置表索引
CREATE INDEX IF NOT EXISTS idx_auto_delivery_config_account_id ON xianyu_goods_auto_delivery_config(xianyu_account_id);
CREATE INDEX IF NOT EXISTS idx_auto_delivery_config_xy_goods_id ON xianyu_goods_auto_delivery_config(xy_goods_id);
CREATE UNIQUE INDEX IF NOT EXISTS idx_auto_delivery_config_unique ON xianyu_goods_auto_delivery_config(xianyu_account_id, xy_goods_id);

-- 创建自动发货配置表更新时间触发器
CREATE TRIGGER IF NOT EXISTS update_xianyu_goods_auto_delivery_config_time
AFTER UPDATE ON xianyu_goods_auto_delivery_config
BEGIN
    UPDATE xianyu_goods_auto_delivery_config SET update_time = CURRENT_TIMESTAMP WHERE id = NEW.id;
END;
-- 商品自动发货记录表 (优化版: 移除重复字段,通过关联xianyu_order表获取订单信息)
CREATE TABLE IF NOT EXISTS xianyu_goods_auto_delivery_record (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    xianyu_account_id BIGINT NOT NULL,                -- 闲鱼账号ID
    xianyu_goods_id BIGINT,                           -- 本地闲鱼商品ID
    xy_goods_id VARCHAR(100) NOT NULL,                -- 闲鱼的商品ID
    pnm_id VARCHAR(100) NOT NULL,                     -- 消息pnmid，用于防止重复发货
    order_id VARCHAR(100),                            -- 订单ID (关联xianyu_order表)
    content TEXT,                                     -- 发货消息内容
    state TINYINT DEFAULT 0,                          -- 发货是否成功: 1-成功, 0-失败
    create_time DATETIME DEFAULT (datetime('now', 'localtime')),  -- 创建时间(本地时间)
    FOREIGN KEY (xianyu_account_id) REFERENCES xianyu_account(id)
);

-- 创建自动发货记录表索引
CREATE INDEX IF NOT EXISTS idx_auto_delivery_record_account_id ON xianyu_goods_auto_delivery_record(xianyu_account_id);
CREATE INDEX IF NOT EXISTS idx_auto_delivery_record_xy_goods_id ON xianyu_goods_auto_delivery_record(xy_goods_id);
CREATE INDEX IF NOT EXISTS idx_auto_delivery_record_state ON xianyu_goods_auto_delivery_record(state);
CREATE INDEX IF NOT EXISTS idx_auto_delivery_record_create_time ON xianyu_goods_auto_delivery_record(create_time);
CREATE INDEX IF NOT EXISTS idx_auto_delivery_record_pnm_id ON xianyu_goods_auto_delivery_record(pnm_id);
CREATE INDEX IF NOT EXISTS idx_auto_delivery_record_order_id ON xianyu_goods_auto_delivery_record(order_id);

-- 创建唯一索引，防止同一消息重复发货
CREATE UNIQUE INDEX IF NOT EXISTS idx_auto_delivery_record_unique 
ON xianyu_goods_auto_delivery_record(xianyu_account_id, pnm_id);

-- 商品自动回复配置表
CREATE TABLE IF NOT EXISTS xianyu_goods_auto_reply_config (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    xianyu_account_id BIGINT NOT NULL,                -- 闲鱼账号ID
    xianyu_goods_id BIGINT,                           -- 本地闲鱼商品ID
    xy_goods_id VARCHAR(100) NOT NULL,                -- 闲鱼的商品ID
    keyword TEXT,                                     -- 关键词（支持多个，用逗号分隔）
    reply_content TEXT,                               -- 回复内容
    match_type TINYINT DEFAULT 1,                     -- 匹配类型（1-包含，2-完全匹配，3-正则）
    rag_delay_seconds INTEGER DEFAULT 15,             -- RAG回复延时秒数（默认15秒）
    create_time DATETIME DEFAULT (datetime('now', 'localtime')),   -- 创建时间
    update_time DATETIME DEFAULT (datetime('now', 'localtime')),   -- 更新时间
    FOREIGN KEY (xianyu_account_id) REFERENCES xianyu_account(id)
);

-- 创建自动回复配置表索引
CREATE INDEX IF NOT EXISTS idx_auto_reply_config_account_id ON xianyu_goods_auto_reply_config(xianyu_account_id);
CREATE INDEX IF NOT EXISTS idx_auto_reply_config_xy_goods_id ON xianyu_goods_auto_reply_config(xy_goods_id);

-- 创建自动回复配置表更新时间触发器
CREATE TRIGGER IF NOT EXISTS update_xianyu_goods_auto_reply_config_time
AFTER UPDATE ON xianyu_goods_auto_reply_config
BEGIN
    UPDATE xianyu_goods_auto_reply_config SET update_time = CURRENT_TIMESTAMP WHERE id = NEW.id;
END;
-- 商品自动回复记录表
CREATE TABLE IF NOT EXISTS xianyu_goods_auto_reply_record (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    xianyu_account_id BIGINT NOT NULL,                -- 闲鱼账号ID
    xianyu_goods_id BIGINT,                           -- 本地闲鱼商品ID
    xy_goods_id VARCHAR(100) NOT NULL,                -- 闲鱼的商品ID
    s_id VARCHAR(100),                                -- 会话ID（用于延时任务去重）
    pnm_id VARCHAR(100),                              -- 触发回复的消息pnmId
    buyer_user_id VARCHAR(100),                       -- 买家用户ID
    buyer_user_name VARCHAR(200),                     -- 买家用户名
    buyer_message TEXT,                               -- 买家消息内容
    reply_content TEXT,                               -- 回复消息内容
    reply_type TINYINT DEFAULT 1,                     -- 回复类型：1-关键词匹配，2-RAG智能回复
    matched_keyword VARCHAR(200),                     -- 匹配的关键词
    trigger_context TEXT,                             -- 触发上下文JSON（包含触发消息列表和RAG命中资料列表）
    state TINYINT DEFAULT 0,                          -- 状态：0-待回复，1-成功，-1-失败
    create_time DATETIME DEFAULT (datetime('now', 'localtime')),   -- 创建时间
    FOREIGN KEY (xianyu_account_id) REFERENCES xianyu_account(id)
);

-- 创建自动回复记录表索引
CREATE INDEX IF NOT EXISTS idx_auto_reply_record_account_id ON xianyu_goods_auto_reply_record(xianyu_account_id);
CREATE INDEX IF NOT EXISTS idx_auto_reply_record_xy_goods_id ON xianyu_goods_auto_reply_record(xy_goods_id);
CREATE INDEX IF NOT EXISTS idx_auto_reply_record_state ON xianyu_goods_auto_reply_record(state);
CREATE INDEX IF NOT EXISTS idx_auto_reply_record_create_time ON xianyu_goods_auto_reply_record(create_time);
CREATE INDEX IF NOT EXISTS idx_auto_reply_record_s_id ON xianyu_goods_auto_reply_record(s_id);
CREATE INDEX IF NOT EXISTS idx_auto_reply_record_pnm_id ON xianyu_goods_auto_reply_record(pnm_id);
-- 创建唯一索引，防止同一会话重复回复（用于延时任务去重）
CREATE UNIQUE INDEX IF NOT EXISTS idx_auto_reply_record_unique 
ON xianyu_goods_auto_reply_record(xianyu_account_id, s_id, pnm_id);

-- 闲鱼订单表
CREATE TABLE IF NOT EXISTS xianyu_order (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    
    -- 关联信息
    xianyu_account_id BIGINT NOT NULL,            -- 关联的闲鱼账号ID
    
    -- 订单基本信息
    order_id VARCHAR(64) NOT NULL,                -- 订单ID
    xy_goods_id VARCHAR(64),                      -- 闲鱼商品ID
    goods_title VARCHAR(512),                     -- 商品标题
    
    -- 交易双方信息
    buyer_user_id VARCHAR(64),                    -- 买家用户ID
    buyer_user_name VARCHAR(256),                 -- 买家用户名
    seller_user_id VARCHAR(64),                   -- 卖家用户ID
    seller_user_name VARCHAR(256),                -- 危家用户名
    
    -- 订单状态信息
    order_status INTEGER,                         -- 订单状态：1-待付款，2-待发货，3-已发货，4-已完成，5-已取消
    order_status_text VARCHAR(128),               -- 订单状态文本
    
    -- 订单金额信息
    order_amount BIGINT,                          -- 订单金额（单位：分）
    order_amount_text VARCHAR(64),                -- 订单金额文本
    
    -- 关联消息信息
    pnm_id VARCHAR(128),                          -- 关联的消息pnmid
    s_id VARCHAR(128),                            -- 关联的会话ID
    reminder_url TEXT,                            -- 消息链接
    
    -- 时间信息
    order_create_time BIGINT,                     -- 订单创建时间戳（毫秒）
    order_pay_time BIGINT,                        -- 订单支付时间戳（毫秒）
    order_delivery_time BIGINT,                   -- 订单发货时间戳（毫秒）
    order_complete_time BIGINT,                   -- 订单完成时间戳（毫秒）
    create_time DATETIME DEFAULT (datetime('now', 'localtime')),  -- 记录创建时间
    update_time DATETIME DEFAULT (datetime('now', 'localtime')),  -- 记录更新时间
    
    -- 扩展信息
    complete_msg TEXT,                            -- 完整的消息体JSON
    
    -- 外键约束
    FOREIGN KEY (xianyu_account_id) REFERENCES xianyu_account(id)
);

-- 创建订单表索引
CREATE INDEX IF NOT EXISTS idx_order_id ON xianyu_order(order_id);
CREATE INDEX IF NOT EXISTS idx_order_account_id ON xianyu_order(xianyu_account_id);
CREATE INDEX IF NOT EXISTS idx_order_xy_goods_id ON xianyu_order(xy_goods_id);
CREATE INDEX IF NOT EXISTS idx_order_buyer_user_id ON xianyu_order(buyer_user_id);
CREATE INDEX IF NOT EXISTS idx_order_status ON xianyu_order(order_status);
CREATE INDEX IF NOT EXISTS idx_order_create_time ON xianyu_order(create_time);
CREATE INDEX IF NOT EXISTS idx_order_order_create_time ON xianyu_order(order_create_time);

-- 创建唯一索引（同一订单ID在同一账号下唯一）
CREATE UNIQUE INDEX IF NOT EXISTS uk_account_order ON xianyu_order(xianyu_account_id, order_id);

-- 创建订单表更新时间触发器
CREATE TRIGGER IF NOT EXISTS update_xianyu_order_time
AFTER UPDATE ON xianyu_order
BEGIN
    UPDATE xianyu_order SET update_time = CURRENT_TIMESTAMP WHERE id = NEW.id;
END;
-- 操作日志表
CREATE TABLE IF NOT EXISTS xianyu_operation_log (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    xianyu_account_id BIGINT,                        -- 账号ID
    operation_type VARCHAR(50),                      -- 操作类型
    operation_module VARCHAR(100),                   -- 操作模块
    operation_desc VARCHAR(500),                     -- 操作描述
    operation_status TINYINT,                        -- 操作状态：1-成功，0-失败，2-部分成功
    target_type VARCHAR(50),                         -- 目标类型
    target_id VARCHAR(100),                          -- 目标ID
    request_params TEXT,                             -- 请求参数（JSON格式）
    response_result TEXT,                            -- 响应结果（JSON格式）
    error_message TEXT,                              -- 错误信息
    ip_address VARCHAR(50),                          -- IP地址
    user_agent VARCHAR(500),                         -- 浏览器UA
    duration_ms INTEGER,                             -- 操作耗时（毫秒）
    create_time BIGINT,                              -- 创建时间（时间戳，毫秒）
    FOREIGN KEY (xianyu_account_id) REFERENCES xianyu_account(id)
);

-- 创建操作日志表索引
CREATE INDEX IF NOT EXISTS idx_operation_log_account_id ON xianyu_operation_log(xianyu_account_id);
CREATE INDEX IF NOT EXISTS idx_operation_log_type ON xianyu_operation_log(operation_type);
CREATE INDEX IF NOT EXISTS idx_operation_log_status ON xianyu_operation_log(operation_status);
CREATE INDEX IF NOT EXISTS idx_operation_log_create_time ON xianyu_operation_log(create_time);

-- 系统配置表
CREATE TABLE IF NOT EXISTS xianyu_sys_setting (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    setting_key VARCHAR(100) NOT NULL UNIQUE,              -- 配置键
    setting_value TEXT,                                     -- 配置值
    setting_desc VARCHAR(500),                              -- 配置描述
    created_time DATETIME DEFAULT (datetime('now', 'localtime')),  -- 创建时间
    updated_time DATETIME DEFAULT (datetime('now', 'localtime'))   -- 更新时间
);

-- 创建系统配置表索引
CREATE UNIQUE INDEX IF NOT EXISTS idx_sys_setting_key ON xianyu_sys_setting(setting_key);

-- 创建系统配置表更新时间触发器
CREATE TRIGGER IF NOT EXISTS update_xianyu_sys_setting_time
AFTER UPDATE ON xianyu_sys_setting
BEGIN
    UPDATE xianyu_sys_setting SET updated_time = CURRENT_TIMESTAMP WHERE id = NEW.id;
END;
-- 初始化系统配置数据
INSERT OR IGNORE INTO xianyu_sys_setting (setting_key, setting_value, setting_desc)
VALUES ('sys_prompt', '你是一个闲鱼卖家，你叫肥极喵，不要回复的像AI，简短回答
参考相关信息回答,不要乱回答,不知道就换不同语气回复提示用户详细点询问', 'AI智能回复的系统提示词');

-- AI API Key配置（初始为空，用户在前端设置页面配置后生效）
INSERT OR IGNORE INTO xianyu_sys_setting (setting_key, setting_value, setting_desc)
VALUES ('ai_api_key', '', 'AI服务的API Key（配置后立即生效，无需重启）');

-- AI API Base URL配置
INSERT OR IGNORE INTO xianyu_sys_setting (setting_key, setting_value, setting_desc)
VALUES ('ai_base_url', 'https://dashscope.aliyuncs.com/compatible-mode', 'AI服务的API Base URL');

-- AI 模型配置
INSERT OR IGNORE INTO xianyu_sys_setting (setting_key, setting_value, setting_desc)
VALUES ('ai_model', 'deepseek-v3', 'AI对话模型名称');

