package com.wordrecall.service;

import com.wordrecall.common.BizException;
import com.wordrecall.common.ErrorCode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Set;

/**
 * Serves pre-generated pronunciation files. Audio is produced offline by
 * {@code scripts/dict/generate_audio.py} and lives under {@code app.audio.dir}.
 */
@Service
public class AudioService {

    private static final Set<String> ACCENTS = Set.of("us", "uk");

    private final Path baseDir;

    public AudioService(@Value("${app.audio.dir:data/audio}") String dir) {
        this.baseDir = Paths.get(dir).toAbsolutePath().normalize();
    }

    public Resource load(Long wordId, String accent) {
        String key = accent == null ? "us" : accent.toLowerCase();
        if (!ACCENTS.contains(key)) {
            throw new BizException(ErrorCode.BAD_REQUEST, "不支持的发音类型");
        }
        Path file = baseDir.resolve(key).resolve(wordId + ".mp3").normalize();
        if (!file.startsWith(baseDir) || !Files.isRegularFile(file)) {
            throw new BizException(ErrorCode.NOT_FOUND, "音频不存在");
        }
        return new FileSystemResource(file);
    }
}
