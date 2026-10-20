package com.wordrecall.service;

import com.wordrecall.domain.LearningRecord;
import com.wordrecall.domain.Word;
import com.wordrecall.dto.WordDto;
import com.wordrecall.dto.WrongbookDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 错词本：以每个词「最近一条」学习记录的 is_wrong 为准，跨天汇总。
 */
@Service
@RequiredArgsConstructor
public class WrongbookService {

    private final LearningRecordService learningRecordService;
    private final WordService wordService;

    public WrongbookDto list(Long userId, int offset, int limit) {
        Map<Long, Boolean> latestWrong = new LinkedHashMap<>();
        for (LearningRecord record : learningRecordService.findByUserOrdered(userId)) {
            latestWrong.computeIfAbsent(record.getWordId(),
                    key -> Boolean.TRUE.equals(record.getIsWrong()));
        }

        List<Long> wrongWordIds = latestWrong.entrySet().stream()
                .filter(entry -> Boolean.TRUE.equals(entry.getValue()))
                .map(Map.Entry::getKey)
                .toList();

        int safeLimit = Math.max(1, Math.min(limit, 100));
        int safeOffset = Math.max(0, offset);
        List<Long> pageIds = wrongWordIds.stream().skip(safeOffset).limit(safeLimit).toList();
        if (pageIds.isEmpty()) {
            return new WrongbookDto(wrongWordIds.size(), List.of());
        }

        Map<Long, Word> words = wordService.findByIds(pageIds).stream()
                .collect(Collectors.toMap(Word::getId, Function.identity(), (left, right) -> left));
        List<WordDto> dtos = new ArrayList<>();
        for (Long wordId : pageIds) {
            Word word = words.get(wordId);
            if (word != null) {
                dtos.add(WordDto.from(word, true));
            }
        }
        return new WrongbookDto(wrongWordIds.size(), dtos);
    }

    /** 复习完成：把「记住了」的词从错词本移除（只动最新一条记录）。 */
    @Transactional
    public int submit(Long userId, List<Long> rememberedWordIds) {
        int cleared = 0;
        for (Long wordId : new LinkedHashSet<>(rememberedWordIds)) {
            LearningRecord latest = learningRecordService.findLatestForWord(userId, wordId);
            if (latest == null || !Boolean.TRUE.equals(latest.getIsWrong())) {
                continue;
            }
            cleared += learningRecordService.clearWrong(latest.getId());
        }
        return cleared;
    }
}
