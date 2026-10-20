package com.wordrecall.dto;

public record DictImportResult(String wordbook, int imported, boolean skipped) {
}
