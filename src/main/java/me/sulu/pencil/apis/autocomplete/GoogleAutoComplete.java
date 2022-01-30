package me.sulu.pencil.apis.autocomplete;

import discord4j.discordjson.json.ApplicationCommandOptionChoiceData;
import me.sulu.pencil.Pencil;
import reactor.core.publisher.Flux;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

public class GoogleAutoComplete {
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
      .asByteArray()
      .flatMapIterable(data -> {
        try {
          return this.pencil.xmlMapper().readTree(data).get("CompleteSuggestion");
        } catch (IOException e) {
          throw new RuntimeException(e);
        }
      })
      .map(node -> node.get("suggestion").get("data").asText())
      .map(suggestion -> ApplicationCommandOptionChoiceData.builder().name(suggestion).value(suggestion).build());
  }
}
