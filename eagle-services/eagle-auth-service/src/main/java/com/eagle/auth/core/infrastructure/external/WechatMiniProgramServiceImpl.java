package com.eagle.auth.core.infrastructure.external;

import cn.binarywang.wx.miniapp.api.WxMaService;
import cn.binarywang.wx.miniapp.bean.WxMaJscode2SessionResult;
import com.eagle.auth.core.domain.AuthErrorCode;
import com.eagle.auth.core.domain.service.WechatService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.chanjar.weixin.common.error.WxErrorException;
import org.springframework.stereotype.Service;

/**
 * 微信小程序服务实现
 * <p>
 * 基于 WxJava（Binary Wang）调用微信小程序 API
 *
 * @author sunshixiong
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WechatMiniProgramServiceImpl implements WechatService {

    private final WxMaService wxMaService;

    @Override
    public WechatUserInfo getUserInfo(String code) {
        try {
            WxMaJscode2SessionResult session = wxMaService.jsCode2SessionInfo(code);
            return new WechatUserInfo(
                    session.getOpenid(),
                    session.getUnionid(),
                    session.getSessionKey()
            );
        } catch (WxErrorException e) {
            throw AuthErrorCode.WECHAT_LOGIN_FAILED.toServiceException(e);
        }
    }
}
