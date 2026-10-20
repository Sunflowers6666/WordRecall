package com.wordrecall.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.wordrecall.domain.LearningRecord;
import com.wordrecall.dto.StatsDto;
import com.wordrecall.mapper.LearningRecordMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

/** 学习统计：累计天数、词数、错词、连续天数、今日词数。 */
@Service
@RequiredArgsConstructor
public class StatsService {

    private final LearningRecordMapper learningRecordMapper;

    public StatsDto stats(Long userId, LocalDate today) {
        Set<LocalDate> days = learningRecordMapper.selectList(Wrappers.<LearningRecord>lambdaQuery()
                        .select(LearningRecord::getLearnDate)
                        .eq(LearningRecord::getUserId, userId)
                        .groupBy(LearningRecord::getLearnDate))
                .stream()
                .map(LearningRecord::getLearnDate)
                .collect(Collectors.toCollection(TreeSet::new));

        int learnedWords = learningRecordMapper.selectList(Wrappers.<LearningRecord>lambdaQuery()
                        .select(LearningRecord::getWordId)
                        .eq(LearningRecord::getUserId, userId)
                        .groupBy(LearningRecord::getWordId))
                .size();

        Long wrong = learningRecordMapper.selectCount(Wrappers.<LearningRecord>lambdaQuery()
                .eq(LearningRecord::getUserId, userId)
                .eq(LearningRecord::getIsWrong, true));
        Long todayWords = learningRecordMapper.selectCount(Wrappers.<LearningRecord>lambdaQuery()
                .eq(LearningRecord::getUserId, userId)
                .eq(LearningRecord::getLearnDate, today));

        return new StatsDto(days.size(), learnedWords, wrong == null ? 0 : wrong.intValue(),
                streak(days, today), todayWords == null ? 0 : todayWords.intValue());
    }

    /**
     * 从今天往前数连续天数；今天还没学时允许从昨天开始算，避免“早上打开就断签”。
     */
    int streak(Set<LocalDate> days, LocalDate today) {
        if (days.isEmpty()) {
            return 0;
        }
        LocalDate cursor = days.contains(today) ? today : today.minusDays(1);
        if (!days.contains(cursor)) {
            return 0;
        }
        int count = 0;
        while (days.contains(cursor)) {
            count++;
            cursor = cursor.minusDays(1);
        }
        return count;
    }
}
