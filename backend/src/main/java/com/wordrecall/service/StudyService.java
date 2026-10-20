package com.wordrecall.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.wordrecall.common.BizException;
import com.wordrecall.common.ErrorCode;
import com.wordrecall.domain.User;
import com.wordrecall.domain.Word;
import com.wordrecall.dto.StudyDto;
import com.wordrecall.dto.StudyRecordDto;
import com.wordrecall.dto.WordDto;
import com.wordrecall.mapper.WordMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * New-word batches. A batch is only a preview: a word becomes "learned today"
 * when the client reports that the user actually moved past it.
 */
@Service
@RequiredArgsConstructor
public class StudyService {

    private static final int MAX_BATCH = 50;

    private final UserService userService;
    private final WordService wordService;
    private final WordMapper wordMapper;
    private final LearningRecordService learningRecordService;

    /**
     * Peek at the next unlearned batch. Records nothing, so opening and leaving the
     * page does not pollute the calendar.
     */
    public StudyDto startSession(Long userId, int size, LocalDate today) {
        int limit = Math.max(1, Math.min(size, MAX_BATCH));

        User user = userService.getById(userId);
        Long wordbookId = requireCurrentWordbook(user);

        Set<Long> learned = learningRecordService.findLearnedWordIds(userId, wordbookId);
        List<Word> batch = new ArrayList<>(fetchCandidates(userId, wordbookId, limit));
        // Belt and braces: the query already excludes learned words, this filter also
        // covers a record inserted between the two statements.
        batch.removeIf(word -> learned.contains(word.getId()));
        if (batch.size() > limit) {
            batch = batch.subList(0, limit);
        }

        long totalInWordbook = wordService.count(Wrappers.<Word>lambdaQuery()
                .eq(Word::getWordbookId, wordbookId));
        int remaining = (int) Math.max(0, totalInWordbook - learned.size() - batch.size());

        List<WordDto> words = batch.stream().map(word -> WordDto.from(word, false)).toList();
        int todayWordCount = (int) learningRecordService.countByUserAndDate(userId, today);
        return new StudyDto(words, todayWordCount, remaining);
    }

    /**
     * Record the words the user has moved past. Idempotent: the same word on the same
     * day inserts nothing the second time.
     */
    @Transactional
    public StudyRecordDto recordWords(Long userId, List<Long> wordIds, LocalDate today) {
        User user = userService.getById(userId);
        Long wordbookId = requireCurrentWordbook(user);

        if (wordIds == null || wordIds.isEmpty()) {
            throw new BizException(ErrorCode.BAD_REQUEST, "wordIds 不能为空");
        }
        Set<Long> distinctIds = new LinkedHashSet<>(wordIds);
        if (distinctIds.size() > MAX_BATCH) {
            throw new BizException(ErrorCode.BAD_REQUEST, "一次最多记录 " + MAX_BATCH + " 个单词");
        }

        List<Word> words = wordService.findByIds(distinctIds);
        if (words.size() != distinctIds.size()) {
            throw new BizException(ErrorCode.NOT_FOUND, "单词不存在");
        }
        for (Word word : words) {
            if (!wordbookId.equals(word.getWordbookId())) {
                throw new BizException(ErrorCode.BAD_REQUEST, "单词不属于当前词库");
            }
        }

        int recorded = learningRecordService.recordStudy(userId, wordbookId, distinctIds, today);
        int todayWordCount = (int) learningRecordService.countByUserAndDate(userId, today);
        return new StudyRecordDto(recorded, todayWordCount);
    }

    private Long requireCurrentWordbook(User user) {
        Long wordbookId = user.getCurrentWordbookId();
        if (wordbookId == null) {
            throw new BizException(ErrorCode.BAD_REQUEST, "请先选择词库");
        }
        return wordbookId;
    }

    /**
     * Unlearned words of the wordbook in sort order. Both ids are Longs taken from the
     * JWT and the database, so string concatenation cannot carry user input.
     */
    private List<Word> fetchCandidates(Long userId, Long wordbookId, int limit) {
        String notLearned = "NOT EXISTS (SELECT 1 FROM learning_record r"
                + " WHERE r.user_id = " + userId
                + " AND r.wordbook_id = " + wordbookId
                + " AND r.word_id = word.id)";
        return wordMapper.selectList(Wrappers.<Word>lambdaQuery()
                .eq(Word::getWordbookId, wordbookId)
                .apply(notLearned)
                .orderByAsc(Word::getSortOrder)
                .orderByAsc(Word::getId)
                .last("LIMIT " + limit));
    }
}
