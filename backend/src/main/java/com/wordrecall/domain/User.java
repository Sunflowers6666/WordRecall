package com.wordrecall.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("user")
public class User {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String openid;
    private String nickname;
    private String avatarUrl;
    private Long currentWordbookId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
