package com.wordrecall.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * Words the user has actually moved past in the learn page. Recording is the
 * client's job now, so that opening a batch and leaving records nothing.
 */
public record StudyRecordRequest(@NotEmpty(message = "wordIds 不能为空")
                                 @Size(max = 50, message = "一次最多记录 50 个单词")
                                 List<Long> wordIds) {
}
