package com.wordrecall.dto;

import java.util.List;

public record DictWordbookFile(
        String wordbook,
        String description,
        String source,
        String license,
        String attribution,
        List<DictWord> words
) {
}
