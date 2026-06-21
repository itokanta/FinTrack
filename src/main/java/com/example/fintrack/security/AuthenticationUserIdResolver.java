package com.example.fintrack.security;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import com.example.fintrack.service.UserService;

@Component
public class AuthenticationUserIdResolver {
  private final UserService userService;

  public AuthenticationUserIdResolver(UserService userService) {
    this.userService = userService;
  }

  public Long getUserId(Authentication authentication) {
    String username = authentication.getName();
    return userService.findUserIdByUsername(username);
  }
}
