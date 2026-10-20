package com.wordrecall.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.wordrecall.common.BizException;
import com.wordrecall.common.ErrorCode;
import com.wordrecall.domain.User;
import com.wordrecall.domain.Word;
import com.wordrecall.dto.StudyDto;
import com.wordrecall.dto.WordDto;
import com.wordrecall.mapper.WordMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Builds one "new words" batch from the user's current wordbook and records it
 * as learned today.
 */
@Service
@RequiredArgsConstructor
public class StudyService {

    private static final int MAX_BATCH = 50;

    private final UserService userService;
    private final WordService wordService;
    private final WordMapper wordMapper;
    private final LearningRecordService learningRecordService;

    @Transactional
    public StudyDto startSession(Long userId, int size, LocalDate today) {
        int limit = Math.max(1, Math.min(size, MAX_BATCH));

        User user = userService.getById(userId);
        Long wordbookId = user.getCurrentWordbookId();
        if (wordbookId == null) {
            throw new BizException(ErrorCode.BAD_REQUEST, "请先选择词库");
        }

        Set<Long> learned = learningRecordService.findLearnedWordIds(userId, wordbookId);
        List<Word> batch = new ArrayList<>(fetchCandidates(userId, wordbookId, limit));
        // Belt and braces: the query already excludes learned words, this filter also
        // covers a record inserted between the two statements.
        batch.removeIf(word -> learned.contains(word.getId()));
        if (batch.size() > limit) {
            batch = batch.subList(0, limit);
        }

        List<Long> wordIds = batch.stream().map(Word::getId).toList();
        if (!wordIds.isEmpty()) {
            learningRecordService.recordStudy(userId, wordbookId, wordIds, today);
        }

        long totalInWordbook = wordService.count(Wrappers.<Word>lambdaQuery()
                .eq(Word::getWordbookId, wordbookId));
        int remaining = (int) Math.max(0, totalInWordbook - learned.size() - batch.size());

        List<WordDto> words = batch.stream().map(word -> WordDto.from(word, false)).toList();
        int todayWordCount = (int) learningRecordService.countByUserAndDate(userId, today);
        return new StudyDto(words, todayWordCount, remaining);
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
