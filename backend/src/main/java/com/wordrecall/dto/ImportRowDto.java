package com.wordrecall.dto;

/** 导入预览行：ok=false 时 reason 说明原因。 */
public record ImportRowDto(int row,
                           String spelling,
                           String meaning,
                           boolean ok,
                           String reason) {
}
