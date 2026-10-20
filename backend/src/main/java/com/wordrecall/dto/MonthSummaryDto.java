package com.wordrecall.dto;

/** Totals for the requested month: number of days with records, words learned, words marked wrong. */
public record MonthSummaryDto(int days, int words, int wrong) {
}
