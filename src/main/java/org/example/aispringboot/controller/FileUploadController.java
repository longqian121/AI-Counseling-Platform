package org.example.aispringboot.controller;

import jakarta.annotation.Resource;
import org.example.aispringboot.common.Result;
import org.example.aispringboot.entity.SysFileInfo;
import org.example.aispringboot.exception.BusinessException;
import org.example.aispringboot.mapper.SysFileInfoMapper;
import org.example.aispringboot.util.JwtTokenUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/file")
public class FileUploadController {

    @Value("${app.upload.path:./uploads}")
    private String uploadPath;

    @Value("${app.upload.allowed-types:image/jpeg,image/png,image/gif,image/webp}")
    private String allowedTypes;

    @Resource
    private SysFileInfoMapper sysFileInfoMapper;

    @PostMapping("/upload")
    public Result<Map<String, Object>> upload(
            @RequestParam("file") MultipartFile file,
            @RequestParam("businessType") String businessType,
            @RequestParam(value = "businessId", required = false) String businessId,
            @RequestParam(value = "businessField", required = false) String businessField) {

        // 1. 文件校验
        if (file.isEmpty()) {
            throw new BusinessException("文件不能为空");
        }

        // 2. 文件类型校验
        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new BusinessException("仅支持上传图片文件");
        }

        // 3. 生成存储路径：uploads/{businessType}/{yyyy/MM}/{uuid}.{ext}
        String originalFilename = file.getOriginalFilename();
        String ext = StringUtils.getFilenameExtension(originalFilename);
        if (ext == null) {
            ext = "";
        }
        String datePath = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy/MM"));
        String fileName = UUID.randomUUID().toString().replace("-", "") + "." + ext;
        String relativePath = "/" + businessType.toLowerCase() + "/" + datePath + "/" + fileName;

        // 4. 保存文件到磁盘
        Path targetPath = Paths.get(uploadPath, relativePath);
        try {
            Files.createDirectories(targetPath.getParent());
            file.transferTo(targetPath);
        } catch (IOException e) {
            throw new BusinessException("文件保存失败: " + e.getMessage());
        }

        // 5. 生成文件路径URL
        String filePath = "/uploads" + relativePath;

        // 6. 写入 sys_file_info 表
        SysFileInfo sysFileInfo = SysFileInfo.builder()
                .originalName(originalFilename)
                .filePath(filePath)
                .fileSize(file.getSize())
                .fileType(contentType)
                .businessType(businessType)
                .businessId(businessId != null ? businessId : null)
                .businessField(businessField)
                .uploadUserId(getCurrentUserId())
                .isTemp(0)
                .status(1)
                .createTime(LocalDateTime.now())
                .build();
        sysFileInfoMapper.insert(sysFileInfo);

        // 7. 构造响应
        Map<String, Object> result = new HashMap<>();
        result.put("fileName", fileName);
        result.put("filePath", filePath);
        result.put("fileSize", file.getSize());

        return Result.ok(result);
    }

    /**
     * 获取当前登录用户ID
     */
    private Long getCurrentUserId() {
        String token = JwtTokenUtil.getCurrentToken();
        if (token != null) {
            JwtTokenUtil.TokenVerificationResult result = JwtTokenUtil.validateToken(token);
            if (result != null && result.isValid()) {
                return result.getUserId();
            }
        }
        return null;
    }
}

