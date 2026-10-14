package com.malexj.login;

import com.malexj.configuration.properties.AppProperties;
import com.malexj.model.TelegramWebUser;
import jakarta.servlet.http.HttpServletRequest;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.util.UriUtils;

/**
 * HTTP endpoints for the login form served from {@code /static/index.html}.
 *
 * <ul>
 *   <li>{@code POST /login} — phone / e-mail login, stores the credential + request statistics.</li>
 *   <li>{@code GET /login/tg} — starts the Telegram Login Widget (OAuth) redirect.</li>
 *   <li>{@code GET /login/tg/callback} — Telegram redirects back here with the signed user data;
 *       the hash is verified and the user is persisted, then the browser is bounced to
 *       {@code returnUrl} with {@code ?auth=ok&user=...} or {@code ?error=...}.</li>
 * </ul>
 */
@Slf4j
@RestController
@RequiredArgsConstructor
public class LoginController {

  private final LoginService loginService;
  private final TelegramLoginService telegramLoginService;
  private final AppProperties appProperties;

  @PostMapping("/login")
  public LoginResponse login(@RequestBody LoginRequest request, HttpServletRequest http) {
    return loginService.login(request, http);
  }

  /** Step 1: redirect the browser to Telegram's OAuth page. */
  @GetMapping("/login/tg")
  public ResponseEntity<Void> telegramLogin(
      @RequestParam(value = "returnUrl", required = false) String returnUrl) {

    String target = fallbackTarget(returnUrl);

    if (!telegramLoginService.isConfigured()) {
      log.warn("Telegram login requested but TELEGRAM_BOT_TOKEN is not configured");
      return redirect(appendQuery(target, "error=" + encode("Telegram-бот не налаштовано")));
    }

    String oauthUrl = telegramLoginService.buildAuthRedirectUrl(target);
    log.info("GET /login/tg -> redirect to Telegram OAuth, returnUrl={}", target);
    return redirect(oauthUrl);
  }

  /** Step 2: Telegram redirects back here with {@code id, first_name, ..., hash}. */
  @GetMapping("/login/tg/callback")
  public ResponseEntity<Void> telegramCallback(
      @RequestParam(value = "returnUrl", required = false) String returnUrl,
      @RequestParam Map<String, String> params) {

    String target = fallbackTarget(returnUrl);

    try {
      TelegramWebUser user = telegramLoginService.verifyAndSave(params);
      String name = TelegramLoginService.displayName(user);
      log.info("Telegram login ok for {} -> redirect to {}", user.getUserId(), target);
      return redirect(appendQuery(target, "auth=ok&user=" + encode(name)));
    } catch (ResponseStatusException e) {
      log.warn("Telegram login failed: {}", e.getReason());
      return redirect(appendQuery(target, "error=" + encode(e.getReason())));
    }
  }

  private String fallbackTarget(String returnUrl) {
    String target = safeReturnUrl(returnUrl);
    return target != null ? target : appProperties.getPublicUrl();
  }

  /** Only allow bouncing back to http(s)/file URLs — prevents open-redirect misuse. */
  private static String safeReturnUrl(String returnUrl) {
    if (returnUrl == null || returnUrl.isBlank()) {
      return null;
    }
    String lower = returnUrl.toLowerCase();
    if (!lower.startsWith("http://") && !lower.startsWith("https://") && !lower.startsWith("file://")) {
      return null;
    }
    return returnUrl;
  }

  private static ResponseEntity<Void> redirect(String url) {
    return ResponseEntity.status(HttpStatus.FOUND).location(URI.create(url)).build();
  }

  private static String appendQuery(String base, String query) {
    return base + (base.contains("?") ? "&" : "?") + query;
  }

  private static String encode(String value) {
    return UriUtils.encodeQueryParam(value, StandardCharsets.UTF_8);
  }
}
