package com.malexj.model;

import java.time.Instant;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 * A user who signed in on the web through the Telegram Login Widget. Kept in a dedicated collection
 * (separate from the bot's channel members and survey participants).
 */
@Getter
@Setter
@Document(collection = "telegram_web_users")
public class TelegramWebUser {

  /** Telegram user id, used as the document key (idempotent upsert). */
  @Id private String id;

  private Long userId;

  // --- Profile returned by the Login Widget ---
  private String firstName;
  private String lastName;
  private String username;
  private String photoUrl;

  /** Telegram epoch seconds of the last authorization. */
  private Integer authDate;

  // --- Statistics ---
  private Long loginCount;
  private Instant firstSeen;
  private Instant lastLoginAt;
  private Instant lastUpdated;
}
