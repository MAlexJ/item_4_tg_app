package com.malexj.configuration.properties;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/** Binds the {@code app.*} configuration block (application.yaml). */
@Getter
@Setter
@ConfigurationProperties(prefix = "app")
public class AppProperties {

  /**
   * Public base URL of this service (scheme://host, no trailing slash). Used as the {@code origin}
   * and {@code return_to} of the Telegram Login Widget redirect.
   */
  private String publicUrl = "https://item-4-tg-app.onrender.com";
}
