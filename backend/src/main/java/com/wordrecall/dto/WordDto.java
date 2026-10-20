package com.wordrecall.dto;

import com.wordrecall.domain.Word;

/**
 * A word as shown in the mini program. {@code isWrong} is only meaningful for
 * review results; new-word batches always report {@code false}.
 */
public record WordDto(Long id,
                      String spelling,
                      String phonetic,
                      String meaning,
                      String exampleSentence,
                      String exampleTranslation,
                      boolean isWrong) {

    public static WordDto from(Word word, boolean isWrong) {
        return new WordDto(word.getId(), word.getSpelling(), word.getPhonetic(), word.getMeaning(),
                word.getExampleSentence(), word.getExampleTranslation(), isWrong);
    }
}
