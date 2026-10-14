package com.malexj.login;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Response of the login endpoints. Null fields are omitted from JSON so the static page can rely
 * on the contract documented in {@code index.html}: {@code redirect}, {@code ok} / {@code token},
 * or {@code message} on error.
 */
@Getter
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class LoginResponse {

  private final Boolean ok;
  private final String token;
  private final String message;
  private final String redirect;

  /** Immediate, successful login (no SMS challenge — the backend decides). */
  public static LoginResponse ok() {
    return new LoginResponse(true, null, null, null);
  }

  public static LoginResponse okWithToken(String token) {
    return new LoginResponse(true, token, null, null);
  }

  public static LoginResponse redirect(String url) {
    return new LoginResponse(null, null, null, url);
  }

  public static LoginResponse message(String message) {
    return new LoginResponse(null, null, message, null);
  }
}
