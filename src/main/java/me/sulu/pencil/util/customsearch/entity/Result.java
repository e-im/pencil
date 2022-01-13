package me.sulu.pencil.util.customsearch.entity;

import java.util.List;

public record Result(
  SearchInformation searchInformation,
  List<Item> items
) {
}
