package com.wordrecall.controller;

import com.wordrecall.common.ApiResponse;
import com.wordrecall.dto.StudyDto;
import com.wordrecall.dto.StudyRecordDto;
import com.wordrecall.dto.StudyRecordRequest;
import com.wordrecall.dto.StudyRequest;
import com.wordrecall.security.UserPrincipal;
import com.wordrecall.service.StudyService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.ZoneId;

@RestController
@RequestMapping("/api/study")
@RequiredArgsConstructor
public class StudyController {

    /** Product dates follow China Standard Time regardless of the server default zone. */
    static final ZoneId ZONE = ZoneId.of("Asia/Shanghai");

    private final StudyService studyService;

    /** 只预览一批未学单词，不写学习记录。 */
    @PostMapping("/sessions")
    public ApiResponse<StudyDto> start(@AuthenticationPrincipal UserPrincipal principal,
                                       @Valid @RequestBody StudyRequest request) {
        return ApiResponse.ok(studyService.startSession(principal.userId(), request.size(), LocalDate.now(ZONE)));
    }

    /** 客户端翻过某些单词后上报，服务端才真正写入当天学习记录。 */
    @PostMapping("/records")
    public ApiResponse<StudyRecordDto> record(@AuthenticationPrincipal UserPrincipal principal,
                                              @Valid @RequestBody StudyRecordRequest request) {
        return ApiResponse.ok(studyService.recordWords(principal.userId(), request.wordIds(), LocalDate.now(ZONE)));
    }
}
