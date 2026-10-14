package com.malexj.model;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 * A user who took part in the movie survey (voted and/or confirmed their phone number). Stored in a
 * dedicated collection, separate from the channel membership table.
 */
@Getter
@Setter
@Document(collection = "telegram_survey_participants")
public class TelegramSurveyParticipant {

  /** Telegram user id — one record per user. */
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

  // --- Phone number (survey confirmation step) ---
  private String phoneNumber;
  private Instant phoneReceivedAt;

  // --- Survey results ---
  private List<SurveyVote> votes = new ArrayList<>();
  private Instant completedAt; // set once the user confirms the phone number

  // --- Timestamps ---
  private Instant firstSeen;
  private Instant lastUpdated;
}
