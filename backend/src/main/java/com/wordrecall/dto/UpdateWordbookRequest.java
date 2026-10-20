package com.wordrecall.dto;

import jakarta.validation.constraints.NotNull;

public record UpdateWordbookRequest(@NotNull(message = "wordbookId 不能为空") Long wordbookId) {
}
