package com.wordrecall.dto;

import jakarta.validation.constraints.Size;

/** 两个字段都可选，至少传一个（服务层校验）。 */
public record WordbookUpdateRequest(@Size(max = 50, message = "词库名称最多 50 个字符")
                                    String name,
                                    @Size(max = 500, message = "描述最多 500 个字符")
                                    String description) {
}
