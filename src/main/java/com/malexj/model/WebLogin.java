package com.malexj.model;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 * A single login attempt coming from the static web page (phone / email form). Stores the
 * credential the user submitted together with the request metadata (IP address, browser, device,
 * language, referrer, ...) so every sign-in can be audited later.
 */
@Getter
@Setter
@Document(collection = "web_logins")
public class WebLogin {

  /** Random UUID generated on the server (not a Telegram id). */
  @Id private String id;

  // --- Credentials ---
  /** Normalized phone number in E.164-like form, e.g. {@code +380501234567}. */
  private String phoneNumber;
  /** Phone exactly as the client sent it. */
  private String rawPhone;
  /** E-mail address (only for the {@code email} method). */
  private String email;
  /** {@code phone} | {@code email} | {@code telegram}. */
  private String method;
  /** The page the user wants to return to after login. */
  private String returnUrl;

  // --- Request metadata (statistics) ---
  /** Best-effort public IP of the client (honours proxy headers). */
  private String ipAddress;
  /** Raw {@code User-Agent} header. */
  private String userAgent;
  /** Parsed browser name, e.g. Chrome / Safari / Firefox. */
  private String browser;
  /** Parsed browser version. */
  private String browserVersion;
  /** Parsed operating system, e.g. Windows / macOS / iOS / Android. */
  private String os;
  /** Parsed operating system version. */
  private String osVersion;
  /** Desktop | Mobile | Tablet | Bot | Unknown. */
  private String deviceType;
  /** True when the User-Agent looks like a crawler / bot. */
  private Boolean bot;
  /** {@code Referer} header. */
  private String referer;
  /** {@code Origin} header. */
  private String origin;
  /** {@code Accept-Language} header (user's preferred languages). */
  private String acceptLanguage;
  /** Curated subset of the interesting request headers (no cookies/auth). */
  private Map<String, String> headers = new LinkedHashMap<>();

  // --- Timestamps ---
  private Instant createdAt;
  private Instant updatedAt;
}
