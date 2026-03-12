package com.eleganteer.system.system.interfaces.rest;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * 登录接口
 *
 * @author 孙士雄（sunshix@seeyon.com）
 * 2025/11/20-14:25
 */
@Controller
@RequestMapping(value = "login")
@RequiredArgsConstructor
public class LoginController {


    @GetMapping
    public String login() {
        return "login";
    }
}