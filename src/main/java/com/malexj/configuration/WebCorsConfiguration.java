package com.malexj.configuration;

import java.util.Arrays;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * CORS for the {@code /login} endpoints. The allowed origins are configured through the
 * {@code CORS_ALLOWED_ORIGINS} environment variable (comma-separated) so the same image can be
 * deployed to different domains without a rebuild.
 *
 * <ul>
 *   <li>{@code *} (default) — allow any origin.</li>
 *   <li>{@code https://app.example.com} — exact origins.</li>
 *   <li>{@code https://*.example.com} — wildcard sub-domains.</li>
 *   <li>{@code null} — required only if the page is opened from disk ({@code file://}).</li>
 * </ul>
 */
@Configuration
public class WebCorsConfiguration implements WebMvcConfigurer {

  private final String[] allowedOrigins;

  public WebCorsConfiguration(@Value("${cors.allowed-origins:*}") String allowedOrigins) {
    this.allowedOrigins = parseAllowedOrigins(allowedOrigins);
  }

  @Override
  public void addCorsMappings(CorsRegistry registry) {
    registry
        .addMapping("/**")
        .allowedOriginPatterns(allowedOrigins)
        .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
        .allowedHeaders("*")
        .exposedHeaders("Location")
        .allowCredentials(false)
        .maxAge(3600);
  }

  private static String[] parseAllowedOrigins(String raw) {
    if (raw == null || raw.isBlank()) {
      return new String[] {"*"};
    }
    return Arrays.stream(raw.split(","))
        .map(String::trim)
        .filter(s -> !s.isEmpty())
        .toArray(String[]::new);
  }
}
