const path = require('node:path')
const crypto = require('node:crypto')
const { app, BrowserWindow, dialog, shell } = require('electron')
const {
  assertRuntimeFiles,
  getOrCreateStablePort,
  resolveRuntimePaths,
  startBackend,
  stopBackend,
  waitForHttp
} = require('./backend-manager')

const userDataPath = path.join(app.getPath('appData'), 'XianYuAssistant')
app.setPath('userData', userDataPath)
app.setAppUserModelId('com.feijimiao.xianyuassistant.desktop')

let mainWindow = null
let backend = null
let backendOrigin = null
let shutdownToken = null
let quitting = false
let backendReady = false
let startupAbortController = null

const hasSingleInstanceLock = app.requestSingleInstanceLock()
if (!hasSingleInstanceLock) {
  app.quit()
} else {
  app.on('second-instance', () => {
    if (!mainWindow) return
    if (mainWindow.isMinimized()) mainWindow.restore()
    mainWindow.show()
    mainWindow.focus()
  })
}

function isTrustedAppUrl(target) {
  if (!backendOrigin) return false
  try {
    return new URL(target).origin === backendOrigin
  } catch {
    return false
  }
}

function openExternalUrl(target) {
  try {
    const protocol = new URL(target).protocol
    if (protocol === 'https:' || protocol === 'http:' || protocol === 'mailto:') {
      void shell.openExternal(target).catch(() => {})
    }
  } catch {
    // Invalid URLs are ignored instead of being handed to the operating system.
  }
}

function createMainWindow() {
  const window = new BrowserWindow({
    width: 1360,
    height: 860,
    minWidth: 1000,
    minHeight: 680,
    show: false,
    autoHideMenuBar: true,
    backgroundColor: '#f5f7f8',
    title: '闲鱼助手',
    webPreferences: {
      nodeIntegration: false,
      contextIsolation: true,
      sandbox: true,
      devTools: !app.isPackaged,
      webSecurity: true,
      allowRunningInsecureContent: false
    }
  })

  window.webContents.setWindowOpenHandler(({ url }) => {
    if (isTrustedAppUrl(url)) void window.loadURL(url).catch(() => {})
    else openExternalUrl(url)
    return { action: 'deny' }
  })

  window.webContents.on('will-navigate', (event, url) => {
    if (isTrustedAppUrl(url)) return
    event.preventDefault()
    openExternalUrl(url)
  })

  window.once('ready-to-show', () => window.show())
  window.on('closed', () => {
    mainWindow = null
  })
  return window
}

async function showFatalError(title, message, logPath) {
  const detail = logPath ? `${message}\n\n日志文件：${logPath}` : message
  const buttons = logPath ? ['打开日志位置', '退出'] : ['退出']
  const options = {
    type: 'error',
    title,
    message: title,
    detail,
    buttons,
    defaultId: 0,
    cancelId: buttons.length - 1,
    noLink: true
  }
  const result = mainWindow && !mainWindow.isDestroyed()
    ? await dialog.showMessageBox(mainWindow, options)
    : await dialog.showMessageBox(options)
  if (logPath && result.response === 0) shell.showItemInFolder(logPath)
}

async function launchApplication() {
  mainWindow = createMainWindow()
  await mainWindow.loadFile(path.join(__dirname, 'loading.html'))

  const runtimePaths = resolveRuntimePaths({
    isPackaged: app.isPackaged,
    resourcesPath: process.resourcesPath,
    appPath: app.getAppPath()
  })
  assertRuntimeFiles(runtimePaths)

  const port = await getOrCreateStablePort(userDataPath)
  backendOrigin = `http://127.0.0.1:${port}`
  shutdownToken = crypto.randomBytes(32).toString('hex')
  backend = startBackend({
    runtimePaths,
    userDataPath,
    port,
    shutdownToken
  })

  startupAbortController = new AbortController()
  const unexpectedExit = new Promise((_, reject) => {
    backend.child.once('error', (error) => {
      startupAbortController.abort(error)
      reject(error)
    })
    backend.child.once('exit', (code, signal) => {
      if (!quitting && !backendReady) {
        startupAbortController.abort(new Error('Backend exited during startup'))
        reject(new Error(`Backend exited during startup (code=${code}, signal=${signal || 'none'})`))
      }
    })
  })

  await Promise.race([
    waitForHttp(`${backendOrigin}/internal/desktop/ready`, {
      expectedStatus: 204,
      headers: { 'X-Desktop-Token': shutdownToken },
      signal: startupAbortController.signal
    }),
    unexpectedExit
  ])
  backendReady = true

  backend.child.once('exit', async (code, signal) => {
    if (quitting) return
    await showFatalError(
      '本地服务已停止',
      `后端进程意外退出（代码 ${code}，信号 ${signal || '无'}）。`,
      backend.logPath
    )
    app.quit()
  })

  await mainWindow.loadURL(`${backendOrigin}/`)
}

if (hasSingleInstanceLock) {
  app.whenReady().then(async () => {
    try {
      await launchApplication()
    } catch (error) {
      if (quitting) return
      const message = error instanceof Error ? error.message : String(error)
      await showFatalError('闲鱼助手启动失败', message, backend?.logPath)
      app.quit()
    }
  })
}

app.on('window-all-closed', () => app.quit())

app.on('before-quit', (event) => {
  if (quitting) return
  event.preventDefault()
  quitting = true
  startupAbortController?.abort(new Error('Application is quitting'))
  mainWindow?.hide()
  void stopBackend({
    child: backend?.child,
    origin: backendOrigin,
    shutdownToken
  }).finally(() => app.exit(0))
})
