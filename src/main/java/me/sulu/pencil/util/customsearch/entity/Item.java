package me.sulu.pencil.util.customsearch.entity;

import java.util.Objects;

public class Item {
  private String title;
  private String link;
  private String displayLink;
  private String snippet;

  public String getTitle() {
    return title;
  }

  public String getLink() {
    return link;
  }

  public String getDisplayLink() {
    return displayLink;
  }

  public String getSnippet() {
    return Objects.requireNonNullElse(snippet, "No description available.");
  }
}
