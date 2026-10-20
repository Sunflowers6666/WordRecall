package com.wordrecall.service;

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import java.io.ByteArrayOutputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SheetParserTest {

    private final SheetParser parser = new SheetParser();

    private MockMultipartFile csv(String text, Charset charset) {
        return new MockMultipartFile("file", "words.csv", "text/csv",
                text.getBytes(charset));
    }

    @Test
    void parsesCsvWithChineseHeader() throws Exception {
        List<SheetParser.ParsedRow> rows = parser.parse(csv("单词,释义,音标\napple,苹果,/ˈæpl/\nbanana,香蕉,\n",
                StandardCharsets.UTF_8));

        assertThat(rows).hasSize(2);
        assertThat(rows.get(0).spelling()).isEqualTo("apple");
        assertThat(rows.get(0).meaning()).isEqualTo("苹果");
        assertThat(rows.get(0).phonetic()).isEqualTo("/ˈæpl/");
        assertThat(rows.get(0).ok()).isTrue();
        assertThat(rows.get(1).row()).isEqualTo(3);
    }

    @Test
    void parsesCsvWithoutHeaderAsWordMeaningOrder() throws Exception {
        List<SheetParser.ParsedRow> rows = parser.parse(csv("apple,苹果\nbanana,香蕉\n",
                StandardCharsets.UTF_8));

        assertThat(rows).hasSize(2);
        assertThat(rows.get(0).spelling()).isEqualTo("apple");
        assertThat(rows.get(0).meaning()).isEqualTo("苹果");
    }

    @Test
    void parsesEnglishHeaderAndTabDelimiter() throws Exception {
        List<SheetParser.ParsedRow> rows = parser.parse(csv("word\tmeaning\napple\t苹果\n",
                StandardCharsets.UTF_8));

        assertThat(rows).hasSize(1);
        assertThat(rows.get(0).spelling()).isEqualTo("apple");
    }

    @Test
    void decodesGbkCsv() throws Exception {
        List<SheetParser.ParsedRow> rows = parser.parse(csv("单词,释义\napple,苹果\n",
                Charset.forName("GBK")));

        assertThat(rows).hasSize(1);
        assertThat(rows.get(0).meaning()).isEqualTo("苹果");
    }

    @Test
    void marksRowWithoutMeaningInvalid() throws Exception {
        List<SheetParser.ParsedRow> rows = parser.parse(csv("单词,释义\napple,\n",
                StandardCharsets.UTF_8));

        assertThat(rows).hasSize(1);
        assertThat(rows.get(0).ok()).isFalse();
        assertThat(rows.get(0).reason()).isEqualTo("缺少释义");
    }

    @Test
    void skipsBlankRows() throws Exception {
        List<SheetParser.ParsedRow> rows = parser.parse(csv("单词,释义\napple,苹果\n\n",
                StandardCharsets.UTF_8));

        assertThat(rows).hasSize(1);
    }

    @Test
    void parsesXlsx() throws Exception {
        byte[] bytes;
        try (XSSFWorkbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("words");
            Row header = sheet.createRow(0);
            header.createCell(0).setCellValue("单词");
            header.createCell(1).setCellValue("释义");
            Row first = sheet.createRow(1);
            first.createCell(0).setCellValue("apple");
            first.createCell(1).setCellValue("苹果");
            Row second = sheet.createRow(2);
            second.createCell(0).setCellValue("banana");
            second.createCell(1).setCellValue("香蕉");
            workbook.write(out);
            bytes = out.toByteArray();
        }

        MockMultipartFile file = new MockMultipartFile("file", "words.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", bytes);
        List<SheetParser.ParsedRow> rows = parser.parse(file);

        assertThat(rows).hasSize(2);
        assertThat(rows.get(1).spelling()).isEqualTo("banana");
        assertThat(rows.get(1).meaning()).isEqualTo("香蕉");
    }

    @Test
    void rejectsEmptyFile() {
        MockMultipartFile file = new MockMultipartFile("file", "words.csv", "text/csv", new byte[0]);

        assertThatThrownBy(() -> parser.parse(file))
                .hasMessageContaining("文件为空");
    }
}
