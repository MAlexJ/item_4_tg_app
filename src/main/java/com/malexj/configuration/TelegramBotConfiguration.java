package com.malexj.configuration;

import com.malexj.configuration.properties.TelegramProperties;
import com.pengrad.telegrambot.TelegramBot;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class TelegramBotConfiguration {

  @Bean
  public TelegramBot telegramBot(TelegramProperties properties) {
    String token = properties.getBot().getToken();
    return new TelegramBot(token == null ? "" : token);
  }
}
