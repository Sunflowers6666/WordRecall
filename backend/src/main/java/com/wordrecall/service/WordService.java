package com.wordrecall.service;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.wordrecall.common.BizException;
import com.wordrecall.common.ErrorCode;
import com.wordrecall.domain.Word;
import com.wordrecall.dto.WordItemRequest;
import com.wordrecall.mapper.WordMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.List;

@Service
@RequiredArgsConstructor
public class WordService {

    private final WordMapper wordMapper;

    public long count() {
        Long count = wordMapper.selectCount(null);
        return count == null ? 0 : count;
    }

    public long count(Wrapper<Word> wrapper) {
        Long count = wordMapper.selectCount(wrapper);
        return count == null ? 0 : count;
    }

    public List<Word> findByIds(Collection<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }
        return wordMapper.selectBatchIds(ids);
    }

    public Word requireById(Long id) {
        Word word = id == null ? null : wordMapper.selectById(id);
        if (word == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "单词不存在");
        }
        return word;
    }

    /** 自建词库的词条列表，按录入顺序。 */
    public List<Word> listByWordbook(Long wordbookId, int offset, int limit) {
        int safeLimit = Math.max(1, Math.min(limit, 200));
        int safeOffset = Math.max(0, offset);
        return wordMapper.selectList(Wrappers.<Word>lambdaQuery()
                .eq(Word::getWordbookId, wordbookId)
                .orderByAsc(Word::getSortOrder)
                .orderByAsc(Word::getId)
                .last("LIMIT " + safeLimit + " OFFSET " + safeOffset));
    }

    public long countInWordbook(Long wordbookId) {
        return count(Wrappers.<Word>lambdaQuery().eq(Word::getWordbookId, wordbookId));
    }

    public Word add(Long wordbookId, WordItemRequest request) {
        Word word = new Word();
        word.setWordbookId(wordbookId);
        word.setSpelling(request.spelling().trim());
        word.setPhonetic(trimToNull(request.phonetic()));
        word.setMeaning(request.meaning().trim());
        word.setExampleSentence(trimToNull(request.exampleSentence()));
        word.setExampleTranslation(trimToNull(request.exampleTranslation()));
        word.setSortOrder(nextSortOrder(wordbookId));
        try {
            wordMapper.insert(word);
        } catch (DuplicateKeyException exception) {
            throw new BizException(ErrorCode.CONFLICT, "这个单词已在该词库中");
        }
        return word;
    }

    public Word update(Long id, WordItemRequest request) {
        Word word = requireById(id);
        word.setSpelling(request.spelling().trim());
        word.setPhonetic(trimToNull(request.phonetic()));
        word.setMeaning(request.meaning().trim());
        word.setExampleSentence(trimToNull(request.exampleSentence()));
        word.setExampleTranslation(trimToNull(request.exampleTranslation()));
        try {
            wordMapper.updateById(word);
        } catch (DuplicateKeyException exception) {
            throw new BizException(ErrorCode.CONFLICT, "这个单词已在该词库中");
        }
        return word;
    }

    public void delete(Long id) {
        Word word = requireById(id);
        wordMapper.deleteById(word.getId());
    }

    private int nextSortOrder(Long wordbookId) {
        List<Word> last = wordMapper.selectList(Wrappers.<Word>lambdaQuery()
                .select(Word::getSortOrder)
                .eq(Word::getWordbookId, wordbookId)
                .orderByDesc(Word::getSortOrder)
                .last("LIMIT 1"));
        if (last.isEmpty() || last.get(0).getSortOrder() == null) {
            return 1;
        }
        return last.get(0).getSortOrder() + 1;
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    /**
     * Simple batch insert used by the one-time dictionary import.
     * The batch size parameter is kept for call-site compatibility.
     */
    public boolean saveBatch(List<Word> words, int batchSize) {
        for (Word word : words) {
            if (wordMapper.insert(word) != 1) {
                return false;
            }
        }
        return true;
    }
}
