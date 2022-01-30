package me.sulu.pencil.apis.customsearch.entity;

public record SearchInformation(
  double searchTime,
  String formattedSearchTime,
  String totalResults,
  String formattedTotalResults
) {
}
