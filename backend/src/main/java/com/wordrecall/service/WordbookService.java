package com.wordrecall.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.wordrecall.domain.Wordbook;
import com.wordrecall.mapper.WordbookMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class WordbookService {

    private final WordbookMapper wordbookMapper;

    public Wordbook findByName(String name) {
        return wordbookMapper.selectOne(
                Wrappers.<Wordbook>lambdaQuery().eq(Wordbook::getName, name));
    }

    public boolean save(Wordbook wordbook) {
        return wordbookMapper.insert(wordbook) == 1;
    }
}
