package com.feijimiao.xianyuassistant.config;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.Playwright;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

@Slf4j
@Component
public class PlaywrightManager {

    private volatile Playwright playwright;
    private volatile Browser browser;
    private final ReentrantLock lock = new ReentrantLock();
    private volatile boolean initialized = false;

    private static final String BROWSER_CACHE_DIR;

    static {
        String jarDir = getJarDirectory();
        BROWSER_CACHE_DIR = jarDir + File.separator + "ms-playwright";
        System.setProperty("PLAYWRIGHT_BROWSERS_PATH", BROWSER_CACHE_DIR);
        log.info("Playwright浏览器缓存目录: {}", BROWSER_CACHE_DIR);
    }

    private static String getJarDirectory() {
        try {
            String jarPath = PlaywrightManager.class.getProtectionDomain()
                    .getCodeSource().getLocation().toURI().getPath();
            File jarFile = new File(jarPath);
            if (jarFile.isFile()) {
                return jarFile.getParent();
            }
            return jarFile.getAbsolutePath();
        } catch (Exception e) {
            String userDir = System.getProperty("user.dir");
            log.warn("无法获取JAR目录，使用user.dir: {}", userDir, e);
            return userDir;
        }
    }

    @PostConstruct
    public void init() {
        log.info("PlaywrightManager初始化，浏览器缓存目录: {}", BROWSER_CACHE_DIR);
    }

    public BrowserContext createContext() {
        lock.lock();
        try {
            ensureBrowserReady();
            Browser.NewContextOptions contextOptions = new Browser.NewContextOptions()
                    .setUserAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36");
            return browser.newContext(contextOptions);
        } catch (Exception e) {
            log.error("创建BrowserContext失败，尝试重建浏览器实例", e);
            try {
                rebuild();
                Browser.NewContextOptions contextOptions = new Browser.NewContextOptions()
                        .setUserAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36");
                return browser.newContext(contextOptions);
            } catch (Exception ex) {
                log.error("重建浏览器后仍然失败", ex);
                throw new RuntimeException("Playwright浏览器不可用", ex);
            }
        } finally {
            lock.unlock();
        }
    }

    private void ensureBrowserReady() {
        if (browser != null && browser.isConnected()) {
            return;
        }
        log.info("Playwright浏览器未就绪或已断开，开始初始化...");
        doInit();
    }

    private void doInit() {
        try {
            if (this.playwright != null) {
                try {
                    this.playwright.close();
                } catch (Exception ignored) {
                }
            }
            this.playwright = Playwright.create();
            BrowserType.LaunchOptions launchOptions = new BrowserType.LaunchOptions()
                    .setHeadless(true);
            this.browser = this.playwright.chromium().launch(launchOptions);
            this.initialized = true;
            log.info("Playwright浏览器初始化成功");
        } catch (Exception e) {
            log.error("Playwright浏览器初始化失败", e);
            this.initialized = false;
            throw new RuntimeException("Playwright浏览器初始化失败", e);
        }
    }

    private void rebuild() {
        log.info("重建Playwright浏览器实例...");
        closeQuietly();
        doInit();
    }

    private void closeQuietly() {
        try {
            if (browser != null) {
                browser.close();
            }
        } catch (Exception ignored) {
        }
        try {
            if (playwright != null) {
                playwright.close();
            }
        } catch (Exception ignored) {
        }
        browser = null;
        playwright = null;
        initialized = false;
    }

    @PreDestroy
    public void destroy() {
        log.info("PlaywrightManager销毁，关闭浏览器资源...");
        lock.lock();
        try {
            closeQuietly();
        } finally {
            lock.unlock();
        }
        log.info("PlaywrightManager已销毁");
    }

    public boolean isInitialized() {
        return initialized && browser != null && browser.isConnected();
    }

    public void cleanTempFiles() {
        try {
            Path tmpDir = Paths.get(System.getProperty("java.io.tmpdir"));
            long now = System.currentTimeMillis();
            long thresholdMs = TimeUnit.HOURS.toMillis(1);
            long[] deletedCount = {0};
            long[] deletedSize = {0};

            Files.list(tmpDir)
                    .filter(path -> {
                        String name = path.getFileName().toString();
                        return name.startsWith("playwright") || name.contains("chromium")
                                || name.startsWith("core.") || name.endsWith(".pipe")
                                || name.endsWith(".sock");
                    })
                    .forEach(path -> {
                        try {
                            File file = path.toFile();
                            if (file.isDirectory()) {
                                long dirSize = deleteDirectory(file);
                                deletedCount[0]++;
                                deletedSize[0] += dirSize;
                            } else {
                                long fileAge = now - file.lastModified();
                                if (fileAge > thresholdMs) {
                                    long fileSize = file.length();
                                    if (file.delete()) {
                                        deletedCount[0]++;
                                        deletedSize[0] += fileSize;
                                    }
                                }
                            }
                        } catch (Exception ignored) {
                        }
                    });

            if (deletedCount[0] > 0) {
                log.info("清理Playwright临时文件: {}个文件, 释放空间: {}KB",
                        deletedCount[0], deletedSize[0] / 1024);
            }
        } catch (Exception e) {
            log.warn("清理Playwright临时文件失败", e);
        }
    }

    private long deleteDirectory(File directory) {
        long totalSize = 0;
        File[] files = directory.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isDirectory()) {
                    totalSize += deleteDirectory(file);
                } else {
                    totalSize += file.length();
                    file.delete();
                }
            }
        }
        directory.delete();
        return totalSize;
    }
}
