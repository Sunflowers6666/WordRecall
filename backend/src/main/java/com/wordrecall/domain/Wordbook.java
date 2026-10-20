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
    private String source;
    private LocalDateTime createdAt;
}
