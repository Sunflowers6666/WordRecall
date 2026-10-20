package com.wordrecall.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.wordrecall.domain.User;
import com.wordrecall.domain.Wordbook;
import com.wordrecall.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserMapper userMapper;
    private final WordbookService wordbookService;

    @Transactional
    public User findOrCreateByOpenid(String openid) {
        User existing = userMapper.selectOne(
                Wrappers.<User>lambdaQuery().eq(User::getOpenid, openid));
        if (existing != null) {
            return existing;
        }

        User user = new User();
        user.setOpenid(openid);
        Wordbook defaultWordbook = wordbookService.findByName("CET-4");
        if (defaultWordbook != null) {
            user.setCurrentWordbookId(defaultWordbook.getId());
        }

        try {
            userMapper.insert(user);
            return user;
        } catch (DuplicateKeyException exception) {
            log.info("concurrent user creation detected, loading existing user");
            User created = userMapper.selectOne(
                    Wrappers.<User>lambdaQuery().eq(User::getOpenid, openid));
            if (created == null) {
                throw exception;
            }
            return created;
        }
    }
}
