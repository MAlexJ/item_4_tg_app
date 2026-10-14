package com.malexj.login;

import lombok.Getter;
import lombok.Setter;

/**
 * Body of {@code POST /login} sent by the static page: {@code { phone | email, method, returnUrl }}.
 */
@Getter
@Setter
public class LoginRequest {

  /** Phone number (digits with an optional leading {@code +}). */
  private String phone;

  /** E-mail address for the {@code email} method. */
  private String email;

  /** Login method: {@code phone} or {@code email}. */
  private String method;

  /** URL the user should be redirected to after the login completes. */
  private String returnUrl;
}
