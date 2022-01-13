package me.sulu.pencil.util.customsearch.entity;

import java.util.Objects;

public record Item(
  String title,
  String link,
  String displayLink,
  String snippet
) {
  @Override
  public String snippet() {
    return Objects.requireNonNullElse(snippet, "No description available.");
  }
}
