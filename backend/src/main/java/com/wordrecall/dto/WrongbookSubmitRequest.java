package com.wordrecall.dto;

import jakarta.validation.constraints.NotEmpty;

import java.util.List;

/** 复习后「记住了」的单词，服务端把每个词最新一条记录置为已掌握。 */
public record WrongbookSubmitRequest(@NotEmpty(message = "rememberedWordIds 不能为空")
                                     List<Long> rememberedWordIds) {
}
