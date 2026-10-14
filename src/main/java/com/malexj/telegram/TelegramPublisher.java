package com.malexj.telegram;

import com.malexj.configuration.properties.TelegramProperties;
import com.malexj.movie.MovieCaptionBuilder;
import com.malexj.movie.dto.MovieDto;
import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.model.request.InlineKeyboardButton;
import com.pengrad.telegrambot.model.request.InlineKeyboardMarkup;
import com.pengrad.telegrambot.model.request.ParseMode;
import com.pengrad.telegrambot.request.SendPhoto;
import com.pengrad.telegrambot.response.SendResponse;
import java.util.concurrent.atomic.AtomicBoolean;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/** Sends a movie as a photo (imageUrl) with a rich caption to the Telegram channel. */
@Slf4j
@Service
@RequiredArgsConstructor
public class TelegramPublisher {

  private final TelegramBot telegramBot;
  private final TelegramProperties properties;
  private final TelegramBotInfoService botInfo;
  private final AtomicBoolean misconfiguredLogged = new AtomicBoolean(false);

  /** True when both TELEGRAM_BOT_TOKEN and TELEGRAM_CHANNEL_ID are set. */
  public boolean isConfigured() {
    String token = properties.getBot().getToken();
    String channelId = properties.getBot().getChannelId();
    boolean configured =
        isFilled(token, "TOKEN") && isFilled(channelId, "ID");

    if (!configured && misconfiguredLogged.compareAndSet(false, true)) {
      log.warn(
          "Telegram bot is not configured. Set TELEGRAM_BOT_TOKEN and TELEGRAM_CHANNEL_ID "
              + "in .env — movie posting is disabled until then.");
    }
    return configured;
  }

  public void sendMovie(MovieDto movie) {
    if (!isConfigured()) {
      return;
    }

    String deepLink = movieDeepLink(movie);
    SendPhoto request = buildSendPhoto(movie, MovieCaptionBuilder.build(movie, deepLink), deepLink);
    try {
      SendResponse response = telegramBot.execute(request);
      if (response.isOk()) {
        log.info(
            "Posted movie '{}' (id={}) to channel {}",
            movie.title(),
            movie.id(),
            properties.getBot().getChannelId());
      } else {
        log.error(
            "Telegram rejected photo for movie '{}': code={} description={}",
            movie.title(),
            response.errorCode(),
            response.description());
      }
    } catch (Exception e) {
      log.error("Failed to send movie '{}' to Telegram: {}", movie.title(), e.getMessage(), e);
    }
  }

  private String movieDeepLink(MovieDto movie) {
    if (movie.id() == null) {
      return null;
    }
    return botInfo.username()
        .map(name -> "https://t.me/" + name + "?start=vote_" + movie.id())
        .orElse(null);
  }

  private SendPhoto buildSendPhoto(MovieDto movie, String caption, String deepLink) {
    String channelId = properties.getBot().getChannelId();
    SendPhoto request;
    if (channelId != null && channelId.matches("-?\\d+")) {
      request = new SendPhoto(Long.parseLong(channelId), movie.imageUrl());
    } else {
      // support @username-style channel reference
      request = new SendPhoto(channelId, movie.imageUrl());
    }
    request = request.caption(caption).parseMode(ParseMode.HTML);

    // Attach a "rate" button that deep-links to the bot (only when the username is resolved)
    if (deepLink != null) {
      InlineKeyboardMarkup keyboard =
          new InlineKeyboardMarkup(
              new InlineKeyboardButton("⭐ Оцените фильм и получите бонус $").url(deepLink));
      request.replyMarkup(keyboard);
    }
    return request;
  }

  private static boolean isFilled(String value, String placeholder) {
    return value != null && !value.isBlank() && !placeholder.equals(value.trim());
  }
}
