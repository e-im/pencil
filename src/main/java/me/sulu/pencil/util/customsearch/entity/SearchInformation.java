package me.sulu.pencil.util.customsearch.entity;

public record SearchInformation(
  double searchTime,
  String formattedSearchTime,
  String totalResults,
  String formattedTotalResults
) {
}
