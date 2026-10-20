package com.wordrecall.controller;

import com.wordrecall.common.ApiResponse;
import com.wordrecall.dto.ImportPreviewDto;
import com.wordrecall.dto.ImportResultDto;
import com.wordrecall.security.UserPrincipal;
import com.wordrecall.service.ImportService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

/** 词表导入：先预览，再选择去向写入。 */
@RestController
@RequestMapping("/api/imports")
@RequiredArgsConstructor
public class ImportController {

    private final ImportService importService;

    @PostMapping("/preview")
    public ApiResponse<ImportPreviewDto> preview(@RequestPart("file") MultipartFile file)
            throws IOException {
        return ApiResponse.ok(importService.preview(file));
    }

    @PostMapping
    public ApiResponse<ImportResultDto> importWords(@AuthenticationPrincipal UserPrincipal principal,
                                                    @RequestPart("file") MultipartFile file,
                                                    @RequestParam(required = false) Long bookId,
                                                    @RequestParam(required = false) String newBookName)
            throws IOException {
        return ApiResponse.ok(importService.importInto(principal.userId(), bookId, newBookName, file));
    }
}
