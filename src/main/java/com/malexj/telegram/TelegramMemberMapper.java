package com.malexj.telegram;

import com.malexj.model.TelegramChannelMember;
import com.malexj.model.TelegramSurveyParticipant;
import com.pengrad.telegrambot.model.Chat;
import com.pengrad.telegrambot.model.ChatMember;
import com.pengrad.telegrambot.model.ChatMemberUpdated;
import com.pengrad.telegrambot.model.User;
import org.springframework.data.mongodb.core.query.Update;

/** Copies every field Telegram exposes into the MongoDB documents. */
public final class TelegramMemberMapper {

  private TelegramMemberMapper() {}

  /** Copies the Telegram {@link User} profile fields into a channel-member document. */
  public static void mergeUser(TelegramChannelMember m, User user) {
    if (user == null) {
      return;
    }
    m.setUserId(user.id());
    m.setIsBot(user.isBot());
    m.setFirstName(user.firstName());
    m.setLastName(user.lastName());
    m.setUsername(user.username());
    m.setLanguageCode(user.languageCode());
    m.setIsPremium(user.isPremium());
    m.setAddedToAttachmentMenu(user.addedToAttachmentMenu());
    m.setCanJoinGroups(user.canJoinGroups());
    m.setCanReadAllGroupMessages(user.canReadAllGroupMessages());
    m.setSupportsInlineQueries(user.supportsInlineQueries());
    m.setCanConnectToBusiness(user.canConnectToBusiness());
    m.setHasMainWebApp(user.hasMainWebApp());
    m.setHasTopicsEnabled(user.hasTopicsEnabled());
    m.setAllowsUsersToCreateTopics(user.allowsUsersToCreateTopics());
    m.setCanManageBots(user.canManageBots());
    m.setSupportsGuestQueries(user.supportsGuestQueries());
    m.setSupportsJoinRequestQueries(user.supportsJoinRequestQueries());
  }

  /** Copies the Telegram {@link User} profile fields into a survey-participant document. */
  public static void mergeUser(TelegramSurveyParticipant p, User user) {
    if (user == null) {
      return;
    }
    p.setUserId(user.id());
    p.setIsBot(user.isBot());
    p.setFirstName(user.firstName());
    p.setLastName(user.lastName());
    p.setUsername(user.username());
    p.setLanguageCode(user.languageCode());
    p.setIsPremium(user.isPremium());
    p.setAddedToAttachmentMenu(user.addedToAttachmentMenu());
    p.setCanJoinGroups(user.canJoinGroups());
    p.setCanReadAllGroupMessages(user.canReadAllGroupMessages());
    p.setSupportsInlineQueries(user.supportsInlineQueries());
    p.setCanConnectToBusiness(user.canConnectToBusiness());
    p.setHasMainWebApp(user.hasMainWebApp());
    p.setHasTopicsEnabled(user.hasTopicsEnabled());
    p.setAllowsUsersToCreateTopics(user.allowsUsersToCreateTopics());
    p.setCanManageBots(user.canManageBots());
    p.setSupportsGuestQueries(user.supportsGuestQueries());
    p.setSupportsJoinRequestQueries(user.supportsJoinRequestQueries());
  }

  /** Adds the Telegram {@link User} profile fields to a MongoDB update operation. */
  public static void mergeUser(Update update, User user) {
    if (user == null) {
      return;
    }
    update.set("userId", user.id());
    update.set("isBot", user.isBot());
    update.set("firstName", user.firstName());
    update.set("lastName", user.lastName());
    update.set("username", user.username());
    update.set("languageCode", user.languageCode());
    update.set("isPremium", user.isPremium());
    update.set("addedToAttachmentMenu", user.addedToAttachmentMenu());
    update.set("canJoinGroups", user.canJoinGroups());
    update.set("canReadAllGroupMessages", user.canReadAllGroupMessages());
    update.set("supportsInlineQueries", user.supportsInlineQueries());
    update.set("canConnectToBusiness", user.canConnectToBusiness());
    update.set("hasMainWebApp", user.hasMainWebApp());
    update.set("hasTopicsEnabled", user.hasTopicsEnabled());
    update.set("allowsUsersToCreateTopics", user.allowsUsersToCreateTopics());
    update.set("canManageBots", user.canManageBots());
    update.set("supportsGuestQueries", user.supportsGuestQueries());
    update.set("supportsJoinRequestQueries", user.supportsJoinRequestQueries());
  }

  /** Copies user + membership + chat context into the channel-member document. */
  public static void apply(
      TelegramChannelMember m, User user, ChatMemberUpdated change, ChatMember.Status status) {

    mergeUser(m, user);

    // Membership
    m.setStatus(status != null ? status.name() : null);
    if (change.newChatMember() != null) {
      m.setCustomTitle(change.newChatMember().customTitle());
      m.setIsAnonymous(change.newChatMember().isAnonymous());
    }

    // Chat context
    Chat chat = change.chat();
    if (chat != null) {
      m.setChatId(chat.id());
      m.setChatTitle(chat.title());
      m.setChatType(chat.type() != null ? chat.type().name() : null);
      m.setChatUsername(chat.username());
    }

    // Join metadata
    m.setViaJoinRequest(change.viaJoinRequest());
    m.setViaChatFolderInviteLink(change.viaChatFolderInviteLink());
    m.setInviteLink(change.inviteLink() != null ? change.inviteLink().inviteLink() : null);

    // Telegram timestamps (epoch seconds)
    m.setJoinedAt(change.date());
    m.setLastSeenAt(change.date());
  }
}
