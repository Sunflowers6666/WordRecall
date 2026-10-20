package com.wordrecall.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

/**
 * @param size 可省略：省略时使用用户设置的每批词数（10 / 20 / 30，默认 20）
 */
public record StudyRequest(@Min(value = 1, message = "size 至少为 1")
                           @Max(value = 50, message = "size 最多为 50")
                           Integer size) {
}
