package me.sulu.pencil.util.customsearch.entity;

public class SearchInformation {
  private double searchTime;
  private String formattedSearchTime;
  private String totalResults;
  private String formattedTotalResults;

  public double getSearchTime() {
    return searchTime;
  }

  public void setSearchTime(double searchTime) {
    this.searchTime = searchTime;
  }

  public String getFormattedSearchTime() {
    return formattedSearchTime;
  }

  public String getTotalResults() {
    return totalResults;
  }

  public String getFormattedTotalResults() {
    return formattedTotalResults;
  }
}
