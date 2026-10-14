package com.malexj.login;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Lightweight, dependency-free {@code User-Agent} parser. Extracts just enough to answer "which
 * browser / OS / device is this user on" for the login statistics — no external UA library needed.
 */
public final class UserAgentParser {

  private UserAgentParser() {}

  public record Info(
      String browser,
      String browserVersion,
      String os,
      String osVersion,
      String deviceType,
      boolean bot) {

    public static Info unknown() {
      return new Info(null, null, null, null, "Unknown", false);
    }
  }

  public static Info parse(String userAgent) {
    if (userAgent == null || userAgent.isBlank()) {
      return Info.unknown();
    }
    String ua = userAgent.trim();

    boolean bot = isBot(ua);
    String browser = browser(ua);
    String browserVersion = versionOf(ua, browser);
    String os = os(ua);
    String osVersion = "macOS".equals(os) ? macOsVersion(ua) : versionOf(ua, os);
    String deviceType = deviceType(ua, bot);

    return new Info(browser, browserVersion, os, osVersion, deviceType, bot);
  }

  /* ------------------------------------------------------------------ browser */

  private static String browser(String ua) {
    String s = lower(ua);
    if (s.contains("edg/") || s.contains("edge/")) return "Edge";
    if (s.contains("opr/") || s.contains("opera")) return "Opera";
    if (s.contains("samsungbrowser")) return "Samsung Browser";
    if (s.contains("yabrowser")) return "Yandex Browser";
    if (s.contains("vivaldi")) return "Vivaldi";
    if (s.contains("crios/")) return "Chrome"; // Chrome on iOS
    if (s.contains("fxios/")) return "Firefox"; // Firefox on iOS
    if (s.contains("chrome/")) return "Chrome";
    if (s.contains("firefox/")) return "Firefox";
    if (s.contains("safari/") && s.contains("version/")) return "Safari";
    if (s.contains("instagram")) return "Instagram";
    if (s.contains("facebook") || s.contains("fbav") || s.contains("fban")) return "Facebook";
    if (s.contains("telegrambot") || s.contains("telegram")) return "Telegram";
    return null;
  }

  /* ----------------------------------------------------------------------- os */

  private static String os(String ua) {
    String s = lower(ua);
    if (s.contains("windows nt")) return "Windows";
    if (s.contains("android")) return "Android";
    if (s.contains("iphone")) return "iOS";
    if (s.contains("ipad")) return "iPadOS";
    if (s.contains("ipod")) return "iOS";
    if (s.contains("cros")) return "Chrome OS";
    if (s.contains("mac os x") || s.contains("macintosh")) return "macOS";
    if (s.contains("linux") || s.contains("x11")) return "Linux";
    if (s.contains("ubuntu")) return "Ubuntu";
    return null;
  }

  /* ------------------------------------------------------------------- device */

  private static String deviceType(String ua, boolean bot) {
    if (bot) return "Bot";
    String s = lower(ua);
    if (s.contains("ipad") || s.contains("tablet") || (s.contains("android") && !s.contains("mobile"))) {
      return "Tablet";
    }
    if (s.contains("mobile")
        || s.contains("iphone")
        || s.contains("ipod")
        || s.contains("android")
        || s.contains("windows phone")) {
      return "Mobile";
    }
    if (s.contains("windows")
        || s.contains("macintosh")
        || s.contains("linux")
        || s.contains("cros")
        || s.contains("x11")) {
      return "Desktop";
    }
    return "Unknown";
  }

  /* ---------------------------------------------------------------------- bot */

  private static boolean isBot(String ua) {
    String s = lower(ua);
    return s.contains("bot")
        || s.contains("crawler")
        || s.contains("spider")
        || s.contains("slurp")
        || s.contains("scanner")
        || s.contains("curl/")
        || s.contains("wget/")
        || s.contains("headlesschrome")
        || s.contains("python-requests");
  }

  /* ------------------------------------------------------------------ helpers */

  /**
   * Extracts the version token that follows the given product name. For {@code Chrome/131.0.0.0}
   * with product {@code Chrome} this returns {@code 131.0.0.0}; for {@code Android 15} it returns
   * {@code 15}; for {@code Mac OS X 10_15_7} it returns {@code 10.15.7}.
   */
  private static String versionOf(String ua, String product) {
    if (product == null || ua == null) {
      return null;
    }
    Matcher m =
        Pattern.compile(Pattern.quote(product) + "[/ ]+([0-9][0-9._]*)", Pattern.CASE_INSENSITIVE)
            .matcher(ua);
    if (m.find()) {
      return m.group(1).replace('_', '.');
    }
    return null;
  }

  /** Extracts {@code 10.15.7} from {@code ... Mac OS X 10_15_7 ...}. */
  private static String macOsVersion(String ua) {
    Matcher m =
        Pattern.compile("Mac OS X[ /]+([0-9][0-9._]*)", Pattern.CASE_INSENSITIVE).matcher(ua);
    return m.find() ? m.group(1).replace('_', '.') : null;
  }

  private static String lower(String s) {
    return s.toLowerCase(Locale.ROOT);
  }
}
