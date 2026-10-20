package com.wordrecall.dto;

import java.util.List;

/**
 * @param words         this batch, in wordbook sort order; empty means the current wordbook has no unlearned words
 * @param todayWordCount how many words the user has learned today (all batches)
 * @param remaining     unlearned words left in the current wordbook after this batch
 */
public record StudyDto(List<WordDto> words, int todayWordCount, int remaining) {
}
