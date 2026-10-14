package com.malexj.movie.dto;

import java.util.List;

/** Paginated response of the movie-hub REST API. */
public record MoviePage(
    List<MovieDto> content,
    int number,
    int size,
    long totalElements,
    int totalPages) {}
