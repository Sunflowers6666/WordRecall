package com.wordrecall.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("word")
public class Word {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long wordbookId;
    private String spelling;
    private String phonetic;
    private String meaning;
    private String exampleSentence;
    private String exampleTranslation;
    private Integer sortOrder;
}
