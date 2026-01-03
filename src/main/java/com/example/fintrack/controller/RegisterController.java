package com.example.fintrack.controller;

import com.example.fintrack.service.UserService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * ユーザー登録コントローラー
 */
@Controller
public class RegisterController {
    
    private final UserService userService;
    
    public RegisterController(UserService userService) {
        this.userService = userService;
    }
    
    /**
     * ユーザー登録画面
     */
    @GetMapping("/register")
    public String registerForm(Model model) {
        return "register";
    }
    
    /**
     * ユーザー登録処理
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
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/register";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "ユーザー登録に失敗しました: " + e.getMessage());
            return "redirect:/register";
        }
    }
}

