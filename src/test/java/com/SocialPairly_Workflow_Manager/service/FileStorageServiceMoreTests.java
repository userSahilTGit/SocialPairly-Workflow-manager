package com.SocialPairly_Workflow_Manager.service;

import com.SocialPairly_Workflow_Manager.exception.BadRequestException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;

import java.io.ByteArrayInputStream;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class FileStorageServiceMoreTests {

    @TempDir
    Path tempDir;

    @Test
    void shouldRejectFileWhenContentTypeMissing() {
        FileStorageService service = new FileStorageService(tempDir.toString());
        MockMultipartFile file = new MockMultipartFile("file", "photo.png", null, "data".getBytes());

        BadRequestException ex = assertThrows(BadRequestException.class, () -> service.store(file));
        assertTrue(ex.getMessage().contains("Only image files are allowed"));
    }

    @Test
    void shouldStoreImageWithExtensionWhenPresent() throws Exception {
        FileStorageService service = new FileStorageService(tempDir.toString());
        MockMultipartFile file = new MockMultipartFile("file", "photo.jpeg", "image/jpeg", "data".getBytes());

        String url = service.store(file);

        assertTrue(url.startsWith("/uploads/"));
        Path stored = tempDir.resolve(url.substring(9));
        assertTrue(Files.exists(stored));
        assertTrue(new String(Files.readAllBytes(stored)).contains("data"));
    }

    @Test
    void shouldStoreImageWhenOriginalFilenameIsNull() throws Exception {
        FileStorageService service = new FileStorageService(tempDir.toString());
        MockMultipartFile file = new MockMultipartFile("file", null, "image/png", "data".getBytes());

        String url = service.store(file);

        assertTrue(url.startsWith("/uploads/"));
        Path stored = tempDir.resolve(url.substring(9));
        assertTrue(Files.exists(stored));
    }
}
