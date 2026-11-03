const fs = require('node:fs')
const http = require('node:http')
const net = require('node:net')
const path = require('node:path')
const { spawn } = require('node:child_process')

function resolveRuntimePaths({ isPackaged, resourcesPath, appPath, env = process.env }) {
  const resourceRoot = isPackaged ? resourcesPath : path.join(appPath, 'resources')
  return {
    javaExecutable:
      env.XIANYU_DESKTOP_JAVA || path.join(resourceRoot, 'runtime', 'bin', 'javaw.exe'),
    jarPath: env.XIANYU_DESKTOP_JAR || path.join(resourceRoot, 'backend', 'app.jar'),
    browserPath:
      env.XIANYU_PLAYWRIGHT_PATH || path.join(resourceRoot, 'backend', 'ms-playwright')
  }
}

function assertRuntimeFiles(runtimePaths) {
  const required = [
    ['Java runtime', runtimePaths.javaExecutable],
    ['Spring Boot application', runtimePaths.jarPath],
    ['Playwright browser', runtimePaths.browserPath]
  ]
  const missing = required.filter(([, target]) => !fs.existsSync(target))
  if (missing.length > 0) {
    const details = missing.map(([label, target]) => `${label}: ${target}`).join('\n')
    throw new Error(`Desktop runtime is incomplete. Missing:\n${details}`)
  }
}

function buildJavaArgs({ jarPath, port, shutdownToken }) {
  return [
    '-Xms128m',
    '-Xmx512m',
    '-Dfile.encoding=UTF-8',
    '-Djava.awt.headless=true',
    '-jar',
    jarPath,
    '--spring.main.banner-mode=off',
    '--server.address=127.0.0.1',
    `--server.port=${port}`,
    '--app.desktop.enabled=true',
    `--app.desktop.shutdown-token=${shutdownToken}`
  ]
}

function findAvailablePort(host = '127.0.0.1', preferredPort = 0) {
  return new Promise((resolve, reject) => {
    const server = net.createServer()
    server.unref()
    server.once('error', reject)
    server.listen(preferredPort, host, () => {
      const address = server.address()
      const port = typeof address === 'object' && address ? address.port : 0
      server.close((error) => {
        if (error) reject(error)
        else resolve(port)
      })
    })
  })
}

async function getOrCreateStablePort(userDataPath, host = '127.0.0.1') {
  fs.mkdirSync(userDataPath, { recursive: true })
  const portFile = path.join(userDataPath, 'desktop-port.json')
  let savedPort = null
  try {
    const parsed = JSON.parse(fs.readFileSync(portFile, 'utf8'))
    if (Number.isInteger(parsed.port) && parsed.port >= 1024 && parsed.port <= 65535) {
      savedPort = parsed.port
    }
  } catch {
    // A missing or invalid file is replaced after selecting a new port.
  }

  if (savedPort !== null) {
    try {
      return await findAvailablePort(host, savedPort)
    } catch (error) {
      throw new Error(
        `本地端口 ${savedPort} 已被占用。请完全退出旧版闲鱼助手，或重启电脑后再试。`,
        { cause: error }
      )
    }
  }

  const port = await findAvailablePort(host)
  const temporaryFile = `${portFile}.${process.pid}.tmp`
  fs.writeFileSync(temporaryFile, `${JSON.stringify({ port }, null, 2)}\n`, 'utf8')
  fs.renameSync(temporaryFile, portFile)
  return port
}

function probeHttp(url, timeoutMs, expectedStatus, headers) {
  return new Promise((resolve) => {
    const request = http.get(url, { headers }, (response) => {
      response.resume()
      resolve(response.statusCode === expectedStatus)
    })
    request.setTimeout(timeoutMs, () => request.destroy())
    request.on('error', () => resolve(false))
  })
}

function delay(milliseconds) {
  return new Promise((resolve) => setTimeout(resolve, milliseconds))
}

async function waitForHttp(
  url,
  {
    timeoutMs = 120000,
    intervalMs = 300,
    requestTimeoutMs = 1500,
    expectedStatus = 200,
    headers = {},
    signal
  } = {}
) {
  const deadline = Date.now() + timeoutMs
  while (Date.now() < deadline) {
    if (signal?.aborted) throw signal.reason || new Error('Startup was cancelled')
    if (await probeHttp(url, requestTimeoutMs, expectedStatus, headers)) return
    await delay(intervalMs)
  }
  throw new Error(`Backend did not become ready within ${timeoutMs}ms: ${url}`)
}

function startBackend({ runtimePaths, userDataPath, port, shutdownToken }) {
  fs.mkdirSync(path.join(userDataPath, 'dbdata'), { recursive: true })
  fs.mkdirSync(path.join(userDataPath, 'logs'), { recursive: true })

  const logPath = path.join(userDataPath, 'logs', 'desktop-backend.log')
  const logStream = fs.createWriteStream(logPath, { flags: 'a' })
  logStream.write(`\n===== Desktop start ${new Date().toISOString()} =====\n`)

  const child = spawn(
    runtimePaths.javaExecutable,
    buildJavaArgs({ jarPath: runtimePaths.jarPath, port, shutdownToken }),
    {
      cwd: userDataPath,
      windowsHide: true,
      stdio: ['ignore', 'pipe', 'pipe'],
      env: {
        ...process.env,
        PLAYWRIGHT_BROWSERS_PATH: runtimePaths.browserPath,
        SPRING_OUTPUT_ANSI_ENABLED: 'NEVER'
      }
    }
  )

  child.stdout.pipe(logStream, { end: false })
  child.stderr.pipe(logStream, { end: false })
  logStream.on('error', () => {})
  child.on('error', (error) => {
    if (!logStream.destroyed) logStream.write(`Backend process error: ${error.stack || error}\n`)
  })
  child.once('close', (code, signal) => {
    logStream.end(`===== Desktop backend exit code=${code} signal=${signal || 'none'} =====\n`)
  })

  return { child, logPath }
}

function requestShutdown(origin, shutdownToken, timeoutMs = 3000) {
  return new Promise((resolve, reject) => {
    const target = new URL('/internal/desktop/shutdown', origin)
    const request = http.request(
      target,
      {
        method: 'POST',
        headers: {
          'X-Desktop-Token': shutdownToken,
          'Content-Length': '0'
        }
      },
      (response) => {
        response.resume()
        if (response.statusCode === 204) resolve()
        else reject(new Error(`Backend rejected shutdown with HTTP ${response.statusCode}`))
      }
    )
    request.setTimeout(timeoutMs, () => request.destroy(new Error('Backend shutdown timed out')))
    request.on('error', reject)
    request.end()
  })
}

function waitForExit(child, timeoutMs) {
  if (!child || child.exitCode !== null || child.signalCode !== null) return Promise.resolve(true)
  return new Promise((resolve) => {
    let settled = false
    const finish = (exited) => {
      if (settled) return
      settled = true
      clearTimeout(timer)
      child.removeListener('exit', onExit)
      child.removeListener('close', onExit)
      resolve(exited)
    }
    const onExit = () => finish(true)
    const timer = setTimeout(() => finish(false), timeoutMs)
    child.once('exit', onExit)
    child.once('close', onExit)
  })
}

async function forceKillProcessTree(child) {
  if (!child || child.exitCode !== null || child.signalCode !== null) return Promise.resolve()
  if (process.platform !== 'win32') {
    child.kill('SIGTERM')
    if (!(await waitForExit(child, 3000))) child.kill('SIGKILL')
    return
  }

  if (!Number.isInteger(child.pid)) return

  await new Promise((resolve) => {
    const killer = spawn('taskkill.exe', ['/PID', String(child.pid), '/T', '/F'], {
      windowsHide: true,
      stdio: 'ignore'
    })
    const timer = setTimeout(() => {
      killer.kill()
      resolve()
    }, 5000)
    killer.once('exit', () => {
      clearTimeout(timer)
      resolve()
    })
    killer.once('error', () => {
      clearTimeout(timer)
      resolve()
    })
  })
  if (await waitForExit(child, 2000)) return
  try {
    child.kill()
  } catch {
    // The process may have exited between the check and the kill call.
  }
  await waitForExit(child, 1000)
}

async function stopBackend({ child, origin, shutdownToken }) {
  if (!child || child.exitCode !== null || child.signalCode !== null) return
  try {
    await requestShutdown(origin, shutdownToken)
  } catch {
    // The forced process-tree stop below is the final fallback.
  }
  if (!(await waitForExit(child, 7000))) await forceKillProcessTree(child)
}

module.exports = {
  assertRuntimeFiles,
  buildJavaArgs,
  findAvailablePort,
  getOrCreateStablePort,
  requestShutdown,
  resolveRuntimePaths,
  startBackend,
  stopBackend,
  waitForHttp
}
