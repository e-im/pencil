package me.sulu.pencil.apis.docs;

import me.sulu.pencil.Pencil;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

public class DocSearch {
  private final Pencil pencil;

  public DocSearch(final Pencil pencil) {
    this.pencil = pencil;
  }

  public Mono<DocItem[]> search(final String term) {
    return this.pencil.http().get()
      .uri("https://docs-search.evan.workers.dev/search/" + URLEncoder.encode(term, StandardCharsets.UTF_8))
      .responseContent()
      .aggregate()
      .asString()
      .map(data -> {
        try {
          System.out.println(data);
          return this.pencil.jsonMapper().readValue(data, DocItem[].class);
        } catch (IOException e) {
          throw new RuntimeException(e);
        }
      });
  }
}
