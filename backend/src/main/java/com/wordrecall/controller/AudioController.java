package com.wordrecall.controller;

import com.wordrecall.service.AudioService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;

/** 预生成的单词发音，公开可访问（不含用户数据）。 */
@RestController
@RequestMapping("/api/words")
@RequiredArgsConstructor
public class AudioController {

    private final AudioService audioService;

    @GetMapping("/{id}/audio")
    public ResponseEntity<Resource> audio(@PathVariable Long id,
                                          @RequestParam(defaultValue = "us") String accent) {
        Resource resource = audioService.load(id, accent);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("audio/mpeg"))
                .cacheControl(CacheControl.maxAge(Duration.ofDays(7)).cachePublic())
                .body(resource);
    }
}
