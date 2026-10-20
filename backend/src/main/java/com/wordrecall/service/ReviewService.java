package com.wordrecall.service;

import com.wordrecall.domain.LearningRecord;
import com.wordrecall.domain.Word;
import com.wordrecall.dto.ReviewDto;
import com.wordrecall.dto.ReviewSubmitDto;
import com.wordrecall.dto.WordDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/** Review the words a user first learned on one specific date. */
@Service
@RequiredArgsConstructor
public class ReviewService {

    private final LearningRecordService learningRecordService;
    private final WordService wordService;

    public ReviewDto byDate(Long userId, LocalDate date) {
        List<LearningRecord> records = learningRecordService.findByUserAndDate(userId, date);
        if (records.isEmpty()) {
            return new ReviewDto(date.toString(), List.of());
        }

        List<Long> wordIds = records.stream().map(LearningRecord::getWordId).toList();
        Map<Long, Word> words = wordService.findByIds(wordIds).stream()
                .collect(Collectors.toMap(Word::getId, Function.identity(), (left, right) -> left));
        Map<Long, Boolean> wrongByWord = records.stream()
                .collect(Collectors.toMap(LearningRecord::getWordId,
                        record -> Boolean.TRUE.equals(record.getIsWrong()),
                        (left, right) -> left));

        List<WordDto> dtos = records.stream()
                .map(record -> words.get(record.getWordId()))
                .filter(Objects::nonNull)
                .map(word -> WordDto.from(word, wrongByWord.getOrDefault(word.getId(), false)))
                .toList();
        return new ReviewDto(date.toString(), dtos);
    }

    @Transactional
    public ReviewSubmitDto submit(Long userId, LocalDate date, List<Long> wrongWordIds) {
        Set<Long> wrong = wrongWordIds == null ? Set.of() : new LinkedHashSet<>(wrongWordIds);
        LearningRecordService.ReviewOutcome outcome = learningRecordService.submitReview(userId, date, wrong);
        return new ReviewSubmitDto(date.toString(), outcome.total(), outcome.wrong());
    }
}
