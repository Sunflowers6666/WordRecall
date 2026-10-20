package com.wordrecall.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.wordrecall.common.BizException;
import com.wordrecall.common.ErrorCode;
import com.wordrecall.domain.LearningRecord;
import com.wordrecall.mapper.LearningRecordMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * All reads and writes of {@code learning_record}. A record means "this user first
 * learned this word on this date", which is exactly what the calendar reviews.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LearningRecordService {

    private final LearningRecordMapper learningRecordMapper;

    public List<LearningRecord> findByUserAndDate(Long userId, LocalDate date) {
        return learningRecordMapper.selectList(Wrappers.<LearningRecord>lambdaQuery()
                .eq(LearningRecord::getUserId, userId)
                .eq(LearningRecord::getLearnDate, date)
                .orderByAsc(LearningRecord::getId));
    }

    public List<LearningRecord> findByUserAndDateRange(Long userId, LocalDate from, LocalDate to) {
        return learningRecordMapper.selectList(Wrappers.<LearningRecord>lambdaQuery()
                .eq(LearningRecord::getUserId, userId)
                .ge(LearningRecord::getLearnDate, from)
                .le(LearningRecord::getLearnDate, to)
                .orderByAsc(LearningRecord::getLearnDate)
                .orderByAsc(LearningRecord::getId));
    }

    public long countByUserAndDate(Long userId, LocalDate date) {
        Long count = learningRecordMapper.selectCount(Wrappers.<LearningRecord>lambdaQuery()
                .eq(LearningRecord::getUserId, userId)
                .eq(LearningRecord::getLearnDate, date));
        return count == null ? 0 : count;
    }

    /** Word ids the user has ever learned in this wordbook. */
    public Set<Long> findLearnedWordIds(Long userId, Long wordbookId) {
        return learningRecordMapper.selectList(Wrappers.<LearningRecord>lambdaQuery()
                        .select(LearningRecord::getWordId)
                        .eq(LearningRecord::getUserId, userId)
                        .eq(LearningRecord::getWordbookId, wordbookId))
                .stream()
                .map(LearningRecord::getWordId)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    /**
     * Insert a record for every word not yet recorded on {@code date}.
     * Duplicates (same user + word + date) are skipped rather than failing the batch.
     *
     * @return number of records actually inserted
     */
    @Transactional
    public int recordStudy(Long userId, Long wordbookId, Collection<Long> wordIds, LocalDate date) {
        if (wordIds == null || wordIds.isEmpty()) {
            return 0;
        }

        Set<Long> alreadyRecorded = learningRecordMapper.selectList(Wrappers.<LearningRecord>lambdaQuery()
                        .select(LearningRecord::getWordId)
                        .eq(LearningRecord::getUserId, userId)
                        .eq(LearningRecord::getLearnDate, date)
                        .in(LearningRecord::getWordId, wordIds))
                .stream()
                .map(LearningRecord::getWordId)
                .collect(Collectors.toSet());

        int inserted = 0;
        for (Long wordId : new LinkedHashSet<>(wordIds)) {
            if (alreadyRecorded.contains(wordId)) {
                continue;
            }
            LearningRecord record = new LearningRecord();
            record.setUserId(userId);
            record.setWordId(wordId);
            record.setWordbookId(wordbookId);
            record.setLearnDate(date);
            record.setIsWrong(false);
            try {
                inserted += learningRecordMapper.insert(record);
            } catch (DuplicateKeyException exception) {
                // A concurrent batch inserted the same user + word + date first.
                log.debug("skip duplicate learning record, userId={}, wordId={}, date={}", userId, wordId, date);
            }
        }
        return inserted;
    }

    /**
     * Replace the review result of one day: every record of that day becomes
     * "remembered" except the words in {@code wrongWordIds}.
     */
    @Transactional
    public ReviewOutcome submitReview(Long userId, LocalDate date, Collection<Long> wrongWordIds) {
        List<LearningRecord> records = findByUserAndDate(userId, date);
        if (records.isEmpty()) {
            throw new BizException(ErrorCode.NOT_FOUND, "这天没有学习记录");
        }

        Set<Long> wrong = wrongWordIds == null ? Set.of() : new LinkedHashSet<>(wrongWordIds);
        Set<Long> known = records.stream().map(LearningRecord::getWordId).collect(Collectors.toSet());
        if (!known.containsAll(wrong)) {
            throw new BizException(ErrorCode.BAD_REQUEST, "存在不属于该日期的单词");
        }

        learningRecordMapper.update(null, Wrappers.<LearningRecord>lambdaUpdate()
                .eq(LearningRecord::getUserId, userId)
                .eq(LearningRecord::getLearnDate, date)
                .set(LearningRecord::getIsWrong, Boolean.FALSE));

        if (!wrong.isEmpty()) {
            learningRecordMapper.update(null, Wrappers.<LearningRecord>lambdaUpdate()
                    .eq(LearningRecord::getUserId, userId)
                    .eq(LearningRecord::getLearnDate, date)
                    .in(LearningRecord::getWordId, wrong)
                    .set(LearningRecord::getIsWrong, Boolean.TRUE));
        }

        return new ReviewOutcome(records.size(), wrong.size());
    }

    public record ReviewOutcome(int total, int wrong) {
    }

    /** 全部学习记录，按「词 ASC、日期 DESC、id DESC」排，方便取每个词的最新一条。 */
    public List<LearningRecord> findByUserOrdered(Long userId) {
        return learningRecordMapper.selectList(Wrappers.<LearningRecord>lambdaQuery()
                .eq(LearningRecord::getUserId, userId)
                .orderByAsc(LearningRecord::getWordId)
                .orderByDesc(LearningRecord::getLearnDate)
                .orderByDesc(LearningRecord::getId));
    }

    /** 某个词最近的一条学习记录。 */
    public LearningRecord findLatestForWord(Long userId, Long wordId) {
        List<LearningRecord> records = learningRecordMapper.selectList(Wrappers.<LearningRecord>lambdaQuery()
                .eq(LearningRecord::getUserId, userId)
                .eq(LearningRecord::getWordId, wordId)
                .orderByDesc(LearningRecord::getLearnDate)
                .orderByDesc(LearningRecord::getId)
                .last("LIMIT 1"));
        return records.isEmpty() ? null : records.get(0);
    }

    /** 把某条记录标记为「已掌握」（is_wrong=0）。 */
    public int clearWrong(Long recordId) {
        return learningRecordMapper.update(null, Wrappers.<LearningRecord>lambdaUpdate()
                .eq(LearningRecord::getId, recordId)
                .set(LearningRecord::getIsWrong, Boolean.FALSE));
    }
}
