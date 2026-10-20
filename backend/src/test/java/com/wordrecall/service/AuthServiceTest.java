package com.wordrecall.service;

import cn.binarywang.wx.miniapp.api.WxMaService;
import cn.binarywang.wx.miniapp.api.WxMaUserService;
import cn.binarywang.wx.miniapp.bean.WxMaJscode2SessionResult;
import com.wordrecall.domain.User;
import com.wordrecall.dto.LoginResponse;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AuthServiceTest {

    private static final String SECRET = "0123456789abcdef0123456789abcdef";

    @Test
    void logsInExistingOrNewUserAndIssuesJwt() throws Exception {
        WxMaService wxMaService = mock(WxMaService.class);
        WxMaUserService wxMaUserService = mock(WxMaUserService.class);
        when(wxMaService.getUserService()).thenReturn(wxMaUserService);

        WxMaJscode2SessionResult session = new WxMaJscode2SessionResult();
        session.setOpenid("openid-1");
        when(wxMaUserService.getSessionInfo("code-1")).thenReturn(session);

        User user = new User();
        user.setId(7L);
        UserService userService = mock(UserService.class);
        when(userService.findOrCreateByOpenid("openid-1")).thenReturn(user);

        JwtService jwtService = new JwtService(SECRET, 7);
        AuthService authService = new AuthService(wxMaService, userService, jwtService);

        LoginResponse response = authService.login("code-1");

        assertThat(response.token()).isNotBlank();
        assertThat(jwtService.parseUserId(response.token())).isEqualTo(7L);
        assertThat(response.user().id()).isEqualTo(7L);
    }
}
