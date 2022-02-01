package me.sulu.pencil.apis.autocomplete;

import com.fasterxml.jackson.databind.JsonNode;
import discord4j.discordjson.json.ApplicationCommandOptionChoiceData;
import me.sulu.pencil.Pencil;
import reactor.core.publisher.Flux;
import reactor.util.Logger;
import reactor.util.Loggers;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
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

  public Flux<ApplicationCommandOptionChoiceData> complete(final String term) {
    return pencil.http().get()
      .uri(this.url(term))
      .responseContent()
      .aggregate()
      .asString()
      .flatMapIterable(data -> {
        try {
          final JsonNode suggestions = this.pencil.xmlMapper().readTree(data).get("CompleteSuggestion");
          if (suggestions == null) {
            return Collections.emptyList();
          }
          return suggestions;
        } catch (IOException e) {
          LOGGER.warn("Failed to handle Google autocomplete API response {}", data);
          throw new RuntimeException(e);
        }
      })
      .map(node -> node.get("suggestion").get("data").asText())
      .map(suggestion -> ApplicationCommandOptionChoiceData.builder().name(suggestion).value(suggestion).build());
  }
}
