package com.wordrecall.dto;

import java.util.List;

/**
 * @param month   requested month, {@code yyyy-MM}
 * @param days    only days that have learning records, ascending by date
 * @param summary month totals
 */
public record CalendarDto(String month, List<CalendarDayDto> days, MonthSummaryDto summary) {
}
