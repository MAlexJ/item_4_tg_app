package com.malexj.model;

import java.time.Instant;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 * A user that joined (or left) the Telegram channel. Stores every field that Telegram exposes for
 * the user plus the channel membership context.
 */
@Getter
@Setter
@Document(collection = "telegram_channel_members")
public class TelegramChannelMember {

  /** Telegram user id, used as the document key (idempotent upsert). */
  @Id private String id;

  private Long userId;

  // --- Telegram User profile ---
  private Boolean isBot;
  private String firstName;
  private String lastName;
  private String username;
  private String languageCode;
  private Boolean isPremium;
  private Boolean addedToAttachmentMenu;
  private Boolean canJoinGroups;
  private Boolean canReadAllGroupMessages;
  private Boolean supportsInlineQueries;
  private Boolean canConnectToBusiness;
  private Boolean hasMainWebApp;
  private Boolean hasTopicsEnabled;
  private Boolean allowsUsersToCreateTopics;
  private Boolean canManageBots;
  private Boolean supportsGuestQueries;
  private Boolean supportsJoinRequestQueries;

  // --- Membership ---
  private String status; // member | left | kicked | restricted | administrator | creator
  private String customTitle;
  private Boolean isAnonymous;

  // --- Chat context ---
  private Long chatId;
  private String chatTitle;
  private String chatType; // private | group | supergroup | channel
  private String chatUsername;

  // --- Join metadata ---
  private Boolean viaJoinRequest;
  private Boolean viaChatFolderInviteLink;
  private String inviteLink;

  // --- Timestamps ---
  private Integer joinedAt; // Telegram epoch seconds of the last membership update
  private Integer lastSeenAt; // Telegram epoch seconds of the last membership update
  private Instant firstSeen; // when this bot first recorded the user
  private Instant lastUpdated; // when the record was last written
}
