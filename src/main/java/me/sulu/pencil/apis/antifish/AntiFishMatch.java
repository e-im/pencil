package me.sulu.pencil.apis.antifish;

import com.fasterxml.jackson.annotation.JsonProperty;
public record AntiFishMatch(
  boolean followed,
  String domain,
  String source,
  AntiFishType type,
  @JsonProperty("trust_rating")
  float trustRating
) {
}
