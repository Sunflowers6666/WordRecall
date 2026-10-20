package com.wordrecall.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.wordrecall.common.BizException;
import com.wordrecall.common.ErrorCode;
import com.wordrecall.domain.Word;
import com.wordrecall.domain.Wordbook;
import com.wordrecall.mapper.WordbookMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class WordbookService {

    private static final String SOURCE_USER = "USER";

    private final WordbookMapper wordbookMapper;
    private final WordService wordService;

    public Wordbook findByName(String name) {
        return wordbookMapper.selectOne(
                Wrappers.<Wordbook>lambdaQuery().eq(Wordbook::getName, name));
    }

    public Wordbook findById(Long id) {
        return id == null ? null : wordbookMapper.selectById(id);
    }

    /** 预置词库 + 当前用户自建的词库。 */
    public List<Wordbook> listForUser(Long userId) {
        return wordbookMapper.selectList(Wrappers.<Wordbook>lambdaQuery()
                .and(query -> query.isNull(Wordbook::getOwnerId)
                        .or().eq(Wordbook::getOwnerId, userId))
                .orderByAsc(Wordbook::getId));
    }

    public List<Wordbook> listBuiltin() {
        return wordbookMapper.selectList(Wrappers.<Wordbook>lambdaQuery()
                .isNull(Wordbook::getOwnerId)
                .orderByAsc(Wordbook::getId));
    }

    public Wordbook create(Long userId, String name, String description) {
        String trimmed = name.trim();
        if (findByName(trimmed) != null) {
            throw new BizException(ErrorCode.CONFLICT, "词库名称已存在");
        }

        Wordbook wordbook = new Wordbook();
        wordbook.setName(trimmed);
        wordbook.setDescription(description);
        wordbook.setSource(SOURCE_USER);
        wordbook.setOwnerId(userId);
        try {
            wordbookMapper.insert(wordbook);
        } catch (DuplicateKeyException exception) {
            throw new BizException(ErrorCode.CONFLICT, "词库名称已存在");
        }
        return wordbook;
    }

    /** 改名/改描述，两个字段都可选，但不能都不传。 */
    public Wordbook update(Long userId, Long id, String name, String description) {
        Wordbook wordbook = requireOwned(userId, id);

        boolean hasName = name != null && !name.trim().isEmpty();
        boolean hasDescription = description != null;
        if (!hasName && !hasDescription) {
            throw new BizException(ErrorCode.BAD_REQUEST, "没有要更新的内容");
        }

        var update = Wrappers.<Wordbook>lambdaUpdate().eq(Wordbook::getId, id);
        if (hasName) {
            String trimmed = name.trim();
            Wordbook sameName = findByName(trimmed);
            if (sameName != null && !sameName.getId().equals(id)) {
                throw new BizException(ErrorCode.CONFLICT, "词库名称已存在");
            }
            update.set(Wordbook::getName, trimmed);
            wordbook.setName(trimmed);
        }
        if (hasDescription) {
            update.set(Wordbook::getDescription, description);
            wordbook.setDescription(description);
        }
        wordbookMapper.update(null, update);
        return wordbook;
    }

    /** 删除自建词库；词条与学习记录由外键级联删除。 */
    public void delete(Long userId, Long id) {
        Wordbook wordbook = requireOwned(userId, id);
        wordbookMapper.deleteById(wordbook.getId());
    }

    /** 只有本人能改；非本人或预置词库一律按不存在处理。 */
    public Wordbook requireOwned(Long userId, Long id) {
        Wordbook wordbook = findById(id);
        if (wordbook == null || wordbook.getOwnerId() == null
                || !wordbook.getOwnerId().equals(userId)) {
            throw new BizException(ErrorCode.NOT_FOUND, "词库不存在");
        }
        return wordbook;
    }

    public long countWords(Long wordbookId) {
        return wordService.count(Wrappers.<Word>lambdaQuery()
                .eq(Word::getWordbookId, wordbookId));
    }

    public boolean save(Wordbook wordbook) {
        return wordbookMapper.insert(wordbook) == 1;
    }
}
