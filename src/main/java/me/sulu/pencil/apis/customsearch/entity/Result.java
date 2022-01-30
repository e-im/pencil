package me.sulu.pencil.apis.customsearch.entity;

import java.util.List;

public record Result(
  SearchInformation searchInformation,
  List<Item> items
) {
}
