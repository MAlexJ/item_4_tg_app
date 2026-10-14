package com.malexj.configuration.properties;

import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/** Binds the {@code telegram.*} configuration block (telegram.yaml). */
@Getter
@Setter
@ConfigurationProperties(prefix = "telegram")
public class TelegramProperties {

  private final Bot bot = new Bot();
  private final Movie movie = new Movie();
  private final Post post = new Post();

  @Getter
  @Setter
  public static class Bot {
    private String token;
    private String channelId;
    private List<String> allowedUpdates =
        new ArrayList<>(List.of("chat_member", "my_chat_member", "message", "callback_query"));
  }

  @Getter
  @Setter
  public static class Movie {
    private String apiUrl = "https://rest-api-movie-hub-latest.onrender.com/v1/movies";
    private int page = 0;
    private int size = 50;
  }

  @Getter
  @Setter
  public static class Post {
    private String cron = "0 */30 * * * *";
    private String timezone = "UTC";
  }
}
