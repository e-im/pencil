package me.sulu.pencil.apis.docs;

import com.algolia.search.DefaultSearchClient;
import com.algolia.search.SearchIndex;
import com.algolia.search.models.indexing.Query;
import com.algolia.search.models.indexing.SearchResult;
import me.sulu.pencil.Pencil;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public class DocSearch {
  private final SearchIndex search;
  private final Pencil pencil;

  public DocSearch(final Pencil pencil) {
    this.pencil = pencil;

    this.search = DefaultSearchClient.create(this.pencil.config().global().secrets().algoliaSearch().applicationId(), this.pencil.config().global().secrets().algoliaSearch().apiKey())
      .initIndex(this.pencil.config().global().secrets().algoliaSearch().index(), DocItem.class);
  }

  public Flux<DocItem> search(final String term) {
    return Mono.just(search.search(new Query(term)))
      .flatMapIterable(SearchResult::getHits)
      .cast(DocItem.class);
  }
}
