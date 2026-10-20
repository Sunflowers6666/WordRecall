package com.wordrecall.dto;

/** One calendar cell that has learning records. {@code date} is ISO-8601 (yyyy-MM-dd). */
public record CalendarDayDto(String date, int wordCount, int wrongCount) {
}
