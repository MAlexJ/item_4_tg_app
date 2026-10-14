package com.malexj.telegram;

import com.malexj.configuration.properties.TelegramProperties;
import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.request.GetMe;
import com.pengrad.telegrambot.response.GetMeResponse;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/** Resolves the bot's @username via {@code getMe} so we can build deep links. */
@Slf4j
@Component
@RequiredArgsConstructor
public class TelegramBotInfoService {

  private final TelegramBot telegramBot;
  private final TelegramProperties properties;

  private volatile String username;

  @EventListener(ApplicationReadyEvent.class)
  public void resolveUsername() {
    if (!isTokenConfigured()) {
      return;
    }
    try {
      GetMeResponse response = telegramBot.execute(new GetMe());
      if (response.isOk() && response.user() != null && response.user().username() != null) {
        username = response.user().username();
        log.info("Resolved Telegram bot username: @{}", username);
      } else {
        log.warn(
            "Could not resolve bot username: code={} description={}",
            response.errorCode(),
            response.description());
      }
    } catch (Exception e) {
      log.warn("Failed to resolve bot username: {}", e.getMessage());
    }
  }

  public Optional<String> username() {
    return Optional.ofNullable(username);
  }

  private boolean isTokenConfigured() {
    String token = properties.getBot().getToken();
    return token != null && !token.isBlank() && !"TOKEN".equals(token.trim());
  }
}
