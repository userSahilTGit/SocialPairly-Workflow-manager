package com.SocialPairly_Workflow_Manager.service;

import com.SocialPairly_Workflow_Manager.exception.BadRequestException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import javax.imageio.ImageIO;

import static org.junit.jupiter.api.Assertions.*;

class FileStorageServiceTest {

    @TempDir
    Path tempDir;

    @Test
    void shouldStoreValidImageFile() throws IOException {
        BufferedImage image = new BufferedImage(10, 10, BufferedImage.TYPE_INT_RGB);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageIO.write(image, "png", out);

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "photo.png",
                "image/png",
                out.toByteArray()
        );

        FileStorageService service = new FileStorageService(tempDir.toString());

        String url = service.store(file);

        assertTrue(url.startsWith("/uploads/"));
        Path storedFile = tempDir.resolve(url.substring(9));
        assertTrue(Files.exists(storedFile));
        assertTrue(Files.size(storedFile) > 0);
    }

    @Test
    void shouldRejectNonImageFile() {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "text.txt",
                "text/plain",
                "hello".getBytes()
        );

        FileStorageService service = new FileStorageService(tempDir.toString());

        assertThrows(BadRequestException.class, () -> service.store(file));
    }
}
