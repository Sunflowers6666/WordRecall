package com.wordrecall.dto;

public record DictWord(
        String spelling,
        String phonetic,
        String meaning,
        String exampleSentence,
        String exampleTranslation,
        Integer sortOrder
) {
}
