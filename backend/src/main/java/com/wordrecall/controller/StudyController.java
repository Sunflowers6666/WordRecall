package com.wordrecall.controller;

import com.wordrecall.common.ApiResponse;
import com.wordrecall.dto.StudyDto;
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

    @PostMapping("/sessions")
    public ApiResponse<StudyDto> start(@AuthenticationPrincipal UserPrincipal principal,
                                       @Valid @RequestBody StudyRequest request) {
        return ApiResponse.ok(studyService.startSession(principal.userId(), request.size(), LocalDate.now(ZONE)));
    }
}
