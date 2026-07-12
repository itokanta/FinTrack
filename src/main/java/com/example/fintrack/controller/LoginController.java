package com.example.fintrack.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * ログイン画面の表示を扱うコントローラー。
 *
 * @author kanta ito
 */
@Controller
public class LoginController {
    
    /**
     * ログイン画面を表示する。
     *
     * @return ログインテンプレート名
     */
    @GetMapping("/login")
    public String login() {
        return "login";
    }
}

