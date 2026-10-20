package com.wordrecall.dto;

import java.util.List;

public record ImportResultDto(Long bookId,
                              String bookName,
                              int total,
                              int imported,
                              int skippedDuplicate,
                              int skippedInvalid,
                              List<ImportRowDto> problems) {
}
