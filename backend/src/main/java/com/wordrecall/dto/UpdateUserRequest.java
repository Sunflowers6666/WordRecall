package com.wordrecall.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

/**
 * Both fields are optional; at least one must be present (checked in the service).
 */
public record UpdateUserRequest(@Size(max = 64, message = "昵称最多 64 个字符")
                                String nickname,
                                @Min(value = 10, message = "每批词数至少 10")
                                @Max(value = 30, message = "每批词数最多 30")
                                Integer batchSize) {
}
