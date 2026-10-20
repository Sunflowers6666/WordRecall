package com.wordrecall.service;

import com.wordrecall.common.BizException;
import com.wordrecall.common.ErrorCode;
import com.wordrecall.domain.Word;
import com.wordrecall.domain.Wordbook;
import com.wordrecall.dto.ImportPreviewDto;
import com.wordrecall.dto.ImportResultDto;
import com.wordrecall.dto.ImportRowDto;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/** 词表导入：解析 → 校验 → 写入自建词库。 */
@Service
@RequiredArgsConstructor
public class ImportService {

    private static final int MAX_ROWS = 5000;
    private static final int PREVIEW_LIMIT = 50;
    private static final int MAX_NAME_LENGTH = 50;

    private final SheetParser sheetParser;
    private final WordService wordService;
    private final WordbookService wordbookService;

    public ImportPreviewDto preview(MultipartFile file) throws IOException {
        List<SheetParser.ParsedRow> rows = parse(file);
        int valid = (int) rows.stream().filter(SheetParser.ParsedRow::ok).count();
        List<ImportRowDto> preview = rows.stream()
                .limit(PREVIEW_LIMIT)
                .map(row -> new ImportRowDto(row.row(), row.spelling(), row.meaning(), row.ok(), row.reason()))
                .toList();
        return new ImportPreviewDto(file.getOriginalFilename(), rows.size(), valid, rows.size() - valid, preview);
    }

    /**
     * 导入到已有词库（bookId）或新建词库（newBookName / 文件名）。
     * 缺单词或释义的行、文件内重复、库里已存在的词都会被跳过并逐行返回原因。
     */
    @Transactional
    public ImportResultDto importInto(Long userId, Long bookId, String newBookName, MultipartFile file)
            throws IOException {
        List<SheetParser.ParsedRow> rows = parse(file);

        Wordbook book = bookId != null
                ? wordbookService.requireOwned(userId, bookId)
                : wordbookService.create(userId, resolveName(newBookName, file), "从文件导入");

        int sortOrder = wordService.maxSortOrder(book.getId()) + 1;
        Set<String> seen = new HashSet<>();
        int imported = 0;
        int duplicate = 0;
        int invalid = 0;
        List<ImportRowDto> problems = new ArrayList<>();

        for (SheetParser.ParsedRow row : rows) {
            if (!row.ok()) {
                invalid++;
                problems.add(issue(row, row.reason()));
                continue;
            }
            String key = row.spelling().toLowerCase(Locale.ROOT);
            if (!seen.add(key)) {
                duplicate++;
                problems.add(issue(row, "文件内重复"));
                continue;
            }

            Word word = new Word();
            word.setWordbookId(book.getId());
            word.setSpelling(row.spelling());
            word.setPhonetic(emptyToNull(row.phonetic()));
            word.setMeaning(row.meaning());
            word.setExampleSentence(emptyToNull(row.exampleSentence()));
            word.setExampleTranslation(emptyToNull(row.exampleTranslation()));
            word.setSortOrder(sortOrder++);

            try {
                wordService.save(word);
                imported++;
            } catch (DuplicateKeyException exception) {
                duplicate++;
                problems.add(issue(row, "词库中已存在"));
            }
        }

        return new ImportResultDto(book.getId(), book.getName(), rows.size(), imported,
                duplicate, invalid, problems);
    }

    private List<SheetParser.ParsedRow> parse(MultipartFile file) throws IOException {
        List<SheetParser.ParsedRow> rows = sheetParser.parse(file);
        if (rows.isEmpty()) {
            throw new BizException(ErrorCode.BAD_REQUEST, "文件里没有可导入的行");
        }
        if (rows.size() > MAX_ROWS) {
            throw new BizException(ErrorCode.BAD_REQUEST, "单次最多导入 " + MAX_ROWS + " 行");
        }
        return rows;
    }

    private String resolveName(String explicit, MultipartFile file) {
        String name = explicit == null ? "" : explicit.trim();
        if (name.isEmpty()) {
            String fileName = file.getOriginalFilename() == null ? "" : file.getOriginalFilename();
            int dot = fileName.lastIndexOf('.');
            name = (dot > 0 ? fileName.substring(0, dot) : fileName).trim();
        }
        if (name.isEmpty()) {
            name = "导入词库";
        }
        return name.length() > MAX_NAME_LENGTH ? name.substring(0, MAX_NAME_LENGTH) : name;
    }

    private ImportRowDto issue(SheetParser.ParsedRow row, String reason) {
        return new ImportRowDto(row.row(), row.spelling(), row.meaning(), false, reason);
    }

    private String emptyToNull(String value) {
        return value == null || value.isEmpty() ? null : value;
    }
}
