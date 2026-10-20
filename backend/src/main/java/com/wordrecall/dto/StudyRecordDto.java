package com.wordrecall.dto;

/**
 * @param recorded      how many records this call actually inserted (duplicates skipped)
 * @param todayWordCount words learned today after this call
 */
public record StudyRecordDto(int recorded, int todayWordCount) {
}
