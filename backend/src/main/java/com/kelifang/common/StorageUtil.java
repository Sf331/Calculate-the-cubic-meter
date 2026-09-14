package com.kelifang.common;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

/**
 * 本地磁盘文件存储。demo 阶段不接对象存储。
 * 返回的是相对路径，前端通过 /uploads/** 访问。
 */
@Component
public class StorageUtil {

    private final Path root;

    public StorageUtil(@Value("${kelifang.storage-path}") String storagePath) throws IOException {
        this.root = Path.of(storagePath).toAbsolutePath().normalize();
        Files.createDirectories(root);
    }

    public String store(MultipartFile file) {
        String original = file.getOriginalFilename();
        String ext = "";
        if (original != null && original.lastIndexOf('.') >= 0) {
            ext = original.substring(original.lastIndexOf('.'));
        }
        String name = UUID.randomUUID().toString().replace("-", "") + ext;

        try {
            file.transferTo(root.resolve(name));
        } catch (IOException e) {
            throw new BizException("文件保存失败：" + e.getMessage());
        }
        return name;
    }

    public void delete(String relativePath) {
        try {
            Files.deleteIfExists(root.resolve(relativePath));
        } catch (IOException e) {
            throw new BizException("文件删除失败：" + e.getMessage());
        }
    }
}
