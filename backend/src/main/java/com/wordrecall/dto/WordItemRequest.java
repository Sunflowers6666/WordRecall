package com.wordrecall.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record WordItemRequest(@NotBlank(message = "单词不能为空")
                              @Size(max = 100, message = "单词最多 100 个字符")
                              String spelling,
                              @Size(max = 100, message = "音标最多 100 个字符")
                              String phonetic,
                              @NotBlank(message = "释义不能为空")
                              String meaning,
                              String exampleSentence,
                              String exampleTranslation) {
}
