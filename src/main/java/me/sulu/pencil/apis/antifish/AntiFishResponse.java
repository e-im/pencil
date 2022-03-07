package me.sulu.pencil.apis.antifish;

import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.List;

public record AntiFishResponse(
  boolean match,
  @Nullable List<AntiFishMatch> matches
  ) {
}
