package com.wordrecall.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.wordrecall.domain.Word;
import com.wordrecall.domain.Wordbook;
import com.wordrecall.mapper.WordbookMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class WordbookService {

    private final WordbookMapper wordbookMapper;
    private final WordService wordService;

    public Wordbook findByName(String name) {
        return wordbookMapper.selectOne(
                Wrappers.<Wordbook>lambdaQuery().eq(Wordbook::getName, name));
    }

    public Wordbook findById(Long id) {
        return id == null ? null : wordbookMapper.selectById(id);
    }

    /** Built-in wordbooks in creation order. */
    public List<Wordbook> listAll() {
        return wordbookMapper.selectList(
                Wrappers.<Wordbook>lambdaQuery().orderByAsc(Wordbook::getId));
    }

    public long countWords(Long wordbookId) {
        return wordService.count(Wrappers.<Word>lambdaQuery()
                .eq(Word::getWordbookId, wordbookId));
    }

    public boolean save(Wordbook wordbook) {
        return wordbookMapper.insert(wordbook) == 1;
    }
}
