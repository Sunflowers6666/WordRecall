package com.wordrecall.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("learning_record")
public class LearningRecord {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;
    private Long wordId;
    private Long wordbookId;
    private LocalDate learnDate;

    @TableField("is_wrong")
    private Boolean isWrong;

    private LocalDateTime createdAt;
}
