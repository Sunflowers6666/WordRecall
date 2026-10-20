package com.wordrecall.service;

import cn.binarywang.wx.miniapp.api.WxMaService;
import cn.binarywang.wx.miniapp.bean.WxMaJscode2SessionResult;
import com.wordrecall.common.BizException;
import com.wordrecall.common.ErrorCode;
import com.wordrecall.domain.User;
import com.wordrecall.dto.LoginResponse;
import com.wordrecall.dto.UserDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.chanjar.weixin.common.error.WxErrorException;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final WxMaService wxMaService;
    private final UserService userService;
    private final JwtService jwtService;

    public LoginResponse login(String code) {
        try {
            WxMaJscode2SessionResult session = wxMaService.getUserService().getSessionInfo(code);
            String openid = session == null ? null : session.getOpenid();
            if (!StringUtils.hasText(openid)) {
                throw new BizException(ErrorCode.WX_LOGIN_FAILED, "微信登录失败：未获取到 openid");
            }

            User user = userService.findOrCreateByOpenid(openid);
            String token = jwtService.createToken(user.getId());
            return new LoginResponse(token, UserDto.from(user));
        } catch (WxErrorException exception) {
            int errorCode = exception.getError() == null ? -1 : exception.getError().getErrorCode();
            log.warn("wx login failed, errorCode={}", errorCode);
            throw new BizException(ErrorCode.WX_LOGIN_FAILED, "微信登录失败，请重试");
        }
    }
}
