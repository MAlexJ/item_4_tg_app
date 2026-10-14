package com.malexj.configuration;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * The static page may be opened directly from disk ({@code file://}, which sends {@code Origin:
 * null}) as well as from this server. Both need to be allowed to POST JSON to {@code /login};
 * the {@code Origin: *} / {@code null} combination triggers a CORS preflight, so we allow it here.
 */
@Configuration
public class WebCorsConfiguration implements WebMvcConfigurer {

  @Override
  public void addCorsMappings(CorsRegistry registry) {
    registry
        .addMapping("/**")
        .allowedOriginPatterns("*")
        .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
        .allowedHeaders("*")
        .exposedHeaders("Location")
        .allowCredentials(false)
        .maxAge(3600);
  }
}
