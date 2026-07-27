package com.SocialPairly_Workflow_Manager.service;

import com.SocialPairly_Workflow_Manager.exception.BadRequestException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

@Service
public class FileStorageService {

    private static final Logger log = LoggerFactory.getLogger(FileStorageService.class);
    private final Path uploadPath;

    public FileStorageService(@Value("${app.upload.dir}") String uploadDir) {
        this.uploadPath = Paths.get(uploadDir).toAbsolutePath().normalize();
        log.info("Initializing file storage path={}", this.uploadPath);
        try {
            Files.createDirectories(this.uploadPath);
        } catch (IOException e) {
            log.error("Could not create upload directory {}", this.uploadPath, e);
            throw new IllegalStateException("Could not create upload directory", e);
        }
    }

    public String store(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            log.warn("Attempted to store empty file");
            throw new BadRequestException("File is empty");
        }

        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            log.warn("Attempted to store invalid file type contentType={}", contentType);
            throw new BadRequestException("Only image files are allowed");
        }

        String original = file.getOriginalFilename();
        String extension = "";
        int dot = original == null ? -1 : original.lastIndexOf('.');
        if (dot >= 0) {
            extension = original.substring(dot);
        }

        String filename = UUID.randomUUID() + extension;
        log.info("Storing file originalName={} generatedName={} contentType={}", original, filename, contentType);

        try {
            Path target = this.uploadPath.resolve(filename).normalize();
            if (!target.startsWith(this.uploadPath)) {
                log.warn("Invalid file path resolved target={} uploadPath={}", target, this.uploadPath);
                throw new BadRequestException("Invalid file path");
            }
            Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            log.error("Failed to store file {}", filename, e);
            throw new IllegalStateException("Failed to store file", e);
        }

        return "/uploads/" + filename;
    }
}