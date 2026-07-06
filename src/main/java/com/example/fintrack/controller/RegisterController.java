package com.example.fintrack.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.example.fintrack.service.UserService;

/**
 * ユーザー登録画面と登録処理を扱うコントローラー。
 *
 * @author kanta ito
 */
@Controller
public class RegisterController {

    private static final Logger log = LoggerFactory.getLogger(RegisterController.class);

    private final UserService userService;
    
    public RegisterController(UserService userService) {
        this.userService = userService;
    }
    
    /**
     * ユーザー登録画面を表示する。
     *
     * @param model ビューモデル
     * @return ユーザー登録テンプレート名
     */
    @GetMapping("/register")
    public String registerForm(Model model) {
        return "register";
    }
    
    /**
     * ユーザーを新規登録する。バリデーションエラー時は登録画面へ戻す。
     *
     * @param username ユーザー名
     * @param password パスワード
     * @param passwordConfirm パスワード（確認用）
     * @param redirectAttributes リダイレクト先へのフラッシュ属性
     * @return ログイン画面または登録画面へのリダイレクト先
     */
    @PostMapping("/register")
    public String register(@RequestParam String username,
                          @RequestParam String password,
                          @RequestParam String passwordConfirm,
                          RedirectAttributes redirectAttributes) {
        try {
            // バリデーション
            if (username == null || username.trim().isEmpty()) {
                redirectAttributes.addFlashAttribute("error", "ユーザー名を入力してください");
                return "redirect:/register";
            }
            
            if (password == null || password.isEmpty()) {
                redirectAttributes.addFlashAttribute("error", "パスワードを入力してください");
                return "redirect:/register";
            }
            
            if (password.length() < 6) {
                redirectAttributes.addFlashAttribute("error", "パスワードは6文字以上で入力してください");
                return "redirect:/register";
            }
            
            if (!password.equals(passwordConfirm)) {
                redirectAttributes.addFlashAttribute("error", "パスワードが一致しません");
                return "redirect:/register";
            }
            
            // ユーザー作成
            userService.createUser(username.trim(), password);
            
            redirectAttributes.addFlashAttribute("message", "ユーザー登録が完了しました。ログインしてください。");
            return "redirect:/login";
            
        } catch (IllegalArgumentException e) {
            log.warn("ユーザー登録のバリデーションに失敗しました", e);
            redirectAttributes.addFlashAttribute("error", "ユーザー登録に失敗しました。入力内容を確認してください。");
            return "redirect:/register";
        } catch (Exception e) {
            log.error("ユーザー登録に失敗しました", e);
            redirectAttributes.addFlashAttribute("error", "ユーザー登録に失敗しました");
            return "redirect:/register";
        }
    }
}

