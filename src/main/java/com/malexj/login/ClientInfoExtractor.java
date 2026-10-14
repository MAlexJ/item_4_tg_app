package com.malexj.login;

import jakarta.servlet.http.HttpServletRequest;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.stereotype.Component;

/**
 * Pulls the "who is behind this request" facts out of an {@link HttpServletRequest}: the client IP
 * (honouring the proxy headers used on Render and behind common CDNs), the User-Agent and a curated
 * set of other headers. No cookies or Authorization values are stored.
 */
@Component
public class ClientInfoExtractor {

  /** Headers we consider useful for the login statistics (everything else is ignored). */
  private static final List<String> INTERESTING_HEADERS =
      List.of(
          "x-forwarded-for",
          "x-forwarded-host",
          "x-forwarded-proto",
          "x-real-ip",
          "cf-connecting-ip",
          "cf-ipcountry",
          "forwarded",
          "sec-ch-ua",
          "sec-ch-ua-platform",
          "sec-ch-ua-mobile",
          "sec-fetch-site",
          "sec-fetch-mode",
          "sec-fetch-dest",
          "accept-encoding",
          "accept-language",
          "referer",
          "origin");

  public record ClientInfo(
      String ipAddress,
      String userAgent,
      String referer,
      String origin,
      String acceptLanguage,
      Map<String, String> headers) {}

  public ClientInfo extract(HttpServletRequest request) {
    String ip = resolveIp(request);
    String userAgent = request.getHeader("User-Agent");

    Map<String, String> headers = new LinkedHashMap<>();
    for (String name : INTERESTING_HEADERS) {
      String value = request.getHeader(name);
      if (value != null && !value.isBlank()) {
        headers.put(name.toLowerCase(Locale.ROOT), value);
      }
    }

    return new ClientInfo(
        ip,
        userAgent,
        request.getHeader("Referer"),
        request.getHeader("Origin"),
        request.getHeader("Accept-Language"),
        headers);
  }

  /**
   * Resolves the public client IP. The first non-empty proxy header wins; {@code X-Forwarded-For}
   * and {@code Forwarded} may contain a list, so only the left-most (client) entry is taken.
   */
  private String resolveIp(HttpServletRequest request) {
    String viaCf = request.getHeader("CF-Connecting-IP");
    if (hasText(viaCf)) {
      return first(viaCf);
    }

    String viaXff = request.getHeader("X-Forwarded-For");
    if (hasText(viaXff)) {
      return first(viaXff);
    }

    String viaRealIp = request.getHeader("X-Real-IP");
    if (hasText(viaRealIp)) {
      return first(viaRealIp);
    }

    String viaForwarded = request.getHeader("Forwarded");
    if (hasText(viaForwarded)) {
      String forPart = extractForwardedFor(viaForwarded);
      if (forPart != null) {
        return forPart;
      }
    }

    return request.getRemoteAddr();
  }

  private static String first(String value) {
    String trimmed = value.trim();
    int comma = trimmed.indexOf(',');
    return comma > 0 ? trimmed.substring(0, comma).trim() : trimmed;
  }

  /** Extracts the {@code for=...} token from an RFC 7239 {@code Forwarded} header. */
  private static String extractForwardedFor(String forwarded) {
    for (String part : forwarded.split(";")) {
      String trimmed = part.trim();
      if (trimmed.regionMatches(true, 0, "for=", 0, 4)) {
        String value = trimmed.substring(4).trim();
        if (value.startsWith("\"")) {
          value = value.replaceAll("^\"|\"$", "");
        }
        return first(value);
      }
    }
    return null;
  }

  private static boolean hasText(String value) {
    return value != null && !value.isBlank();
  }
}
