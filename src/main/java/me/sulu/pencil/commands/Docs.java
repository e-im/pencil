package me.sulu.pencil.commands;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import discord4j.core.event.domain.interaction.ChatInputAutoCompleteEvent;
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.object.command.ApplicationCommandOption;
import discord4j.discordjson.json.ApplicationCommandOptionChoiceData;
import discord4j.discordjson.json.ApplicationCommandOptionData;
import discord4j.discordjson.json.ApplicationCommandRequest;
import me.sulu.pencil.Pencil;
import me.sulu.pencil.apis.docs.DocItem;
import me.sulu.pencil.apis.docs.DocSearch;
import me.sulu.pencil.util.StringUtil;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.Collections;

public class Docs extends Command {
  private final ApplicationCommandRequest request = ApplicationCommandRequest.builder()
    .name("docs")
    .description("Search the PaperMC docs")
    .addOption(ApplicationCommandOptionData.builder()
      .name("term")
      .description("Search term")
      .type(ApplicationCommandOption.Type.STRING.getValue())
      .autocomplete(true)
      .required(true)
      .build())
    .build();

  private final DocSearch search;
  private final Cache<String, DocItem> cache = Caffeine.newBuilder()
    .expireAfterWrite(Duration.ofMinutes(5))
    .build();

  public Docs(Pencil pencil) {
    super(pencil);
    this.search = new DocSearch(pencil);
  }

  @Override
  public Mono<Void> execute(ChatInputInteractionEvent event) {
    final String term = event.getOption("term").orElseThrow().getValue().orElseThrow().asString();

    DocItem item = cache.getIfPresent(term);

    if (item == null) {
      return search.search(term).sampleFirst(r -> event.reply(r.url())).then();
    }
    return event.reply(item.url());
  }

  @Override
  public Mono<Void> complete(ChatInputAutoCompleteEvent event) {
    final String term = event.getFocusedOption().getValue().orElseThrow().asString();
    if (term.length() == 0 || term.isBlank()) {
      return event.respondWithSuggestions(Collections.emptyList());
    }

    return this.search.search(term)
      .doOnNext(next -> cache.put(String.valueOf(next.hashCode()), next)) // this is awful, but also...
      .map(item -> ApplicationCommandOptionChoiceData.builder()
        .name(StringUtil.left(item.url(), 100))
        .value(String.valueOf(item.hashCode()))
        .build())
      .cast(ApplicationCommandOptionChoiceData.class)
      .collectList()
      .flatMap(event::respondWithSuggestions);
  }

  @Override
  public ApplicationCommandRequest request() {
    return this.request;
  }

}
