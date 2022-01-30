package me.sulu.pencil.apis.customsearch;

import me.sulu.pencil.Pencil;
import me.sulu.pencil.apis.customsearch.entity.Result;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

public class GoogleCustomSearch {
  private final Pencil pencil;

  public GoogleCustomSearch(final Pencil pencil) {
    this.pencil = pencil;
  }

  private String url(final String term) {
    return "https://www.googleapis.com/customsearch/v1?q=%s&key=%s&cx=%s&num=10&safe=Medium"
      .formatted(URLEncoder.encode(term.toLowerCase(Locale.ROOT), StandardCharsets.UTF_8).replace("%20", "+"),
        this.pencil.config().global().secrets().customSearch().key(),
        this.pencil.config().global().secrets().customSearch().cx()
      );
  }

  public Mono<Result> search(final String term) {
    return this.pencil.http().get()
      .uri(this.url(term))
      .responseContent()
      .aggregate()
      .asByteArray()
      .map(data -> {
        try {
          return this.pencil.jsonMapper().readValue(data, Result.class);
        } catch (IOException e) {
          throw new RuntimeException(e);
        }
      });
  }

}
