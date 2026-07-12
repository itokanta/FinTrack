package com.example.fintrack.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import jakarta.servlet.http.HttpServletRequest;

@ControllerAdvice
public class GlobalExceptionHandler {

  private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

  /** アップロードサイズ超過：ユーザー起因の想定内エラーなので WARN */
  @ExceptionHandler(MaxUploadSizeExceededException.class)
  @ResponseStatus(HttpStatus.PAYLOAD_TOO_LARGE)
  public String handleMaxUploadSize(MaxUploadSizeExceededException e, HttpServletRequest request, Model model) {
    log.warn("アップロードサイズ超過 uri={}", request.getRequestURI());
    model.addAttribute("error", "ファイルサイズが大きすぎます。");

    return "error";
  }

  /** 静的リソース未検出（favicon.ico 等）：想定内の 404 なので stack trace なしで DEBUG */
  @ExceptionHandler(NoResourceFoundException.class)
  @ResponseStatus(HttpStatus.NOT_FOUND)
  public String handleNoResourceFound(NoResourceFoundException e, HttpServletRequest request, Model model) {
    log.debug("静的リソース未検出 uri={}", request.getRequestURI());

    return "error";
  }

  /** 想定外の例外：原因調査のため stack trace 付きで ERROR */
  @ExceptionHandler(Exception.class)
  @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
  public String handleUnexpected(Exception e, HttpServletRequest request, Model model) {
    log.error("予期しないエラー method={} uri={}", request.getMethod(), request.getRequestURI(), e);
    model.addAttribute("error", "システムエラーが発生しました。時間をおいて再度お試しください。");

    return "error";
  }
}
