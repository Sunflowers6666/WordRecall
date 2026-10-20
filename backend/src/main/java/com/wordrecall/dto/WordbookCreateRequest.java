package com.wordrecall.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record WordbookCreateRequest(@NotBlank(message = "词库名称不能为空")
                                    @Size(max = 50, message = "词库名称最多 50 个字符")
                                    String name,
                                    @Size(max = 500, message = "描述最多 500 个字符")
                                    String description) {
}
