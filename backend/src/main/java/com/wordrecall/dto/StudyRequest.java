package com.wordrecall.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/** How many new words the user wants in this batch (1..50; the client default is 20). */
public record StudyRequest(@NotNull(message = "size 不能为空")
                           @Min(value = 1, message = "size 至少为 1")
                           @Max(value = 50, message = "size 最多为 50")
                           Integer size) {
}
