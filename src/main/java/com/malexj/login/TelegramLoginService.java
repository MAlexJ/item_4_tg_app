package com.malexj.login;

import com.malexj.configuration.properties.AppProperties;
import com.malexj.configuration.properties.TelegramProperties;
import com.malexj.model.TelegramWebUser;
import com.malexj.repository.TelegramWebUserRepository;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.util.UriUtils;

/**
 * Telegram Login Widget (OAuth) backend. Builds the {@code oauth.telegram.org} redirect URL,
 * verifies the returned {@code hash} with the bot token and persists the user in the dedicated
 * {@code telegram_web_users} collection.
 *
 * <p>Verification algorithm (official Telegram docs): {@code secret = SHA256(bot_token)},
 * {@code data_check_string} = all received fields except {@code hash}, sorted by key, joined as
 * {@code key=value} with newlines, then {@code hash = hex(HMAC_SHA256(secret, data_check_string))}.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TelegramLoginService {

  /** Maximum age of the Telegram {@code auth_date} we accept (replay protection). */
  private static final long MAX_AUTH_AGE_SECONDS = 24 * 60 * 60;

  /** The only fields Telegram signs in the Login Widget response (already sorted). */
  private static final List<String> AUTH_FIELDS =
      List.of("auth_date", "first_name", "id", "last_name", "photo_url", "username");

  private final TelegramProperties telegramProperties;
  private final AppProperties appProperties;
  private final TelegramWebUserRepository repository;

  /** True when a bot token is present and the Login Widget can be used. */
  public boolean isConfigured() {
    return botId() != null;
  }

  /** Builds the Telegram OAuth URL the browser should be redirected to. */
  public String buildAuthRedirectUrl(String returnUrl) {
    String origin = appProperties.getPublicUrl();
    String callback = origin + "/login/tg/callback?returnUrl=" + encode(returnUrl);

    return "https://oauth.telegram.org/auth"
        + "?bot_id="
        + encode(botId())
        + "&origin="
        + encode(origin)
        + "&request_access=write"
        + "&return_to="
        + encode(callback);
  }

  /**
   * Verifies the Login Widget signature and upserts the user. Throws {@link
   * ResponseStatusException} with a user-facing message when the data cannot be trusted.
   */
  public TelegramWebUser verifyAndSave(Map<String, String> params) {
    if (!verifyHash(params)) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Невірний підпис Telegram");
    }

    long authDate = parseLong(params.get("auth_date"));
    if (authDate <= 0 || Instant.now().getEpochSecond() - authDate > MAX_AUTH_AGE_SECONDS) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Сесія Telegram застаріла, увійдіть ще раз");
    }

    String id = params.get("id");
    if (id == null || id.isBlank()) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Telegram не повернув id користувача");
    }

    Instant now = Instant.now();
    TelegramWebUser user = repository.findById(id).orElse(null);
    if (user == null) {
      user = new TelegramWebUser();
      user.setId(id);
      user.setUserId(parseLong(id));
      user.setFirstSeen(now);
      user.setLoginCount(0L);
    }

    user.setFirstName(params.get("first_name"));
    user.setLastName(params.get("last_name"));
    user.setUsername(params.get("username"));
    user.setPhotoUrl(params.get("photo_url"));
    user.setAuthDate((int) authDate);
    user.setLoginCount((user.getLoginCount() == null ? 0L : user.getLoginCount()) + 1);
    user.setLastLoginAt(now);
    user.setLastUpdated(now);
    repository.save(user);

    log.info(
        "Telegram web login recorded: id={} @{} first_name={} loginCount={}",
        user.getUserId(),
        user.getUsername(),
        user.getFirstName(),
        user.getLoginCount());
    return user;
  }

  /** A human-friendly display name for the redirect-back URL. */
  public static String displayName(TelegramWebUser user) {
    if (user.getFirstName() != null && !user.getFirstName().isBlank()) {
      return user.getFirstName();
    }
    if (user.getUsername() != null && !user.getUsername().isBlank()) {
      return "@" + user.getUsername();
    }
    return "Telegram";
  }

  private boolean verifyHash(Map<String, String> params) {
    String hash = params.get("hash");
    if (hash == null || hash.isBlank()) {
      return false;
    }
    String botToken = telegramProperties.getBot().getToken();
    if (botToken == null || botToken.isBlank()) {
      return false;
    }

    // Build the check string only from the Telegram-signed fields; ignore our own params
    // (e.g. returnUrl) and the hash itself.
    String dataCheckString =
        AUTH_FIELDS.stream()
            .filter(field -> params.get(field) != null && !params.get(field).isBlank())
            .map(field -> field + "=" + params.get(field))
            .collect(Collectors.joining("\n"));

    byte[] secret = sha256(botToken.getBytes(StandardCharsets.UTF_8));
    byte[] expected = hmacSha256(secret, dataCheckString.getBytes(StandardCharsets.UTF_8));
    byte[] actual = hexToBytes(hash);
    return actual != null && MessageDigest.isEqual(expected, actual);
  }

  private String botId() {
    String token = telegramProperties.getBot().getToken();
    if (token == null || token.isBlank()) {
      return null;
    }
    int colon = token.indexOf(':');
    return colon > 0 ? token.substring(0, colon) : token;
  }

  private static String encode(String value) {
    return UriUtils.encodeQueryParam(value, StandardCharsets.UTF_8);
  }

  private static long parseLong(String value) {
    if (value == null) {
      return 0L;
    }
    try {
      return Long.parseLong(value);
    } catch (NumberFormatException e) {
      return 0L;
    }
  }

  private static byte[] sha256(byte[] data) {
    try {
      return MessageDigest.getInstance("SHA-256").digest(data);
    } catch (NoSuchAlgorithmException e) {
      throw new IllegalStateException("SHA-256 not available", e);
    }
  }

  private static byte[] hmacSha256(byte[] key, byte[] data) {
    try {
      Mac mac = Mac.getInstance("HmacSHA256");
      mac.init(new SecretKeySpec(key, "HmacSHA256"));
      return mac.doFinal(data);
    } catch (Exception e) {
      throw new IllegalStateException("HmacSHA256 not available", e);
    }
  }

  private static byte[] hexToBytes(String hex) {
    if (hex == null || hex.length() % 2 != 0) {
      return null;
    }
    byte[] out = new byte[hex.length() / 2];
    for (int i = 0; i < out.length; i++) {
      int hi = Character.digit(hex.charAt(i * 2), 16);
      int lo = Character.digit(hex.charAt(i * 2 + 1), 16);
      if (hi < 0 || lo < 0) {
        return null;
      }
      out[i] = (byte) ((hi << 4) | lo);
    }
    return out;
  }
}
