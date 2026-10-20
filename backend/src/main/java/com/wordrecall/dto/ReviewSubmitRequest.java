package com.wordrecall.dto;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.util.List;

/**
 * Submit the review result of one day. Every word learned that day is treated as
 * "remembered" except the ids listed in {@code wrongWordIds}.
 */
public record ReviewSubmitRequest(@NotNull(message = "date 不能为空") LocalDate date,
                                  List<Long> wrongWordIds) {
}
