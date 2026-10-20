package com.wordrecall.service;

import com.wordrecall.common.BizException;
import com.wordrecall.common.ErrorCode;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.StringReader;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * 词表解析：CSV（UTF-8/GBK、逗号/分号/制表符）与 xlsx（第一个工作表）。
 * 表头可选；无表头时按「单词, 释义, 音标, 例句, 例句翻译」的顺序取列。
 */
@Component
public class SheetParser {

    private static final int MAX_COLUMNS = 8;

    /** 表头别名（统一小写、去空格/下划线后匹配）。 */
    private static final Map<String, String> HEADER_ALIASES = new HashMap<>();

    static {
        alias("spelling", "word", "spelling", "单词", "词汇", "英文", "english");
        alias("phonetic", "phonetic", "ipa", "音标");
        alias("meaning", "meaning", "definition", "释义", "中文", "翻译", "意思", "词义");
        alias("exampleSentence", "example", "sentence", "例句", "句子");
        alias("exampleTranslation", "exampletranslation", "例句翻译", "例句中文", "翻译例句");
    }

    private static void alias(String target, String... names) {
        for (String name : names) {
            HEADER_ALIASES.put(normalizeHeader(name), target);
        }
    }

    public record ParsedRow(int row,
                            String spelling,
                            String phonetic,
                            String meaning,
                            String exampleSentence,
                            String exampleTranslation,
                            boolean ok,
                            String reason) {
    }

    public List<ParsedRow> parse(MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new BizException(ErrorCode.BAD_REQUEST, "文件为空");
        }
        String name = file.getOriginalFilename() == null ? "" : file.getOriginalFilename().toLowerCase(Locale.ROOT);
        byte[] bytes = file.getBytes();
        List<List<String>> raw = name.endsWith(".xlsx") || name.endsWith(".xls")
                ? readWorkbook(bytes)
                : readCsv(bytes);
        return mapRows(raw);
    }

    private List<List<String>> readCsv(byte[] bytes) throws IOException {
        String text = decode(bytes);
        char delimiter = detectDelimiter(text);
        CSVFormat format = CSVFormat.DEFAULT.builder()
                .setDelimiter(delimiter)
                .setIgnoreEmptyLines(false)
                .build();
        List<List<String>> rows = new ArrayList<>();
        Iterable<CSVRecord> records = format.parse(new StringReader(text));
        for (CSVRecord record : records) {
            List<String> cells = new ArrayList<>();
            for (int index = 0; index < Math.min(record.size(), MAX_COLUMNS); index++) {
                cells.add(record.get(index));
            }
            rows.add(cells);
        }
        return rows;
    }

    private List<List<String>> readWorkbook(byte[] bytes) throws IOException {
        List<List<String>> rows = new ArrayList<>();
        DataFormatter formatter = new DataFormatter();
        try (Workbook workbook = WorkbookFactory.create(new ByteArrayInputStream(bytes))) {
            Sheet sheet = workbook.getSheetAt(0);
            for (Row row : sheet) {
                List<String> cells = new ArrayList<>();
                int last = Math.min(row.getLastCellNum(), MAX_COLUMNS);
                for (int index = 0; index < last; index++) {
                    Cell cell = row.getCell(index);
                    cells.add(cell == null ? "" : formatter.formatCellValue(cell).trim());
                }
                rows.add(cells);
            }
        }
        return rows;
    }

    private String decode(byte[] bytes) {
        int offset = 0;
        if (bytes.length >= 3 && (bytes[0] & 0xFF) == 0xEF && (bytes[1] & 0xFF) == 0xBB
                && (bytes[2] & 0xFF) == 0xBF) {
            offset = 3;
        }
        ByteBuffer buffer = ByteBuffer.wrap(bytes, offset, bytes.length - offset);
        try {
            return StandardCharsets.UTF_8.newDecoder()
                    .onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT)
                    .decode(buffer)
                    .toString();
        } catch (CharacterCodingException exception) {
            return new String(bytes, offset, bytes.length - offset, Charset.forName("GBK"));
        }
    }

    private char detectDelimiter(String text) {
        int lineEnd = text.indexOf('\n');
        String firstLine = lineEnd < 0 ? text : text.substring(0, lineEnd);
        int commas = count(firstLine, ',');
        int semicolons = count(firstLine, ';');
        int tabs = count(firstLine, '\t');
        if (tabs > commas && tabs >= semicolons) {
            return '\t';
        }
        if (semicolons > commas) {
            return ';';
        }
        return ',';
    }

    private int count(String text, char target) {
        int total = 0;
        for (int index = 0; index < text.length(); index++) {
            if (text.charAt(index) == target) {
                total++;
            }
        }
        return total;
    }

    private List<ParsedRow> mapRows(List<List<String>> raw) {
        List<List<String>> rows = new ArrayList<>();
        for (List<String> row : raw) {
            if (!isBlankRow(row)) {
                rows.add(row);
            }
        }
        List<ParsedRow> parsed = new ArrayList<>();
        if (rows.isEmpty()) {
            return parsed;
        }

        Map<String, Integer> columns = headerColumns(rows.get(0));
        int start = columns.isEmpty() ? 0 : 1;
        if (columns.isEmpty()) {
            columns = defaultColumns();
        }

        for (int index = start; index < rows.size(); index++) {
            List<String> row = rows.get(index);
            String spelling = cell(row, columns.get("spelling"));
            String meaning = cell(row, columns.get("meaning"));
            String phonetic = cell(row, columns.get("phonetic"));
            String exampleSentence = cell(row, columns.get("exampleSentence"));
            String exampleTranslation = cell(row, columns.get("exampleTranslation"));
            int displayRow = index + 1;

            boolean ok = true;
            String reason = "";
            if (spelling.isEmpty()) {
                ok = false;
                reason = "缺少单词";
            } else if (spelling.length() > 100) {
                ok = false;
                reason = "单词超过 100 字";
            } else if (meaning.isEmpty()) {
                ok = false;
                reason = "缺少释义";
            }

            parsed.add(new ParsedRow(displayRow, spelling, phonetic, meaning,
                    exampleSentence, exampleTranslation, ok, reason));
        }
        return parsed;
    }

    private Map<String, Integer> headerColumns(List<String> firstRow) {
        Map<String, Integer> columns = new HashMap<>();
        for (int index = 0; index < firstRow.size(); index++) {
            String target = HEADER_ALIASES.get(normalizeHeader(firstRow.get(index)));
            if (target != null && !columns.containsKey(target)) {
                columns.put(target, index);
            }
        }
        return columns;
    }

    private Map<String, Integer> defaultColumns() {
        Map<String, Integer> columns = new HashMap<>();
        columns.put("spelling", 0);
        columns.put("meaning", 1);
        columns.put("phonetic", 2);
        columns.put("exampleSentence", 3);
        columns.put("exampleTranslation", 4);
        return columns;
    }

    private boolean isBlankRow(List<String> row) {
        for (String cell : row) {
            if (cell != null && !cell.trim().isEmpty()) {
                return false;
            }
        }
        return true;
    }

    private String cell(List<String> row, Integer index) {
        if (index == null || index < 0 || index >= row.size()) {
            return "";
        }
        String value = row.get(index);
        return value == null ? "" : value.trim();
    }

    private static String normalizeHeader(String value) {
        if (value == null) {
            return "";
        }
        return value.toLowerCase(Locale.ROOT).replace(" ", "").replace("_", "").replace("-", "");
    }
}
