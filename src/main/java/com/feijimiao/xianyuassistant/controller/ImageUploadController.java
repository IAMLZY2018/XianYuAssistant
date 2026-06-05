package com.feijimiao.xianyuassistant.controller;

import com.feijimiao.xianyuassistant.common.ResultObject;
import com.feijimiao.xianyuassistant.service.ImageUploadService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@Slf4j
@RestController
@RequestMapping("/api/image")
@CrossOrigin(origins = "*")
public class ImageUploadController {
    
    @Autowired
    private ImageUploadService imageUploadService;
    
    /**
     * 上传图片到闲鱼CDN
     *
     * @param accountId 账号ID
     * @param file 图片文件
     * @return CDN URL
     */
    @PostMapping("/upload")
    public ResultObject<String> uploadImage(
            @RequestParam("accountId") Long accountId,
            @RequestParam("file") MultipartFile file) {
        try {
            if (file.isEmpty()) {
                return ResultObject.failed("文件不能为空");
            }
            
            String filename = file.getOriginalFilename();
            if (filename == null || filename.isEmpty()) {
                filename = "image.jpg";
            }
            
            // 检查文件类型
            String contentType = file.getContentType();
            if (contentType == null || !contentType.startsWith("image/")) {
                return ResultObject.failed("只能上传图片文件");
            }
            
            // 检查文件大小（最大10MB）
            if (file.getSize() > 10 * 1024 * 1024) {
                return ResultObject.failed("图片大小不能超过10MB");
            }
            
            byte[] imageData = file.getBytes();
            return imageUploadService.uploadImage(accountId, imageData, filename);
            
        } catch (IOException e) {
            log.error("读取文件失败", e);
            return ResultObject.failed("读取文件失败: " + e.getMessage());
        } catch (Exception e) {
            log.error("上传图片失败", e);
            return ResultObject.failed("上传图片失败: " + e.getMessage());
        }
    }
    
    /**
     * 通过URL上传图片到闲鱼CDN
     *
     * @param accountId 账号ID
     * @param imageUrl 图片URL
     * @return CDN URL
     */
    @PostMapping("/uploadFromUrl")
    public ResultObject<String> uploadImageFromUrl(
            @RequestParam("accountId") Long accountId,
            @RequestParam("imageUrl") String imageUrl) {
        try {
            return imageUploadService.uploadImageFromUrl(accountId, imageUrl);
        } catch (Exception e) {
            log.error("从URL上传图片失败", e);
            return ResultObject.failed("从URL上传图片失败: " + e.getMessage());
        }
    }
}
