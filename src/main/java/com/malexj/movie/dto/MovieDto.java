package com.malexj.movie.dto;

/** One movie returned by the movie-hub REST API. */
public record MovieDto(
    Long id,
    String title,
    String description,
    Double averageRating,
    Integer ratingsCount,
    Integer categoryId,
    String categoryName,
    Integer releaseYear,
    String imageUrl,
    String createdAt,
    String updatedAt) {}
