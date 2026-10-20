package com.wordrecall.dto;

import java.util.List;

public record WrongbookDto(long total, List<WordDto> words) {
}
