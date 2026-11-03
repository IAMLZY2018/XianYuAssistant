# Windows 桌面版

桌面版会把 Electron、Java 21、Spring Boot、SQLite 和 Playwright Chromium 一并打包。最终用户不需要安装 Java、Node.js、Maven、SQLite 或浏览器驱动。

## 构建

构建机需要 Windows x64、Node.js 22 和 Maven。JDK/JRE 会由脚本下载到项目内部，不会修改系统 Java：

```powershell
powershell -ExecutionPolicy Bypass -File .\desktop\scripts\build.ps1 -UseChinaMirror
```

网络可直接访问官方源时可去掉 `-UseChinaMirror`。只构建一种产物可加 `-Target nsis` 或 `-Target portable`。

输出位于 `desktop\dist`：

- `闲鱼助手-2.0.3-x64-setup.exe`：安装版，可创建桌面和开始菜单快捷方式。
- `闲鱼助手-2.0.3-x64-portable.exe`：免安装版，直接双击运行。

## 用户数据

数据库、向量数据和日志保存在 `%APPDATA%\XianYuAssistant`，不会放进安装目录。卸载应用默认保留这些数据：

```text
%APPDATA%\XianYuAssistant\
  dbdata\
    xianyu_assistant.db
    vectorstore.json
  logs\
    desktop-backend.log
  desktop-port.json
```

应用首次启动时选择一个空闲的 `127.0.0.1` 端口，并在后续启动中复用，以保留登录状态。若提示该端口已被占用，请完全退出旧版闲鱼助手或重启电脑后再试；不要同时运行两个后端。关闭窗口后，Electron 会先请求 Spring Boot 正常退出；只有服务无响应时才强制终止进程树。

## 发布说明

当前构建没有代码签名证书，Windows 可能显示“未知发布者”或 SmartScreen 提示。正式分发给大量用户前，应使用受信任的 Windows 代码签名证书对安装版和便携版签名。
