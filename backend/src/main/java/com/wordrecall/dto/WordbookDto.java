package com.wordrecall.dto;

public record WordbookDto(Long id,
                          String name,
                          String description,
                          long wordCount,
                          boolean selected) {
}
