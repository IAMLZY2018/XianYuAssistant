package com.feijimiao.xianyuassistant.service.impl;

import com.feijimiao.xianyuassistant.common.ResultObject;
import com.feijimiao.xianyuassistant.entity.XianyuAccount;
import com.feijimiao.xianyuassistant.mapper.XianyuAccountMapper;
import com.feijimiao.xianyuassistant.service.AccountService;
import com.feijimiao.xianyuassistant.service.ImageUploadService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.UUID;

@Slf4j
@Service
public class ImageUploadServiceImpl implements ImageUploadService {
    
    private static final String UPLOAD_URL = "https://stream-upload.goofish.com/api/upload.api?floderId=0&appkey=xy_chat&_input_charset=utf-8";
    private static final int MAX_WIDTH = 1920;
    private static final int MAX_HEIGHT = 1920;
    private static final int MAX_SIZE = 5 * 1024 * 1024; // 5MB
    private static final int QUALITY = 85;
    
    @Autowired
    private XianyuAccountMapper accountMapper;
    
    @Autowired
    private AccountService accountService;
    
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(30))
            .build();
    
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    @Override
    public ResultObject<String> uploadImage(Long accountId, byte[] imageData, String filename) {
        try {
            XianyuAccount account = accountMapper.selectById(accountId);
            if (account == null) {
                return ResultObject.failed("账号不存在");
            }
            
            String cookie = accountService.getCookieByAccountId(accountId);
            if (cookie == null || cookie.isEmpty()) {
                return ResultObject.failed("账号Cookie为空");
            }
            
            // 压缩图片
            byte[] compressedData = compressImage(imageData);
            if (compressedData == null) {
                return ResultObject.failed("图片压缩失败");
            }
            
            // 上传到闲鱼CDN
            String cdnUrl = uploadToGoofishCDN(cookie, compressedData, filename);
            if (cdnUrl == null) {
                return ResultObject.failed("上传到闲鱼CDN失败");
            }
            if ("COOKIE_EXPIRED".equals(cdnUrl)) {
                accountService.updateCookieStatus(accountId, 2);
                return ResultObject.failed("Cookie已过期，请刷新Cookie");
            }
            
            log.info("【账号{}】图片上传成功: {}", accountId, cdnUrl);
            return ResultObject.success(cdnUrl);
            
        } catch (Exception e) {
            log.error("上传图片失败: accountId={}", accountId, e);
            return ResultObject.failed("上传图片失败: " + e.getMessage());
        }
    }
    
    @Override
    public ResultObject<String> uploadImageFromUrl(Long accountId, String imageUrl) {
        try {
            // 下载图片
            byte[] imageData = downloadImage(imageUrl);
            if (imageData == null) {
                return ResultObject.failed("下载图片失败");
            }
            
            String filename = UUID.randomUUID().toString() + ".jpg";
            return uploadImage(accountId, imageData, filename);
            
        } catch (Exception e) {
            log.error("从URL上传图片失败: accountId={}, url={}", accountId, imageUrl, e);
            return ResultObject.failed("从URL上传图片失败: " + e.getMessage());
        }
    }
    
    private byte[] compressImage(byte[] imageData) {
        try {
            ByteArrayInputStream bis = new ByteArrayInputStream(imageData);
            BufferedImage image = ImageIO.read(bis);
            
            if (image == null) {
                log.error("无法读取图片数据");
                return null;
            }
            
            int width = image.getWidth();
            int height = image.getHeight();
            
            // 调整尺寸
            if (width > MAX_WIDTH || height > MAX_HEIGHT) {
                double scale = Math.min((double) MAX_WIDTH / width, (double) MAX_HEIGHT / height);
                int newWidth = (int) (width * scale);
                int newHeight = (int) (height * scale);
                
                BufferedImage resized = new BufferedImage(newWidth, newHeight, BufferedImage.TYPE_INT_RGB);
                Graphics2D g2d = resized.createGraphics();
                g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
                g2d.drawImage(image, 0, 0, newWidth, newHeight, null);
                g2d.dispose();
                image = resized;
                
                log.info("图片尺寸调整: {}x{} -> {}x{}", width, height, newWidth, newHeight);
            }
            
            // 转换为RGB模式
            if (image.getType() != BufferedImage.TYPE_INT_RGB) {
                BufferedImage rgb = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_INT_RGB);
                Graphics2D g2d = rgb.createGraphics();
                g2d.drawImage(image, 0, 0, null);
                g2d.dispose();
                image = rgb;
            }
            
            // 压缩为JPEG
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            ImageIO.write(image, "jpg", bos);
            byte[] compressed = bos.toByteArray();
            
            // 如果还是太大，降低质量
            if (compressed.length > MAX_SIZE) {
                float quality = 0.5f;
                bos = new ByteArrayOutputStream();
                javax.imageio.ImageWriter writer = ImageIO.getImageWritersByFormatName("jpg").next();
                javax.imageio.ImageWriteParam param = writer.getDefaultWriteParam();
                param.setCompressionMode(javax.imageio.ImageWriteParam.MODE_EXPLICIT);
                param.setCompressionQuality(quality);
                writer.setOutput(ImageIO.createImageOutputStream(bos));
                writer.write(null, new javax.imageio.IIOImage(image, null, null), param);
                writer.dispose();
                compressed = bos.toByteArray();
                log.info("图片质量调整，文件大小: {}KB", compressed.length / 1024);
            }
            
            log.info("图片压缩完成: {}KB", compressed.length / 1024);
            return compressed;
            
        } catch (Exception e) {
            log.error("压缩图片失败", e);
            return null;
        }
    }
    
    private String uploadToGoofishCDN(String cookie, byte[] imageData, String filename) {
        try {
            String boundary = "----WebKitFormBoundary" + UUID.randomUUID().toString().replace("-", "").substring(0, 16);
            
            StringBuilder bodyBuilder = new StringBuilder();
            bodyBuilder.append("--").append(boundary).append("\r\n");
            bodyBuilder.append("Content-Disposition: form-data; name=\"file\"; filename=\"").append(filename).append("\"\r\n");
            bodyBuilder.append("Content-Type: image/jpeg\r\n\r\n");
            
            byte[] header = bodyBuilder.toString().getBytes("UTF-8");
            byte[] footer = ("\r\n--" + boundary + "--\r\n").getBytes("UTF-8");
            
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            bos.write(header);
            bos.write(imageData);
            bos.write(footer);
            byte[] body = bos.toByteArray();
            
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(UPLOAD_URL))
                    .header("Cookie", cookie)
                    .header("Referer", "https://www.goofish.com/")
                    .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
                    .header("Content-Type", "multipart/form-data; boundary=" + boundary)
                    .header("x-requested-with", "XMLHttpRequest")
                    .header("Accept", "application/json, text/javascript, */*; q=0.01")
                    .POST(HttpRequest.BodyPublishers.ofByteArray(body))
                    .timeout(Duration.ofSeconds(30))
                    .build();
            
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            
            if (response.statusCode() == 200) {
                String responseBody = response.body();
                log.debug("上传响应: {}", responseBody);
                
                return parseUploadResponse(responseBody);
            } else if (response.statusCode() == 302 || response.statusCode() == 301) {
                log.error("上传失败: HTTP {}，Cookie已过期（被重定向到登录页）", response.statusCode());
                return "COOKIE_EXPIRED";
            } else {
                log.error("上传失败: HTTP {}", response.statusCode());
                return null;
            }
            
        } catch (Exception e) {
            log.error("上传到闲鱼CDN失败", e);
            return null;
        }
    }
    
    private String parseUploadResponse(String responseBody) {
        try {
            // 检查是否返回HTML（Cookie失效）
            if (responseBody.contains("<!DOCTYPE html>") || responseBody.contains("<html>")) {
                log.error("Cookie已失效，返回了登录页面");
                return "COOKIE_EXPIRED";
            }
            
            JsonNode root = objectMapper.readTree(responseBody);
            
            // 方式1: data.url
            if (root.has("data") && root.get("data").has("url")) {
                return root.get("data").get("url").asText();
            }
            
            // 方式2: object.url
            if (root.has("object") && root.get("object").has("url")) {
                return root.get("object").get("url").asText();
            }
            
            // 方式3: url
            if (root.has("url")) {
                return root.get("url").asText();
            }
            
            // 方式4: result.url
            if (root.has("result") && root.get("result").has("url")) {
                return root.get("result").get("url").asText();
            }
            
            // 方式5: data.fileUrl
            if (root.has("data") && root.get("data").has("fileUrl")) {
                return root.get("data").get("fileUrl").asText();
            }
            
            log.error("无法从响应中提取图片URL: {}", responseBody);
            return null;
            
        } catch (Exception e) {
            log.error("解析上传响应失败", e);
            return null;
        }
    }
    
    private byte[] downloadImage(String imageUrl) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(imageUrl))
                    .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                    .GET()
                    .timeout(Duration.ofSeconds(30))
                    .build();
            
            HttpResponse<byte[]> response = httpClient.send(request, HttpResponse.BodyHandlers.ofByteArray());
            
            if (response.statusCode() == 200) {
                return response.body();
            } else {
                log.error("下载图片失败: HTTP {}", response.statusCode());
                return null;
            }
            
        } catch (Exception e) {
            log.error("下载图片失败: {}", imageUrl, e);
            return null;
        }
    }
}
