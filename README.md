# 闲鱼自动化管理系统

<div align="center">

![License](https://img.shields.io/badge/license-Apache%202.0-blue.svg)
![Java](https://img.shields.io/badge/Java-21-orange.svg)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.5.7-brightgreen.svg)
![Vue](https://img.shields.io/badge/Vue-3.5-green.svg)

一个功能强大的闲鱼店铺自动化管理工具，支持自动发货、自动回复、AI智能客服、消息管理等功能。

[功能特性](#功能特性) • [部署方式](#部署方式) • [使用指南](#使用指南) • [截图展示](#截图展示) • [技术栈](#技术栈) • [常见问题](#常见问题)

</div>

---

## 📸 截图展示

### 消息管理

查看聊天记录，支持快速回复和消息筛选：

<div align="center">
  <img src="docs/images/1.png" alt="消息管理" width="800"/>
  <p><i>消息管理 - 聊天记录查看与快速回复</i></p>
</div>

### 自动发货配置

配置商品自动发货规则，支持自动确认收货：

<div align="center">
  <img src="docs/images/2-1.png" alt="自动发货配置" width="800"/>
  <p><i>自动发货 - 自动同步闲鱼商品列表</i></p>
</div>

<div align="center">
  <img src="docs/images/2.png" alt="自动发货配置" width="800"/>
  <p><i>自动发货 -  配置发货内容与规则</i></p>
</div>


### 闲鱼账号管理

管理多个闲鱼账号，支持扫码登录：

<div align="center">
  <img src="docs/images/3.png" alt="闲鱼账号管理" width="800"/>
  <p><i>账号管理 - 多账号统一管理</i></p>
</div>

### 商品管理

同步和管理闲鱼商品，配置自动化功能：

<div align="center">
  <img src="docs/images/4.png" alt="商品管理" width="800"/>
  <p><i>商品管理 - 商品列表与配置</i></p>
</div>

### 自动回复配置

支持为商品单独配置知识库，根据知识库回答用户问题：

<div align="center">
  <img src="docs/images/5.png" alt="自动回复配置" width="800"/>
  <p><i>自动回复 - 商品知识库配置</i></p>
</div>
---

## 📋 功能特性

### 核心功能

- 🔐 **多账号管理** - 支持同时管理多个闲鱼账号，轻松切换
- 🔗 **WebSocket连接** - 实时监听闲鱼消息，及时响应买家
- 🚀 **自动发货** - 买家付款后自动发送发货信息，节省时间
- 💬 **自动回复** - 智能匹配关键词，自动回复买家消息
- 🤖 **AI智能客服** - 集成通义千问大模型 + RAG知识库，智能回复买家
- 📦 **商品管理** - 同步商品信息，统一管理在售商品
- 📋 **订单管理** - 查看订单列表，支持一键确认发货
- 💌 **消息管理** - 查看聊天记录，支持快速回复

### 高级功能

- 🔄 **Token自动刷新** - 智能维护登录状态，随机间隔避免检测
- 📊 **数据统计** - 实时查看账号、商品、订单等数据统计
- 📜 **操作日志** - 详细记录所有操作，方便追踪和排查
- 🎯 **消息过滤** - 支持按商品、账号筛选消息
- 🔐 **滑块验证处理** - 智能检测验证需求，提供详细操作指引
- 🧠 **RAG知识库** - 按商品维度构建向量知识库，提升AI回复准确性
- 🔌 **自定义发货** - 提供API接入指南，支持外部系统对接发货流程
- 📱 **响应式设计** - 完美适配桌面、平板、手机三种设备模式

---

## 🚀 部署方式

### Docker 部署（推荐）

使用 Docker 一键部署，所有配置已数据库化，无需配置文件。

#### 环境要求

- **Docker**: 20.10+

#### 快速启动

**Linux/Mac**:

```bash
docker run -d \
  --name xianyu-assistant \
  -p 12400:12400 \
  -v $(pwd)/data/dbdata:/app/dbdata \
  -v $(pwd)/data/logs:/app/logs \
  --restart unless-stopped \
  iamlzy/xianyuassistant:latest
```

**Windows PowerShell**:

```powershell
docker run -d `
  --name xianyu-assistant `
  -p 12400:12400 `
  -v ${PWD}/data/dbdata:/app/dbdata `
  -v ${PWD}/data/logs:/app/logs `
  --restart unless-stopped `
  iamlzy/xianyuassistant:latest
```

> **数据目录说明**:
> - `dbdata/` - 数据库数据目录，包含：
>   - `xianyu_assistant.db` - SQLite数据库（账号、商品、订单、配置等）
>   - `vectorstore.json` - 向量数据库（AI知识库向量数据）
> - `logs/` - 应用日志目录
>
> ⚠️ **重要警告**:
> - **容器内路径（`/app/dbdata` 和 `/app/logs`）不能修改**，这是应用固定的数据存储路径
> - **物理机路径可以自定义**，但一旦设置后不要随意更改，否则会导致数据丢失
> - **升级版本时请使用相同的物理机路径**，确保数据能够正确迁移
> - **建议定期备份 `dbdata/` 目录**，防止意外数据丢失

启动后访问: `http://localhost:12400`

#### 自定义配置

通过环境变量自定义配置：

> ⚠️ **数据路径警告**: 修改物理机路径前请先备份 `dbdata/` 目录，避免数据丢失！

**Linux/Mac**:

```bash
docker run -d \
  --name xianyu-assistant \
  -p 12400:12400 \
  -e JAVA_OPTS="-Xms256m -Xmx512m" \
  -v /your/path/dbdata:/app/dbdata \
  -v /your/path/logs:/app/logs \
  --restart unless-stopped \
  iamlzy/xianyuassistant:latest
```

**Windows PowerShell**:

```powershell
docker run -d `
  --name xianyu-assistant `
  -p 12400:12400 `
  -e JAVA_OPTS="-Xms256m -Xmx512m" `
  -v D:\your\path\dbdata:/app/dbdata `
  -v D:\your\path\logs:/app/logs `
  --restart unless-stopped `
  iamlzy/xianyuassistant:latest
```

#### 配置项说明

| 参数 | 默认值 | 说明 |
|------|--------|------|
| 端口映射 | `12400:12400` | 应用服务端口（物理机端口:容器端口） |
| 数据卷 `/app/dbdata` | - | 数据库数据目录，包含：<br>- `xianyu_assistant.db`：SQLite数据库（账号、商品、订单、配置等）<br>- `vectorstore.json`：向量数据库（AI知识库向量数据） |
| 数据卷 `/app/logs` | - | 应用日志目录 |
| 环境变量 `JAVA_OPTS` | `-Xms256m -Xmx512m` | JVM 内存参数 |
| 环境变量 `SERVER_PORT` | `12400` | Spring Boot 服务端口（容器内部） |

> **提示**: 所有业务配置（如阿里云 API Key、AI 模型等）已数据库化，可在系统设置页面直接配置，无需修改环境变量。

#### 常用命令

```bash
# 启动服务
docker start xianyu-assistant

# 停止服务
docker stop xianyu-assistant

# 查看日志
docker logs -f xianyu-assistant

# 重启服务
docker restart xianyu-assistant

# 更新到最新版本
docker pull iamlzy/xianyuassistant:latest
docker stop xianyu-assistant
docker rm xianyu-assistant
# 然后重新执行 docker run 命令

# 进入容器
docker exec -it xianyu-assistant sh
```

#### 服务器部署示例

```bash
# 1. SSH 连接服务器
ssh username@your-server-ip

# 2. 安装 Docker（如未安装）
curl -fsSL https://get.docker.com | sh
sudo systemctl start docker && sudo systemctl enable docker

# 3. 创建数据目录
mkdir -p /opt/xianyu-assistant/data

# 4. 启动服务
docker run -d \
  --name xianyu-assistant \
  -p 12400:12400 \
  -v /opt/xianyu-assistant/data/dbdata:/app/dbdata \
  -v /opt/xianyu-assistant/data/logs:/app/logs \
  --restart unless-stopped \
  iamlzy/xianyuassistant:latest

# 5. 访问应用
# 浏览器打开 http://your-server-ip:12400
```

---

## 📖 使用指南

### 快速上手

#### 1️⃣ 添加闲鱼账号

- 进入"闲鱼账号"页面
- 点击"扫码登录"按钮
- 使用闲鱼APP扫描二维码
- 等待登录成功

#### 2️⃣ 启动WebSocket连接

- 进入"连接管理"页面
- 选择要连接的账号
- 点击"启动连接"按钮
- 等待连接成功

> ⚠️ **注意**: 如果遇到滑块验证，请按照弹窗提示操作：
> 1. 访问闲鱼IM页面完成验证
> 2. 点击"❓ 如何获取？"按钮查看教程
> 3. 手动更新Cookie和Token

#### 3️⃣ 同步商品信息

- 进入"商品管理"页面
- 选择已连接的账号
- 点击"刷新商品"按钮
- 等待商品同步完成

#### 4️⃣ 配置自动化功能

- 在商品列表中找到目标商品
- 开启"自动发货"或"自动回复"
- 配置发货内容或回复规则
- 保存配置，自动化开始工作

### 功能说明

#### 自动发货

当买家付款后，系统会自动检测到"已付款待发货"消息，并根据配置自动发送发货信息。

**配置步骤**:
1. 进入"自动发货"页面
2. 选择商品
3. 切换到"自动发货"标签页
4. 开启自动发货开关
5. 输入发货内容（支持文本、链接、卡密等）
6. 可选：开启"自动确认发货"
7. 保存配置

#### 自定义发货

支持通过API接口对接外部系统实现自定义发货逻辑。

**使用步骤**:
1. 进入"自动发货"页面
2. 选择商品
3. 切换到"自定义发货"标签页
4. 查看API接入指南，包含接口地址、请求参数、参数说明
5. 点击"复制"按钮获取接口和参数信息
6. 在外部系统中调用API完成发货

#### 自动回复

智能匹配买家消息中的关键词，自动发送预设的回复内容。

**配置步骤**:
1. 进入"自动回复"页面
2. 选择商品，点击"添加规则"
3. 设置关键词和回复内容
4. 选择匹配方式（精确/模糊/正则）
5. 保存规则

#### AI智能客服

集成通义千问大模型，通过RAG知识库实现智能回复。

**配置步骤**:
1. 在系统设置页面配置阿里云 API Key
2. 在AI对话页面上传商品知识库数据
3. 开启AI自动回复

#### Token刷新策略

系统采用随机间隔刷新策略，避免被检测为机器人：

- **Cookie保活**: 每30分钟调用hasLogin接口
- **_m_h5_tk**: 1.5-2.5小时随机刷新
- **websocket_token**: 10-14小时随机刷新
- **账号间隔**: 2-5秒随机

---

## 🛠️ 技术栈

### 后端

| 技术 | 版本 | 用途 |
|------|------|------|
| Java | 21 | 编程语言 |
| Spring Boot | 3.5.7 | 应用框架 |
| MyBatis-Plus | 3.5.5 | ORM框架 |
| SQLite | 3.42.0 | 嵌入式数据库 |
| Java-WebSocket | 1.5.4 | WebSocket客户端 |
| OkHttp | 4.12.0 | HTTP客户端 |
| Gson | 2.10.1 | JSON处理 |
| MessagePack | 0.9.8 | 消息解密 |
| Playwright | 1.40.0 | 浏览器自动化(扫码登录) |
| ZXing | 3.5.3 | 二维码生成 |
| Spring AI | 1.1.4 | AI集成(通义千问+RAG) |
| Lombok | - | 简化代码 |

### 前端

| 技术 | 版本 | 用途 |
|------|------|------|
| Vue | 3.5 | 渐进式框架 |
| TypeScript | 5.x | 类型安全 |
| Element Plus | 2.11 | UI组件库 |
| Vite | 7.x | 构建工具 |
| Axios | 1.13 | HTTP客户端 |
| Pinia | 3.0 | 状态管理 |
| Vue Router | 4.6 | 路由管理 |

### 响应式设计

系统支持三种设备模式的自适应布局：

| 设备模式 | 屏幕宽度 | 导航方式 | 特性 |
|---------|---------|---------|------|
| 桌面模式 | > 1024px | 固定侧边栏 | 完整功能展示，侧边栏常驻显示 |
| 平板模式 | 768px - 1024px | 可折叠侧边栏 | 自动折叠侧边栏，点击按钮展开/收起 |
| 手机模式 | < 768px | 抽屉式菜单 | 顶部导航栏 + 全屏抽屉菜单 |

**响应式特性**：
- 自动检测屏幕尺寸并切换布局模式
- 平板模式下侧边栏默认折叠，节省屏幕空间
- 平滑的过渡动画效果
- 选择菜单项后自动收起侧边栏（平板模式）

---

## ❓ 常见问题

### 1. WebSocket连接失败怎么办？

**解决方案**:
1. 检查Cookie是否有效
2. 尝试手动更新Token
3. 如果提示需要滑块验证，访问 https://www.goofish.com/im 完成验证后手动更新Cookie和Token

### 2. 如何获取Cookie和Token？

点击连接管理页面中Cookie和Token区域的"❓ 如何获取？"按钮，查看详细的图文教程。

### 3. 自动发货什么时候触发？

当买家付款后，系统会自动检测到"已付款待发货"消息，并根据配置自动发送发货信息。

### 4. Token过期了怎么办？

系统会自动刷新Token（1.5-2.5小时刷新一次），也可以在连接管理页面手动更新。

### 5. 为什么不建议频繁启动/断开连接？

频繁操作容易触发闲鱼的人机验证，导致账号暂时不可用。建议保持连接稳定。

### 6. 如何使用自定义发货？

切换到"自定义发货"标签页，查看API接入指南，复制接口地址和请求参数，在外部系统中调用 `/api/order/list` 获取待发货订单，再调用 `/api/order/confirmShipment` 确认发货。

### 7. AI智能客服如何配置？

1. 在系统设置页面配置阿里云 API Key
2. 在AI对话页面上传商品知识库数据
3. 系统将自动使用RAG检索相关知识并生成智能回复

### 8. Docker部署数据存在哪里？

默认存储在容器的 `/app/dbdata` 和 `/app/logs` 目录，通过数据卷映射到物理机：

**数据目录结构**:
```
data/
├── dbdata/                          # 数据库数据目录
│   ├── xianyu_assistant.db          # SQLite数据库
│   │   - 账号信息
│   │   - 商品数据
│   │   - 订单记录
│   │   - 系统配置
│   │   - 自动化规则
│   │   - 操作日志
│   └── vectorstore.json             # 向量数据库
│       - AI知识库向量数据
│       - RAG检索索引
└── logs/                            # 应用日志目录
    └── xianyu-assistant.log         # 应用运行日志
```

**重要提示**:
- ⚠️ **容器内路径（`/app/dbdata` 和 `/app/logs`）不能修改**
- ⚠️ **物理机路径一旦设置不要随意更改**，否则会导致数据丢失
- ⚠️ **升级版本时请使用相同的物理机路径**，确保数据能够正确迁移
- 定期备份 `dbdata/` 目录，避免数据丢失
- 向量数据库文件 `vectorstore.json` 会随着知识库数据增加而变大
- 日志文件会持续增长，建议定期清理或配置日志轮转

### 9. 如何更新到最新版本？

```bash
docker pull iamlzy/xianyuassistant:latest
docker stop xianyu-assistant
docker rm xianyu-assistant
# 然后重新执行 docker run 命令
```

---

## 🤝 贡献指南

感谢Python版本提供的参考：

https://github.com/zhinianboke/xianyu-auto-reply

欢迎提交Issue和Pull Request！

**仓库地址:**

- 🇨🇳 Gitee: https://gitee.com/lzy2018cn/xian-yu-assistant
- 🌍 GitHub: https://github.com/IAMLZY2018/XianYuAssistant

**贡献步骤:**

1. Fork本项目
2. 创建特性分支 (`git checkout -b feature/AmazingFeature`)
3. 提交更改 (`git commit -m 'Add some AmazingFeature'`)
4. 推送到分支 (`git push origin feature/AmazingFeature`)
5. 提交Pull Request

---

## 📄 许可证

本项目采用 Apache License 2.0 许可证 - 查看 [LICENSE](LICENSE) 文件了解详情

---

## ⚠️ 免责声明

本项目仅供学习交流使用，请勿用于商业用途。使用本工具产生的任何后果由使用者自行承担。

---

## 📧 联系方式

如有问题或建议，欢迎通过以下方式联系：

- 提交 [Issue](https://github.com/IAMLZY2018/XianYuAssistant/issues)
- **联系作者:** https://www.feijimiao.cn/contact

---

<div align="center">

**如果这个项目对你有帮助，请给个 ⭐️ Star 支持一下！**

Made with ❤️

</div>
