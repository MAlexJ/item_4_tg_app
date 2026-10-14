package com.malexj.model;

import java.time.Instant;
import lombok.Getter;
import lombok.Setter;

/** A single movie vote embedded inside a survey participant document. */
@Getter
@Setter
public class SurveyVote {

  private Long movieId;
  private Integer score;
  private Instant votedAt;
}
