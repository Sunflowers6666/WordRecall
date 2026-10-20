package com.wordrecall.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.wordrecall.common.BizException;
import com.wordrecall.common.ErrorCode;
import com.wordrecall.domain.User;
import com.wordrecall.domain.Wordbook;
import com.wordrecall.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    /** 允许的每批新学词数。 */
    private static final Set<Integer> BATCH_SIZES = Set.of(10, 20, 30);

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

    public User getById(Long userId) {
        User user = userId == null ? null : userMapper.selectById(userId);
        if (user == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "用户不存在");
        }
        return user;
    }

    /**
     * 只写真正变化的列，{@code updated_at} 仍由 ON UPDATE CURRENT_TIMESTAMP 刷新。
     * 昵称与每批词数都可单独更新，但不能两个都不传。
     */
    public User updateProfile(Long userId, String nickname, Integer batchSize) {
        boolean hasNickname = nickname != null;
        boolean hasBatchSize = batchSize != null;
        if (!hasNickname && !hasBatchSize) {
            throw new BizException(ErrorCode.BAD_REQUEST, "没有要更新的内容");
        }
        if (hasBatchSize && !BATCH_SIZES.contains(batchSize)) {
            throw new BizException(ErrorCode.BAD_REQUEST, "每批词数只能是 10 / 20 / 30");
        }

        User user = getById(userId);
        var update = Wrappers.<User>lambdaUpdate().eq(User::getId, userId);
        if (hasNickname) {
            update.set(User::getNickname, nickname);
        }
        if (hasBatchSize) {
            update.set(User::getBatchSize, batchSize);
        }
        userMapper.update(null, update);

        if (hasNickname) {
            user.setNickname(nickname);
        }
        if (hasBatchSize) {
            user.setBatchSize(batchSize);
        }
        return user;
    }

    public User updateCurrentWordbook(Long userId, Long wordbookId) {
        User user = getById(userId);
        if (wordbookService.findById(wordbookId) == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "词库不存在");
        }
        userMapper.update(null, Wrappers.<User>lambdaUpdate()
                .eq(User::getId, userId)
                .set(User::getCurrentWordbookId, wordbookId));
        user.setCurrentWordbookId(wordbookId);
        return user;
    }
}
