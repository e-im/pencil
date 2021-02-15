package me.sulu.pencil.util.customsearch.entity;

import java.util.List;

public class Result {
  private SearchInformation searchInformation;
  private List<Item> items;

  public SearchInformation getSearchInformation() {
    return searchInformation;
  }

  public List<Item> getItems() {
    return items;
  }
}
