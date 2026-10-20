package com.wordrecall.dto;

import java.util.List;

/** Words first learned on {@code date}, in learning order. Empty list means no records for that day. */
public record ReviewDto(String date, List<WordDto> words) {
}
