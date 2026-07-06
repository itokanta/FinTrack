package com.example.fintrack.security;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import com.example.fintrack.service.UserService;

/**
 * ログイン中の {@link Authentication} からユーザー ID を引くヘルパー。
 *
 * @author kanta ito
 */
@Component
public class AuthenticationUserIdResolver {
  private final UserService userService;

  public AuthenticationUserIdResolver(UserService userService) {
    this.userService = userService;
  }

  /**
   * 認証情報のユーザー名から DB 上のユーザー ID を取得する。
   *
   * @param authentication 現在の認証情報
   * @return ユーザー ID
   */
  public Long getUserId(Authentication authentication) {
    String username = authentication.getName();
    return userService.findUserIdByUsername(username);
  }
}
