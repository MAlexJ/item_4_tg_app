package com.malexj.movie;

import com.malexj.movie.dto.MovieDto;
import java.util.Locale;

/** Builds the HTML caption used for a movie post / rating card. */
public final class MovieCaptionBuilder {

  private static final int MAX_DESCRIPTION_LENGTH = 400;

  private MovieCaptionBuilder() {}

  public static String build(MovieDto m) {
    return build(m, null);
  }

  public static String build(MovieDto m, String titleUrl) {
    StringBuilder sb = new StringBuilder();

    String title = escape(m.title());
    if (titleUrl != null && !titleUrl.isBlank()) {
      // Make the movie title a clickable link to the bot (deep link for voting)
      sb.append("🎬 <a href=\"")
          .append(escape(titleUrl))
          .append("\"><b>")
          .append(title)
          .append("</b></a>");
    } else {
      sb.append("🎬 <b>").append(title).append("</b>");
    }
    if (m.releaseYear() != null) {
      sb.append(" (").append(m.releaseYear()).append(')');
    }

    sb.append('\n').append("⭐ ").append(formatRating(m.averageRating()));
    if (m.ratingsCount() != null) {
      sb.append(" · ").append(m.ratingsCount()).append(" votes");
    }

    if (m.categoryName() != null && !m.categoryName().isBlank()) {
      sb.append('\n').append("🎭 ").append(escape(m.categoryName()));
    }

    if (m.description() != null && !m.description().isBlank()) {
      sb.append("\n\n").append(escape(truncate(m.description(), MAX_DESCRIPTION_LENGTH)));
    }

    return sb.toString();
  }

  public static String formatRating(Double rating) {
    if (rating == null) {
      return "—";
    }
    if (rating == Math.floor(rating) && !rating.isInfinite()) {
      return String.valueOf(rating.longValue());
    }
    return String.format(Locale.US, "%.1f", rating);
  }

  public static String escape(String value) {
    if (value == null) {
      return "";
    }
    return value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
  }

  private static String truncate(String value, int max) {
    if (value.length() <= max) {
      return value;
    }
    return value.substring(0, max - 1) + "…";
  }
}
