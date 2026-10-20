package com.wordrecall.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("wordbook")
public class Wordbook {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String name;
    private String description;

    /** BUILTIN 或 USER */
    private String source;

    /** 空 = 预置词库；非空 = 该用户自建 */
    private Long ownerId;

    private LocalDateTime createdAt;
}
