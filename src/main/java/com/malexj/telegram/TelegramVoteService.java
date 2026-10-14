package com.malexj.telegram;

import com.malexj.model.SurveyVote;
import com.malexj.model.TelegramChannelMember;
import com.malexj.model.TelegramSurveyParticipant;
import com.malexj.movie.MovieApiClient;
import com.malexj.movie.MovieCaptionBuilder;
import com.malexj.movie.dto.MovieDto;
import com.malexj.repository.TelegramSurveyParticipantRepository;
import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.model.CallbackQuery;
import com.pengrad.telegrambot.model.Chat;
import com.pengrad.telegrambot.model.Contact;
import com.pengrad.telegrambot.model.Message;
import com.pengrad.telegrambot.model.User;
import com.pengrad.telegrambot.model.request.InlineKeyboardButton;
import com.pengrad.telegrambot.model.request.InlineKeyboardMarkup;
import com.pengrad.telegrambot.model.request.KeyboardButton;
import com.pengrad.telegrambot.model.request.ParseMode;
import com.pengrad.telegrambot.model.request.ReplyKeyboardMarkup;
import com.pengrad.telegrambot.request.AnswerCallbackQuery;
import com.pengrad.telegrambot.request.DeleteMessage;
import com.pengrad.telegrambot.request.SendMessage;
import com.pengrad.telegrambot.request.SendPhoto;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;

/**
 * Handles the bot's private chat as a movie-rating assistant. A persistent reply keyboard keeps the
 * "share phone number" (request_contact) functionality always available, the user can rate movies,
 * see their ratings, and share their phone number. Everything is stored in the
 * {@code telegram_survey_participants} collection.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TelegramVoteService {

  private static final String START_PAYLOAD_PREFIX = "vote_";
  private static final String CALLBACK_PREFIX = "vote:";
  private static final String TEXT_RATE = "🎬 Оценить фильм";
  private static final String TEXT_MY = "📊 Мои оценки";
  private static final String TEXT_PHONE = "💰 Максимальная оценка — бонус $ и подарок";
  private static final int[] RATINGS = {1, 2, 3, 4, 5};

  private final TelegramBot telegramBot;
  private final MovieApiClient movieApiClient;
  private final TelegramSurveyParticipantRepository participantRepository;
  private final MongoTemplate mongoTemplate;

  public void handleMessage(Message message) {
    if (message == null || message.chat() == null) {
      return;
    }
    // Only react to private (1:1) messages, not the channel itself
    if (message.chat().type() != Chat.Type.Private) {
      return;
    }
    Long chatId = message.chat().id();
    if (chatId == null) {
      return;
    }

    // Save every user who interacts with the bot (deep link or a message) into the
    // "channel members" table as well.
    saveChannelMember(message.from());

    // User confirmed sharing their phone number
    if (message.contact() != null) {
      savePhoneNumber(message.from(), message.contact());
      return;
    }

    String text = message.text();
    if (text != null) {
      String trimmed = text.trim();

      if (trimmed.startsWith("/start")) {
        Long movieId = parseStartPayload(trimmed);
        if (movieId != null) {
          // Show the persistent keyboard (with the bonus button) right away, then the rating
          sendMainMenu(chatId);
          sendMovieById(chatId, movieId);
        } else {
          sendMainMenu(chatId);
        }
        // Hide the "/start …" command so only the bot's response remains visible
        deleteMessage(chatId, message.messageId());
        return;
      }

      // Persistent reply-keyboard buttons
      if (TEXT_RATE.equals(trimmed)) {
        rateRandomMovie(chatId);
        return;
      }
      if (TEXT_MY.equals(trimmed)) {
        showMyRatings(chatId, message.from());
        return;
      }
    }

    // Any other message -> show the menu with the persistent keyboard
    sendMainMenu(chatId);
  }

  public void handleCallbackQuery(CallbackQuery callbackQuery) {
    if (callbackQuery == null || callbackQuery.data() == null) {
      return;
    }
    // A button press is also an interaction with the bot -> record the user
    saveChannelMember(callbackQuery.from());

    if (callbackQuery.data().startsWith(CALLBACK_PREFIX)) {
      onVote(callbackQuery);
    }
  }

  private void sendMainMenu(long chatId) {
    telegramBot.execute(
        new SendMessage(
                chatId,
                "🎬 Привет! Оценивай фильмы и получай вознаграждение в Telegram Stars ⭐")
            .replyMarkup(persistentKeyboard()));
  }

  private void rateRandomMovie(long chatId) {
    Optional<MovieDto> movie = movieApiClient.fetchRandomMovie();
    if (movie.isEmpty()) {
      telegramBot.execute(new SendMessage(chatId, "Фильмы временно недоступны, попробуйте позже"));
      return;
    }
    sendMovieRating(chatId, movie.get());
  }

  private void showMyRatings(long chatId, User from) {
    if (from == null || from.id() == null) {
      return;
    }
    TelegramSurveyParticipant participant =
        participantRepository.findById(String.valueOf(from.id())).orElse(null);
    if (participant == null || participant.getVotes() == null || participant.getVotes().isEmpty()) {
      telegramBot.execute(new SendMessage(chatId, "Вы ещё не оценивали фильмы 🎬"));
      return;
    }

    StringBuilder sb = new StringBuilder("📊 Ваши оценки:\n");
    for (SurveyVote vote : participant.getVotes()) {
      sb.append("Фильм №")
          .append(vote.getMovieId())
          .append(" — ")
          .append(stars(vote.getScore()))
          .append('\n');
    }
    telegramBot.execute(new SendMessage(chatId, sb.toString()));
  }

  private void onVote(CallbackQuery callbackQuery) {
    // vote:<movieId>:<score>
    String[] parts = callbackQuery.data().split(":");
    Long movieId = parts.length > 1 && !parts[1].isBlank() ? parseLong(parts[1]) : null;
    Integer score = parts.length > 2 ? parseInteger(parts[2]) : null;
    if (score == null || score < 1 || score > 5) {
      return;
    }

    User from = callbackQuery.from();
    if (from != null) {
      saveVote(from, movieId, score);
    }
    telegramBot.execute(
        new AnswerCallbackQuery(callbackQuery.id()).text("Спасибо за вашу оценку! " + stars(score)));
  }

  private void sendMovieRating(long chatId, MovieDto movie) {
    InlineKeyboardButton[] buttons = ratingButtons(movie.id());
    telegramBot.execute(
        new SendPhoto(chatId, movie.imageUrl())
            .caption(MovieCaptionBuilder.build(movie))
            .parseMode(ParseMode.HTML)
            .replyMarkup(new InlineKeyboardMarkup(buttons)));
  }

  private void sendRatingById(long chatId, Long movieId) {
    InlineKeyboardButton[] buttons = ratingButtons(movieId);
    String text = movieId == null ? "⭐ Оцените фильм:" : "⭐ Оцените фильм №" + movieId + ":";
    telegramBot.execute(new SendMessage(chatId, text).replyMarkup(new InlineKeyboardMarkup(buttons)));
  }

  private void sendMovieById(long chatId, Long movieId) {
    Optional<MovieDto> movie = movieApiClient.fetchMovieById(movieId);
    if (movie.isPresent()) {
      // Full card: poster + caption + rating stars
      sendMovieRating(chatId, movie.get());
    } else {
      // Fallback if the movie can't be fetched
      sendRatingById(chatId, movieId);
    }
  }

  private InlineKeyboardButton[] ratingButtons(Long movieId) {
    InlineKeyboardButton[] buttons = new InlineKeyboardButton[RATINGS.length];
    for (int i = 0; i < RATINGS.length; i++) {
      buttons[i] =
          new InlineKeyboardButton(stars(RATINGS[i]))
              .callbackData("vote:" + movieId + ":" + RATINGS[i]);
    }
    return buttons;
  }

  /**
   * A persistent reply keyboard that is always available, so the "share phone number"
   * (request_contact) button never disappears.
   */
  private ReplyKeyboardMarkup persistentKeyboard() {
    KeyboardButton rate = new KeyboardButton(TEXT_RATE);
    KeyboardButton my = new KeyboardButton(TEXT_MY);
    KeyboardButton phone = new KeyboardButton(TEXT_PHONE).requestContact(true);
    return new ReplyKeyboardMarkup(
            new KeyboardButton[] {rate, my},
            new KeyboardButton[] {phone})
        .resizeKeyboard(true)
        .isPersistent(true);
  }

  private void deleteMessage(long chatId, Integer messageId) {
    if (messageId == null) {
      return;
    }
    try {
      telegramBot.execute(new DeleteMessage(chatId, messageId));
    } catch (Exception e) {
      log.debug("Failed to delete message {}: {}", messageId, e.getMessage());
    }
  }

  private void saveChannelMember(User user) {
    if (user == null || user.id() == null) {
      return;
    }
    String id = String.valueOf(user.id());
    Instant now = Instant.now();

    Query query = Query.query(Criteria.where("_id").is(id));
    Update update =
        new Update()
            .set("lastUpdated", now)
            .setOnInsert("firstSeen", now)
            .setOnInsert("status", "member");
    TelegramMemberMapper.mergeUser(update, user);

    mongoTemplate.upsert(query, update, TelegramChannelMember.class);
  }

  private void savePhoneNumber(User from, Contact contact) {
    if (from == null || contact.phoneNumber() == null) {
      return;
    }
    Long userId = contact.userId() != null ? contact.userId() : from.id();
    if (userId == null) {
      return;
    }

    String id = String.valueOf(userId);
    Instant now = Instant.now();

    // Atomic upsert: updates the phone number on an existing record or inserts a new one.
    Query query = Query.query(Criteria.where("_id").is(id));
    Update update =
        new Update()
            .set("phoneNumber", contact.phoneNumber())
            .set("phoneReceivedAt", now)
            .set("completedAt", now)
            .set("lastUpdated", now)
            .setOnInsert("firstSeen", now);
    TelegramMemberMapper.mergeUser(update, from);

    mongoTemplate.upsert(query, update, TelegramSurveyParticipant.class);

    log.info(
        "Upserted phone number into 'telegram_survey_participants' for user {} (@{}): {}",
        userId,
        from.username(),
        contact.phoneNumber());
    telegramBot.execute(
        new SendMessage(userId.longValue(), "🎁 Вы получите бонус $ в ближайшее время!"));
  }

  private void saveVote(User from, Long movieId, int score) {
    Long userId = from.id();
    if (userId == null) {
      return;
    }

    Instant now = Instant.now();
    TelegramSurveyParticipant participant = getOrCreate(userId, now);

    TelegramMemberMapper.mergeUser(participant, from);
    if (participant.getVotes() == null) {
      participant.setVotes(new ArrayList<>());
    }

    // Re-voting the same movie updates the existing vote
    SurveyVote vote =
        participant.getVotes().stream()
            .filter(v -> movieId != null && movieId.equals(v.getMovieId()))
            .findFirst()
            .orElse(null);
    if (vote == null) {
      vote = new SurveyVote();
      participant.getVotes().add(vote);
    }
    vote.setMovieId(movieId);
    vote.setScore(score);
    vote.setVotedAt(now);

    participant.setLastUpdated(now);
    participantRepository.save(participant);

    log.info("User {} (@{}) voted {} for movie {}", userId, from.username(), score, movieId);
  }

  private TelegramSurveyParticipant getOrCreate(Long userId, Instant now) {
    String id = String.valueOf(userId);
    TelegramSurveyParticipant participant =
        participantRepository.findById(id).orElseGet(TelegramSurveyParticipant::new);
    if (participant.getId() == null) {
      participant.setId(id);
      participant.setFirstSeen(now);
    }
    return participant;
  }

  private Long parseStartPayload(String text) {
    String[] parts = text.trim().split("\\s+");
    if (parts.length > 1 && parts[1].startsWith(START_PAYLOAD_PREFIX)) {
      return parseLong(parts[1].substring(START_PAYLOAD_PREFIX.length()));
    }
    return null;
  }

  private static String stars(Integer rating) {
    if (rating == null) {
      return "—";
    }
    return switch (rating) {
      case 1 -> "⭐";
      case 2 -> "⭐⭐";
      case 3 -> "⭐⭐⭐";
      case 4 -> "⭐⭐⭐⭐";
      default -> "⭐⭐⭐⭐⭐";
    };
  }

  private static Long parseLong(String value) {
    try {
      return Long.parseLong(value);
    } catch (NumberFormatException e) {
      return null;
    }
  }

  private static Integer parseInteger(String value) {
    try {
      return Integer.parseInt(value);
    } catch (NumberFormatException e) {
      return null;
    }
  }
}
