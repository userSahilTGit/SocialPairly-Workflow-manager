package com.SocialPairly_Workflow_Manager.service;

import com.SocialPairly_Workflow_Manager.exception.BadRequestException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class FileStorageServiceAdditionalTests {

    @TempDir
    Path tempDir;

    @Test
    void storeShouldRejectEmptyFile() {
        FileStorageService service = new FileStorageService(tempDir.toString());

        MockMultipartFile file = new MockMultipartFile("file", "image.png", "image/png", new byte[0]);
        assertThrows(BadRequestException.class, () -> service.store(file));
    }

    @Test
    void storeShouldRejectNonImageFiles() {
        FileStorageService service = new FileStorageService(tempDir.toString());
        MockMultipartFile file = new MockMultipartFile("file", "text.txt", "text/plain", "hello".getBytes());

        assertThrows(BadRequestException.class, () -> service.store(file));
    }

    @Test
    void storeShouldRejectNullFileAndNullContentTypeAndNullOriginalName() {
        FileStorageService service = new FileStorageService(tempDir.toString());
        assertThrows(BadRequestException.class, () -> service.store(null));

        MockMultipartFile noType = new MockMultipartFile("file", "a.png", null, "x".getBytes());
        assertThrows(BadRequestException.class, () -> service.store(noType));

        MockMultipartFile noName = new MockMultipartFile("file", null, "image/png", "x".getBytes());
        String path = service.store(noName);
        assertTrue(path.startsWith("/uploads/"));
    }
}
