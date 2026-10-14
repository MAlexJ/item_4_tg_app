package com.malexj.admin;

import com.malexj.model.TelegramWebUser;
import com.malexj.model.WebLogin;
import com.malexj.repository.TelegramWebUserRepository;
import com.malexj.repository.WebLoginRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Read-only test API for inspecting login records. No authentication is applied yet — add a guard
 * here before exposing this in production.
 */
@Slf4j
@RestController
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminController {

  private final WebLoginRepository webLoginRepository;
  private final TelegramWebUserRepository telegramWebUserRepository;

  /** Users who logged in with a phone number ({@code method = "phone"}), newest first. */
  @GetMapping("/phone")
  public List<WebLogin> phoneLogins() {
    return webLoginRepository.findByMethodOrderByCreatedAtDesc("phone");
  }

  /** Users who logged in through the Telegram Login Widget, most recent login first. */
  @GetMapping("/telegram")
  public List<TelegramWebUser> telegramUsers() {
    return telegramWebUserRepository.findAllByOrderByLastLoginAtDesc();
  }
}
