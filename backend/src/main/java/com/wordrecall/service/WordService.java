package com.wordrecall.service;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.wordrecall.domain.Word;
import com.wordrecall.mapper.WordMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

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
