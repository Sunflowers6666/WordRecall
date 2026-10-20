package com.wordrecall.service;

import com.wordrecall.common.BizException;
import com.wordrecall.common.ErrorCode;
import com.wordrecall.domain.User;
import com.wordrecall.domain.Wordbook;
import com.wordrecall.mapper.UserMapper;
import com.wordrecall.support.MybatisTestSupport;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class UserServiceTest {

    @BeforeAll
    static void initMybatisMetadata() {
        MybatisTestSupport.initAll();
    }

    private final UserMapper userMapper = mock(UserMapper.class);
    private final WordbookService wordbookService = mock(WordbookService.class);
    private final UserService userService = new UserService(userMapper, wordbookService);

    @Test
    void updateNicknameWritesOnlyTheNicknameColumn() {
        when(userMapper.selectById(1L)).thenReturn(existingUser());
        when(userMapper.update(isNull(), any())).thenReturn(1);

        User updated = userService.updateNickname(1L, "小明");

        assertThat(updated.getNickname()).isEqualTo("小明");
        assertThat(updated.getCurrentWordbookId()).isEqualTo(3L);
        verify(userMapper).update(isNull(), any());
    }

    @Test
    void switchingToUnknownWordbookIsNotFound() {
        when(userMapper.selectById(1L)).thenReturn(existingUser());
        when(wordbookService.findById(99L)).thenReturn(null);

        assertThatThrownBy(() -> userService.updateCurrentWordbook(1L, 99L))
                .isInstanceOfSatisfying(BizException.class,
                        exception -> assertThat(exception.getCode()).isEqualTo(ErrorCode.NOT_FOUND));
        verify(userMapper, never()).update(isNull(), any());
    }

    @Test
    void switchingWordbookUpdatesTheUser() {
        when(userMapper.selectById(1L)).thenReturn(existingUser());
        Wordbook wordbook = new Wordbook();
        wordbook.setId(8L);
        when(wordbookService.findById(8L)).thenReturn(wordbook);
        when(userMapper.update(isNull(), any())).thenReturn(1);

        User updated = userService.updateCurrentWordbook(1L, 8L);

        assertThat(updated.getCurrentWordbookId()).isEqualTo(8L);
        verify(userMapper).update(isNull(), any());
    }

    @Test
    void unknownUserIsNotFound() {
        when(userMapper.selectById(42L)).thenReturn(null);

        assertThatThrownBy(() -> userService.getById(42L))
                .isInstanceOfSatisfying(BizException.class,
                        exception -> assertThat(exception.getCode()).isEqualTo(ErrorCode.NOT_FOUND));
    }

    private static User existingUser() {
        User user = new User();
        user.setId(1L);
        user.setOpenid("openid-1");
        user.setCurrentWordbookId(3L);
        return user;
    }
}
