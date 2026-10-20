package com.wordrecall.dto;

import java.util.List;

public record WordbookWordsDto(long total, List<WordDto> words) {
}
