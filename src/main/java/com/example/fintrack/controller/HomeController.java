package com.example.fintrack.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * トップページから資産一覧へのリダイレクトを扱うコントローラー。
 *
 * @author kanta ito
 */
@Controller
public class HomeController {
    
    /**
     * トップページへのアクセスを資産一覧へリダイレクトする。
     *
     * @return 資産一覧へのリダイレクト先
     */
    @GetMapping("/")
    public String home() {
        return "redirect:/assets";
    }
}

