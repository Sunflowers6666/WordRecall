package com.wordrecall.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.wordrecall.domain.Word;
import com.wordrecall.domain.Wordbook;
import com.wordrecall.dto.DictImportResult;
import com.wordrecall.dto.DictWord;
import com.wordrecall.dto.DictWordbookFile;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class DictImportService {

    private static final List<String> RESOURCES = List.of(
            "dict/cet4.json",
            "dict/cet6.json",
            "dict/ielts.json"
    );

    private final ObjectMapper objectMapper;
    private final WordbookService wordbookService;
    private final WordService wordService;

    @Transactional
    public void importIfEmpty() throws IOException {
        long existing = wordService.count();
        if (existing > 0) {
            log.info("word table is not empty ({} rows), skip dictionary import", existing);
            return;
        }

        for (String resource : RESOURCES) {
            try (InputStream input = new ClassPathResource(resource).getInputStream()) {
                DictWordbookFile file = objectMapper.readValue(input, DictWordbookFile.class);
                importFile(file);
            } catch (FileNotFoundException exception) {
                log.warn("dictionary resource is missing: {}", resource);
            }
        }
    }

    @Transactional
    public DictImportResult importFile(DictWordbookFile file) {
        if (file == null || !StringUtils.hasText(file.wordbook())
                || file.words() == null || file.words().isEmpty()) {
            return new DictImportResult(file == null ? "unknown" : file.wordbook(), 0, true);
        }

        Wordbook wordbook = wordbookService.findByName(file.wordbook());
        if (wordbook == null) {
            wordbook = new Wordbook();
            wordbook.setName(file.wordbook());
            wordbook.setDescription(file.description());
            wordbook.setSource(StringUtils.hasText(file.source()) ? file.source() : "BUILTIN");
            if (!wordbookService.save(wordbook)) {
                throw new IllegalStateException("failed to create wordbook: " + file.wordbook());
            }
        }

        long existing = wordService.count(
                Wrappers.<Word>lambdaQuery().eq(Word::getWordbookId, wordbook.getId()));
        if (existing > 0) {
            log.info("wordbook {} already has {} words, skip", file.wordbook(), existing);
            return new DictImportResult(file.wordbook(), 0, true);
        }

        List<Word> words = new ArrayList<>();
        int fallbackOrder = 1;
        for (DictWord item : file.words()) {
            if (!StringUtils.hasText(item.spelling()) || !StringUtils.hasText(item.meaning())) {
                continue;
            }
            Word word = new Word();
            word.setWordbookId(wordbook.getId());
            word.setSpelling(item.spelling().trim());
            word.setPhonetic(item.phonetic());
            word.setMeaning(item.meaning());
            word.setExampleSentence(item.exampleSentence());
            word.setExampleTranslation(item.exampleTranslation());
            word.setSortOrder(item.sortOrder() == null ? fallbackOrder : item.sortOrder());
            words.add(word);
            fallbackOrder++;
        }

        if (!words.isEmpty() && !wordService.saveBatch(words, 500)) {
            throw new IllegalStateException("failed to save words for " + file.wordbook());
        }
        log.info("imported wordbook {}: {} words", file.wordbook(), words.size());
        return new DictImportResult(file.wordbook(), words.size(), false);
    }
}
