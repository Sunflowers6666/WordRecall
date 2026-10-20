package com.wordrecall.service;

import com.wordrecall.common.BizException;
import com.wordrecall.common.ErrorCode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.core.io.Resource;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AudioServiceTest {

    @TempDir
    Path tempDir;

    @Test
    void returnsExistingMp3() throws Exception {
        Files.createDirectories(tempDir.resolve("us"));
        Files.write(tempDir.resolve("us").resolve("42.mp3"), new byte[]{1, 2, 3});

        AudioService service = new AudioService(tempDir.toString());
        Resource resource = service.load(42L, "us");

        assertThat(resource.exists()).isTrue();
        assertThat(resource.contentLength()).isEqualTo(3);
    }

    @Test
    void missingAudioIsNotFound() {
        AudioService service = new AudioService(tempDir.toString());

        assertThatThrownBy(() -> service.load(7L, "us"))
                .isInstanceOfSatisfying(BizException.class,
                        exception -> assertThat(exception.getCode()).isEqualTo(ErrorCode.NOT_FOUND));
    }

    @Test
    void unknownAccentIsRejected() {
        AudioService service = new AudioService(tempDir.toString());

        assertThatThrownBy(() -> service.load(7L, "jp"))
                .isInstanceOfSatisfying(BizException.class,
                        exception -> assertThat(exception.getCode()).isEqualTo(ErrorCode.BAD_REQUEST));
    }

    @Test
    void nullAccentDefaultsToUs() throws Exception {
        Files.createDirectories(tempDir.resolve("us"));
        Files.write(tempDir.resolve("us").resolve("8.mp3"), new byte[]{9});

        AudioService service = new AudioService(tempDir.toString());

        assertThat(service.load(8L, null).exists()).isTrue();
    }

    @Test
    void pathTraversalIsBlocked() throws Exception {
        Files.createDirectories(tempDir.resolve("us"));
        AudioService service = new AudioService(tempDir.toString());

        assertThatThrownBy(() -> service.load(1L, "../us"))
                .isInstanceOfSatisfying(BizException.class,
                        exception -> assertThat(exception.getCode()).isEqualTo(ErrorCode.BAD_REQUEST));
    }
}
