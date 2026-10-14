package com.malexj.configuration;

import java.net.http.HttpClient;
import java.time.Duration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@Configuration
public class RestClientConfiguration {

  /*
   * Telegram API: Request Timeouts
   * link: https://telegram-bot-sdk.com/docs/advanced/timeouts/
   *
   * Connection timeout
   * Default connection timeout is 10 seconds. It can be changed by calling setConnectTimeOut($seconds).
   *
   * General timeout
   * Default timeout of Telegram Bot API server response is 60 seconds.
   * It can be changed by calling setTimeOut($seconds).
   */
  public static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(30);
  public static final Duration READ_TIMEOUT = Duration.ofSeconds(60);

  @Bean
  public RestClient restClient() {

    // create JDK HttpClient with timeouts
    HttpClient httpClient = HttpClient.newBuilder().connectTimeout(CONNECT_TIMEOUT).build();

    // Spring request factory wrapper
    JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);

    // read timeout in milliseconds
    requestFactory.setReadTimeout(READ_TIMEOUT);

    return RestClient.builder() //
        .requestFactory(requestFactory)
        .build();
  }
}
