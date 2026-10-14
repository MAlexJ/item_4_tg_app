package com.malexj.login;

import com.malexj.login.ClientInfoExtractor.ClientInfo;
import com.malexj.login.UserAgentParser.Info;
import com.malexj.model.WebLogin;
import com.malexj.repository.WebLoginRepository;
import jakarta.servlet.http.HttpServletRequest;
import java.time.Instant;
import java.util.UUID;
import java.util.regex.Pattern;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

/**
 * Business logic behind {@code POST /login}. Validates the submitted phone number / e-mail, enriches
 * it with the request statistics (IP, browser, OS, device, ...) and stores everything in MongoDB.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LoginService {

  private static final Pattern EMAIL = Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]{2,}$");

  private final WebLoginRepository repository;
  private final ClientInfoExtractor clientInfoExtractor;

  public LoginResponse login(LoginRequest request, HttpServletRequest http) {
    String method = normalizeMethod(request.getMethod());
    if ("phone".equals(method)) {
      return loginWithPhone(request, http);
    }
    if ("email".equals(method)) {
      return loginWithEmail(request, http);
    }
    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unsupported login method: " + request.getMethod());
  }

  private LoginResponse loginWithPhone(LoginRequest request, HttpServletRequest http) {
    String phone = normalizePhone(request.getPhone());
    if (phone == null) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "Некоректний номер телефону. Очікується формат +380XXXXXXXXX");
    }

    WebLogin doc = buildDocument(request, http);
    doc.setMethod("phone");
    doc.setRawPhone(request.getPhone());
    doc.setPhoneNumber(phone);
    repository.save(doc);

    log.info(
        "Phone login recorded: {} from ip={} browser={} {} os={} device={}",
        phone,
        doc.getIpAddress(),
        doc.getBrowser(),
        doc.getBrowserVersion(),
        doc.getOs(),
        doc.getDeviceType());
    return LoginResponse.ok();
  }

  private LoginResponse loginWithEmail(LoginRequest request, HttpServletRequest http) {
    String email = request.getEmail() == null ? null : request.getEmail().trim();
    if (email == null || !EMAIL.matcher(email).matches()) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Некоректна електронна пошта");
    }

    WebLogin doc = buildDocument(request, http);
    doc.setMethod("email");
    doc.setEmail(email);
    repository.save(doc);

    log.info(
        "Email login recorded: {} from ip={} browser={} {} os={}",
        email,
        doc.getIpAddress(),
        doc.getBrowser(),
        doc.getBrowserVersion(),
        doc.getOs());
    return LoginResponse.ok();
  }

  private WebLogin buildDocument(LoginRequest request, HttpServletRequest http) {
    ClientInfo clientInfo = clientInfoExtractor.extract(http);
    Info ua = UserAgentParser.parse(clientInfo.userAgent());

    Instant now = Instant.now();
    WebLogin doc = new WebLogin();
    doc.setId(UUID.randomUUID().toString());
    doc.setReturnUrl(request.getReturnUrl());
    doc.setIpAddress(clientInfo.ipAddress());
    doc.setUserAgent(clientInfo.userAgent());
    doc.setBrowser(ua.browser());
    doc.setBrowserVersion(ua.browserVersion());
    doc.setOs(ua.os());
    doc.setOsVersion(ua.osVersion());
    doc.setDeviceType(ua.deviceType());
    doc.setBot(ua.bot());
    doc.setReferer(clientInfo.referer());
    doc.setOrigin(clientInfo.origin());
    doc.setAcceptLanguage(clientInfo.acceptLanguage());
    doc.setHeaders(clientInfo.headers());
    doc.setCreatedAt(now);
    doc.setUpdatedAt(now);
    return doc;
  }

  private static String normalizeMethod(String method) {
    return method == null ? null : method.trim().toLowerCase();
  }

  /**
   * Normalizes a phone number to {@code +<digits>}. Accepts {@code +380501234567}, {@code
   * 380501234567} and the UA local form {@code 0501234567} (leading {@code 0} is expanded to the
   * {@code 38} country code). Returns {@code null} when the input cannot be a phone number.
   */
  static String normalizePhone(String raw) {
    if (raw == null) {
      return null;
    }
    String digits = raw.replaceAll("\\D", "");
    if (digits.isEmpty()) {
      return null;
    }
    // UA local format: 0XX XXX-XX-XX -> 38 0XX XXX-XX-XX
    if (digits.startsWith("0")) {
      digits = "38" + digits.substring(1);
    }
    if (digits.length() < 10 || digits.length() > 15) {
      return null;
    }
    return "+" + digits;
  }
}
