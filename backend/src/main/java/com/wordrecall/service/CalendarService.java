package com.wordrecall.service;

import com.wordrecall.common.BizException;
import com.wordrecall.common.ErrorCode;
import com.wordrecall.domain.LearningRecord;
import com.wordrecall.dto.CalendarDayDto;
import com.wordrecall.dto.CalendarDto;
import com.wordrecall.dto.MonthSummaryDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/** Monthly calendar data for the home page: which days have records and how many. */
@Service
@RequiredArgsConstructor
public class CalendarService {

    private final LearningRecordService learningRecordService;

    public CalendarDto month(Long userId, int year, int month) {
        if (year < 2000 || year > 2100 || month < 1 || month > 12) {
            throw new BizException(ErrorCode.BAD_REQUEST, "月份不合法");
        }

        YearMonth yearMonth = YearMonth.of(year, month);
        List<LearningRecord> records = learningRecordService.findByUserAndDateRange(
                userId, yearMonth.atDay(1), yearMonth.atEndOfMonth());

        Map<LocalDate, int[]> byDate = new TreeMap<>();
        for (LearningRecord record : records) {
            int[] counters = byDate.computeIfAbsent(record.getLearnDate(), key -> new int[2]);
            counters[0]++;
            if (Boolean.TRUE.equals(record.getIsWrong())) {
                counters[1]++;
            }
        }

        List<CalendarDayDto> days = new ArrayList<>(byDate.size());
        int totalWords = 0;
        int totalWrong = 0;
        for (Map.Entry<LocalDate, int[]> entry : byDate.entrySet()) {
            days.add(new CalendarDayDto(entry.getKey().toString(), entry.getValue()[0], entry.getValue()[1]));
            totalWords += entry.getValue()[0];
            totalWrong += entry.getValue()[1];
        }

        return new CalendarDto(yearMonth.toString(), days,
                new MonthSummaryDto(days.size(), totalWords, totalWrong));
    }
}
