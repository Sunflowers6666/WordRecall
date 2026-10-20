package com.wordrecall.dto;

import java.util.List;

public record ImportPreviewDto(String fileName,
                               int total,
                               int valid,
                               int invalid,
                               List<ImportRowDto> rows) {
}
