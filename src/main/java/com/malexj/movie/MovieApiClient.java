package com.malexj.movie;

import com.malexj.configuration.properties.TelegramProperties;
import com.malexj.movie.dto.MovieDto;
import com.malexj.movie.dto.MoviePage;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

/** Fetches the movie list from the movie-hub REST API. */
@Slf4j
@Service
@RequiredArgsConstructor
public class MovieApiClient {

  private final RestClient restClient;
  private final TelegramProperties properties;

  public List<MovieDto> fetchMovies() {
    TelegramProperties.Movie movie = properties.getMovie();
    String url =
        movie.getApiUrl() + "?page=" + movie.getPage() + "&size=" + movie.getSize();

    try {
      MoviePage page = restClient.get().uri(url).retrieve().body(MoviePage.class);
      if (page == null || page.content() == null || page.content().isEmpty()) {
        log.warn("Movie API returned no movies for {}", url);
        return List.of();
      }
      log.debug("Fetched {} movies from movie-hub API", page.content().size());
      return page.content();
    } catch (Exception e) {
      log.error("Failed to fetch movies from {}: {}", url, e.getMessage());
      return List.of();
    }
  }

  /** Returns a random movie from the current page, used by the in-bot rating menu. */
  public Optional<MovieDto> fetchRandomMovie() {
    List<MovieDto> movies = fetchMovies();
    if (movies.isEmpty()) {
      return Optional.empty();
    }
    return Optional.of(movies.get(ThreadLocalRandom.current().nextInt(movies.size())));
  }

  /** Fetches a single movie by id via {@code /v1/movies/{id}}. */
  public Optional<MovieDto> fetchMovieById(Long id) {
    if (id == null) {
      return Optional.empty();
    }
    String url = properties.getMovie().getApiUrl() + "/" + id;
    try {
      MovieDto movie = restClient.get().uri(url).retrieve().body(MovieDto.class);
      return Optional.ofNullable(movie);
    } catch (Exception e) {
      log.error("Failed to fetch movie {} from {}: {}", id, url, e.getMessage());
      return Optional.empty();
    }
  }
}
