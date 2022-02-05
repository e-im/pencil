package me.sulu.pencil.apis.autocomplete;

import com.fasterxml.jackson.databind.JsonNode;
import me.sulu.pencil.Pencil;
import reactor.core.publisher.Flux;
import reactor.util.Logger;
import reactor.util.Loggers;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

public class GoogleAutoComplete {
  private static final Logger LOGGER = Loggers.getLogger(GoogleAutoComplete.class);
  private final Pencil pencil;

  public GoogleAutoComplete(final Pencil pencil) {
    this.pencil = pencil;
  }

  private String url(final String term) {
    return "https://google.com/complete/search?output=toolbar&q="
      + URLEncoder.encode(term.toLowerCase(Locale.ROOT), StandardCharsets.UTF_8).replace("%20", "+");
  }

  public Flux<String> complete(final String term) {
    return pencil.http().get()
      .uri(this.url(term))
      .responseContent()
      .aggregate()
      .asString()
      .mapNotNull(data -> {
        try {
          return this.pencil.xmlMapper().readTree(data);
        } catch (IOException e) {
          LOGGER.warn("Failed to handle Google autocomplete API response {}", data);
          throw new RuntimeException(e);
        }
      })
      .mapNotNull(node -> node.get("CompleteSuggestion"))
      .flatMapIterable(node -> node)
      .mapNotNull(node -> node.get("suggestion"))
      .mapNotNull(node -> node.get("data"))
      .mapNotNull(JsonNode::asText);
  }
}
