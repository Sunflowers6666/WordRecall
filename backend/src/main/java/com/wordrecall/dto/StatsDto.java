package com.wordrecall.dto;

/**
 * @param learnedDays  days with at least one learning record
 * @param learnedWords distinct words ever learned
 * @param wrongWords   records currently marked wrong
 * @param streakDays   consecutive days ending today or yesterday
 * @param todayWords   words learned today
 */
public record StatsDto(int learnedDays,
                       int learnedWords,
                       int wrongWords,
                       int streakDays,
                       int todayWords) {
}
