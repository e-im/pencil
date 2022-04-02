package me.sulu.pencil.commands;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import discord4j.core.event.domain.interaction.ChatInputAutoCompleteEvent;
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.object.command.ApplicationCommandOption;
import discord4j.core.object.command.ApplicationCommandOptionChoice;
import discord4j.core.object.component.SelectMenu;
import discord4j.core.spec.EmbedCreateSpec;
import discord4j.discordjson.json.ApplicationCommandOptionChoiceData;
import discord4j.discordjson.json.ApplicationCommandOptionData;
import discord4j.discordjson.json.ApplicationCommandRequest;
import discord4j.rest.util.Color;
import me.sulu.pencil.Pencil;
import me.sulu.pencil.apis.docs.DocItem;
import me.sulu.pencil.apis.docs.DocSearch;
import me.sulu.pencil.util.StringUtil;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Docs extends Command{
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
      return search.search(term).flatMap(result -> {
        if (result.length == 0) return event.reply("No results found for " + term);
        return event.reply(result[0].url());
      });
    }

    return event.reply(item.url());
  }

  @Override
  public Mono<Void> complete(ChatInputAutoCompleteEvent event) {
    final String term = event.getFocusedOption().getValue().orElseThrow().asString();
    if (term.length() == 0 || term.isBlank()) {
      return event.respondWithSuggestions(Collections.emptyList());
    }

    return this.search.search(term).flatMap(result -> {
      if (result.length == 0) return event.respondWithSuggestions(Collections.emptyList());
      final List<ApplicationCommandOptionChoiceData> choices = new ArrayList<>();

      for (DocItem item : result) {
        choices.add(ApplicationCommandOptionChoiceData.builder()
          .name(StringUtil.left(item.name() + " - " + item.content(), 100))
          .value(item.id())
          .build());

        cache.put(item.id(), item);
      }

      return event.respondWithSuggestions(choices);
    });

  }

  @Override
  public ApplicationCommandRequest request() {
    return this.request;
  }

}
