const assert = require('node:assert/strict')
const fs = require('node:fs')
const http = require('node:http')
const os = require('node:os')
const path = require('node:path')
const test = require('node:test')
const {
  buildJavaArgs,
  findAvailablePort,
  getOrCreateStablePort,
  requestShutdown,
  resolveRuntimePaths,
  waitForHttp
} = require('../src/backend-manager')

test('resolves packaged runtime paths under Electron resources', () => {
  const result = resolveRuntimePaths({
    isPackaged: true,
    resourcesPath: path.join('C:', 'Program Files', 'XianYu', 'resources'),
    appPath: path.join('C:', 'unused'),
    env: {}
  })

  assert.equal(
    result.javaExecutable,
    path.join('C:', 'Program Files', 'XianYu', 'resources', 'runtime', 'bin', 'javaw.exe')
  )
  assert.equal(
    result.jarPath,
    path.join('C:', 'Program Files', 'XianYu', 'resources', 'backend', 'app.jar')
  )
  assert.equal(
    result.browserPath,
    path.join('C:', 'Program Files', 'XianYu', 'resources', 'backend', 'ms-playwright')
  )
})

test('builds Java arguments with loopback binding and desktop token', () => {
  const args = buildJavaArgs({ jarPath: 'app.jar', port: 43123, shutdownToken: 'secret' })

  assert.ok(args.includes('-jar'))
  assert.ok(args.includes('app.jar'))
  assert.ok(args.includes('--server.address=127.0.0.1'))
  assert.ok(args.includes('--server.port=43123'))
  assert.ok(args.includes('--app.desktop.enabled=true'))
  assert.ok(args.includes('--app.desktop.shutdown-token=secret'))
})

test('finds a usable loopback port', async () => {
  const port = await findAvailablePort()
  assert.ok(Number.isInteger(port) && port > 0)

  const server = http.createServer((_, response) => response.end('ok'))
  await new Promise((resolve, reject) => {
    server.once('error', reject)
    server.listen(port, '127.0.0.1', resolve)
  })
  await new Promise((resolve) => server.close(resolve))
})

test('persists and reuses a stable desktop port', async () => {
  const testDirectory = fs.mkdtempSync(path.join(os.tmpdir(), 'xianyu-desktop-port-'))
  try {
    const firstPort = await getOrCreateStablePort(testDirectory)
    const secondPort = await getOrCreateStablePort(testDirectory)
    const persisted = JSON.parse(
      fs.readFileSync(path.join(testDirectory, 'desktop-port.json'), 'utf8')
    )

    assert.equal(secondPort, firstPort)
    assert.equal(persisted.port, firstPort)
  } finally {
    fs.rmSync(testDirectory, { recursive: true, force: true })
  }
})

test('refuses to change origin when the saved port is occupied', async () => {
  const testDirectory = fs.mkdtempSync(path.join(os.tmpdir(), 'xianyu-desktop-port-'))
  let blocker = null
  try {
    const firstPort = await getOrCreateStablePort(testDirectory)
    blocker = http.createServer()
    await new Promise((resolve, reject) => {
      blocker.once('error', reject)
      blocker.listen(firstPort, '127.0.0.1', resolve)
    })

    await assert.rejects(
      () => getOrCreateStablePort(testDirectory),
      new RegExp(`本地端口 ${firstPort} 已被占用`)
    )
    const persisted = JSON.parse(
      fs.readFileSync(path.join(testDirectory, 'desktop-port.json'), 'utf8')
    )

    assert.equal(persisted.port, firstPort)
  } finally {
    if (blocker) await new Promise((resolve) => blocker.close(resolve))
    fs.rmSync(testDirectory, { recursive: true, force: true })
  }
})

test('waits until an HTTP endpoint becomes healthy', async () => {
  let requestCount = 0
  const server = http.createServer((_, response) => {
    requestCount += 1
    response.statusCode = requestCount < 2 ? 503 : 200
    response.end()
  })
  await new Promise((resolve) => server.listen(0, '127.0.0.1', resolve))
  const port = server.address().port

  await waitForHttp(`http://127.0.0.1:${port}/health`, {
    timeoutMs: 2000,
    intervalMs: 20,
    requestTimeoutMs: 200
  })

  assert.ok(requestCount >= 2)
  await new Promise((resolve) => server.close(resolve))
})

test('requires the expected ready status and token header', async () => {
  let requestCount = 0
  const server = http.createServer((request, response) => {
    requestCount += 1
    const validToken = request.headers['x-desktop-token'] === 'ready-token'
    response.statusCode = validToken && requestCount >= 2 ? 204 : 403
    response.end()
  })
  await new Promise((resolve) => server.listen(0, '127.0.0.1', resolve))
  const port = server.address().port

  await waitForHttp(`http://127.0.0.1:${port}/internal/desktop/ready`, {
    timeoutMs: 2000,
    intervalMs: 20,
    requestTimeoutMs: 200,
    expectedStatus: 204,
    headers: { 'X-Desktop-Token': 'ready-token' }
  })

  assert.ok(requestCount >= 2)
  await new Promise((resolve) => server.close(resolve))
})

test('sends the desktop token when requesting shutdown', async () => {
  let receivedMethod = null
  let receivedToken = null
  const server = http.createServer((request, response) => {
    receivedMethod = request.method
    receivedToken = request.headers['x-desktop-token']
    response.statusCode = 204
    response.end()
  })
  await new Promise((resolve) => server.listen(0, '127.0.0.1', resolve))
  const port = server.address().port

  await requestShutdown(`http://127.0.0.1:${port}`, 'test-token')

  assert.equal(receivedMethod, 'POST')
  assert.equal(receivedToken, 'test-token')
  await new Promise((resolve) => server.close(resolve))
})
