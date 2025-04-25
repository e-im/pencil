package me.sulu.pencil.apis.docs;

import com.algolia.api.SearchClient;
import com.algolia.model.search.SearchParamsObject;
import com.algolia.model.search.SearchResponse;
import me.sulu.pencil.Pencil;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public class DocSearch {
  private final SearchClient client;
  private final Pencil pencil;

  public DocSearch(final Pencil pencil) {
    this.pencil = pencil;

    this.client = new SearchClient(this.pencil.config().global().secrets().algoliaSearch().applicationId(), this.pencil.config().global().secrets().algoliaSearch().apiKey());
  }

  public Flux<DocItem> search(final String term) {
    return Mono.just(this.client.searchSingleIndex(this.pencil.config().global().secrets().algoliaSearch().index(), new SearchParamsObject().setQuery(term), DocItem.class))
      .flatMapIterable(SearchResponse::getHits);
  }
}
