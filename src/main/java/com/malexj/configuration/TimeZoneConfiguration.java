package com.malexj.configuration;

import jakarta.annotation.PostConstruct;
import java.util.TimeZone;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
public class TimeZoneConfiguration {

  @Value("${application.timezone:UTC}")
  private String applicationTimeZone;

  /*
   * Last, we can use PostConstruct of our MainApplication class to set the default TimeZone value just after
   * the initialization of WebApplicationContext is completed.
   *
   * At this point, we can inject the TimeZone value from our configuration properties:
   */
  @PostConstruct
  public void init() {
    TimeZone.setDefault(TimeZone.getTimeZone(applicationTimeZone));
  }
}
