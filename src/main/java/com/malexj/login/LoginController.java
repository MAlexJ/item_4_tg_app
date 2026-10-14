package com.malexj.login;

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
import org.springframework.web.util.UriUtils;

/**
 * HTTP endpoints for the login form served from {@code /static/index.html}.
 *
 * <ul>
 *   <li>{@code POST /login} — phone / e-mail login, stores the credential + request statistics.</li>
 *   <li>{@code GET /login/tg} — Telegram login entry point (Telegram flow is a separate task; for
 *       now it just bounces back to {@code returnUrl} with an error).</li>
 * </ul>
 */
@Slf4j
@RestController
@RequiredArgsConstructor
public class LoginController {

  private final LoginService loginService;

  @PostMapping("/login")
  public LoginResponse login(@RequestBody LoginRequest request, HttpServletRequest http) {
    return loginService.login(request, http);
  }

  @GetMapping("/login/tg")
  public ResponseEntity<?> telegramLogin(
      @RequestParam(value = "returnUrl", required = false) String returnUrl) {

    log.info("GET /login/tg requested, returnUrl={}", returnUrl);

    // TODO(telegram-login): implement the real Telegram Login Widget flow here and return to
    //  returnUrl with ?token=... / ?auth=ok / ?user=... instead of bouncing back with an error.

    String target = safeReturnUrl(returnUrl);
    if (target != null) {
      String message = "Вхід через Telegram ще не налаштовано";
      URI location =
          URI.create(
              target
                  + (target.contains("?") ? "&" : "?")
                  + "auth_error="
                  + UriUtils.encodeQueryParam(message, StandardCharsets.UTF_8));
      return ResponseEntity.status(HttpStatus.FOUND).location(location).build();
    }

    return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED)
        .body(Map.of("message", "Telegram login is not implemented yet"));
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
}
