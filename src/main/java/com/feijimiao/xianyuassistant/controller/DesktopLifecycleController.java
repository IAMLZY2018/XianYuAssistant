package com.feijimiao.xianyuassistant.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/internal/desktop")
@ConditionalOnProperty(prefix = "app.desktop", name = "enabled", havingValue = "true")
public class DesktopLifecycleController {

    private final ConfigurableApplicationContext applicationContext;
    private final String shutdownToken;

    public DesktopLifecycleController(
            ConfigurableApplicationContext applicationContext,
            @Value("${app.desktop.shutdown-token:}") String shutdownToken) {
        this.applicationContext = applicationContext;
        this.shutdownToken = shutdownToken;
    }

    @GetMapping("/ready")
    public ResponseEntity<Void> ready(
            @RequestHeader(name = "X-Desktop-Token", required = false) String suppliedToken) {
        if (!tokensMatch(shutdownToken, suppliedToken)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/shutdown")
    public ResponseEntity<Void> shutdown(
            @RequestHeader(name = "X-Desktop-Token", required = false) String suppliedToken) {
        if (!tokensMatch(shutdownToken, suppliedToken)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        CompletableFuture.delayedExecutor(200, TimeUnit.MILLISECONDS).execute(() -> {
            applicationContext.close();
            System.exit(0);
        });
        return ResponseEntity.noContent().build();
    }

    private boolean tokensMatch(String expected, String supplied) {
        if (expected == null || expected.isBlank() || supplied == null) {
            return false;
        }
        return MessageDigest.isEqual(
                expected.getBytes(StandardCharsets.UTF_8),
                supplied.getBytes(StandardCharsets.UTF_8));
    }
}
