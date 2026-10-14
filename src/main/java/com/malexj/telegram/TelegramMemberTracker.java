package com.malexj.telegram;

import com.malexj.configuration.properties.TelegramProperties;
import com.malexj.model.TelegramChannelMember;
import com.malexj.repository.TelegramChannelMemberRepository;
import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.UpdatesListener;
import com.pengrad.telegrambot.model.ChatMember;
import com.pengrad.telegrambot.model.ChatMemberUpdated;
import com.pengrad.telegrambot.model.Update;
import com.pengrad.telegrambot.model.User;
import com.pengrad.telegrambot.request.GetUpdates;
import jakarta.annotation.PreDestroy;
import java.time.Instant;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Long-polls Telegram updates and persists every user that joins (or leaves) the channel into
 * MongoDB. The bot must be an administrator of the channel to receive {@code chat_member} updates.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TelegramMemberTracker {

  private final TelegramBot telegramBot;
  private final TelegramProperties properties;
  private final TelegramChannelMemberRepository repository;
  private final TelegramVoteService voteService;

  @EventListener(ApplicationReadyEvent.class)
  public void startPolling() {
    String token = properties.getBot().getToken();
    if (token == null || token.isBlank() || "TOKEN".equals(token.trim())) {
      log.warn(
          "TELEGRAM_BOT_TOKEN is not configured — channel member tracking is disabled. "
              + "Set it in .env to start saving members.");
      return;
    }

    List<String> allowedUpdates = properties.getBot().getAllowedUpdates();
    GetUpdates request =
        new GetUpdates().timeout(50).allowedUpdates(allowedUpdates.toArray(new String[0]));
    telegramBot.setUpdatesListener(this::processUpdates, this::onException, request);
    log.info("Telegram long-polling started, allowed_updates={}", allowedUpdates);
  }

  @PreDestroy
  public void stopPolling() {
    try {
      telegramBot.removeGetUpdatesListener();
      log.info("Telegram long-polling stopped");
    } catch (Exception e) {
      log.debug("Error while stopping Telegram polling: {}", e.getMessage());
    }
  }

  private int processUpdates(List<Update> updates) {
    for (Update update : updates) {
      try {
        handle(update);
      } catch (Exception e) {
        log.error("Failed to process Telegram update {}: {}", update.updateId(), e.getMessage(), e);
      }
    }
    return UpdatesListener.CONFIRMED_UPDATES_ALL;
  }

  private void onException(Exception e) {
    log.error("Telegram polling error: {}", e.getMessage(), e);
  }

  private void handle(Update update) {
    if (update.chatMember() != null) {
      trackMemberChange(update.chatMember());
    }
    if (update.myChatMember() != null) {
      logBotStatusChange(update.myChatMember());
    }
    if (update.message() != null) {
      voteService.handleMessage(update.message());
    }
    if (update.callbackQuery() != null) {
      voteService.handleCallbackQuery(update.callbackQuery());
    }
  }

  private void trackMemberChange(ChatMemberUpdated change) {
    User user = change.newChatMember() != null ? change.newChatMember().user() : null;
    if (user == null) {
      user = change.from();
    }
    if (user == null) {
      log.debug("Ignoring chat_member update without a user");
      return;
    }

    ChatMember oldMember = change.oldChatMember();
    ChatMember newMember = change.newChatMember();
    ChatMember.Status newStatus = newMember != null ? newMember.status() : null;
    ChatMember.Status oldStatus = oldMember != null ? oldMember.status() : null;

    boolean joined =
        newStatus == ChatMember.Status.member
            && (oldStatus == null
                || oldStatus == ChatMember.Status.left
                || oldStatus == ChatMember.Status.kicked);
    boolean left =
        newStatus == ChatMember.Status.left || newStatus == ChatMember.Status.kicked;

    if (joined) {
      log.info(
          "User {} (@{}) joined channel {}",
          user.id(),
          user.username(),
          change.chat() != null ? change.chat().title() : "?");
    } else if (left) {
      log.info(
          "User {} (@{}) left channel {}",
          user.id(),
          user.username(),
          change.chat() != null ? change.chat().title() : "?");
    }

    upsert(user, change, newStatus);
  }

  private void logBotStatusChange(ChatMemberUpdated change) {
    ChatMember.Status status = change.newChatMember() != null ? change.newChatMember().status() : null;
    log.info(
        "Bot membership status changed in chat {} (id={}): {}",
        change.chat() != null ? change.chat().title() : "?",
        change.chat() != null ? change.chat().id() : "?",
        status);
  }

  private void upsert(User user, ChatMemberUpdated change, ChatMember.Status status) {
    String id = String.valueOf(user.id());
    Instant now = Instant.now();

    TelegramChannelMember doc = repository.findById(id).orElse(null);
    if (doc == null) {
      doc = new TelegramChannelMember();
      doc.setId(id);
      doc.setFirstSeen(now);
    }

    TelegramMemberMapper.apply(doc, user, change, status);
    doc.setLastUpdated(now);
    repository.save(doc);
  }
}
