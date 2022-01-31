package me.sulu.pencil.commands;

import discord4j.core.event.domain.interaction.ChatInputAutoCompleteEvent;
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.event.domain.interaction.SelectMenuInteractionEvent;
import discord4j.core.object.command.ApplicationCommandOption;
import discord4j.core.object.component.ActionRow;
import discord4j.core.object.component.SelectMenu;
import discord4j.core.spec.EmbedCreateSpec;
import discord4j.core.spec.InteractionApplicationCommandCallbackSpec;
import discord4j.core.spec.MessageEditSpec;
import discord4j.discordjson.json.ApplicationCommandOptionData;
import discord4j.discordjson.json.ApplicationCommandRequest;
import discord4j.rest.util.Color;
import me.sulu.pencil.Pencil;
import me.sulu.pencil.apis.autocomplete.GoogleAutoComplete;
import me.sulu.pencil.apis.customsearch.GoogleCustomSearch;
import me.sulu.pencil.apis.customsearch.entity.Item;
import me.sulu.pencil.apis.customsearch.entity.Result;
import me.sulu.pencil.apis.customsearch.entity.SearchInformation;
import me.sulu.pencil.util.Emojis;
import me.sulu.pencil.util.StringUtil;
import reactor.core.publisher.Mono;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeoutException;

public class Google extends Command {
  private final ApplicationCommandRequest request = ApplicationCommandRequest.builder()
    .name("google")
    .description("Search the web")
    .addOption(ApplicationCommandOptionData.builder()
      .name("term")
      .description("Search term")
      .type(ApplicationCommandOption.Type.STRING.getValue())
      .autocomplete(true)
      .required(true)
      .build())
    .build();

  private final GoogleCustomSearch search;
  private final GoogleAutoComplete completer;

  public Google(Pencil pencil) {
    super(pencil);
    this.search = new GoogleCustomSearch(this.pencil());
    this.completer = new GoogleAutoComplete(this.pencil());
  }

  @Override
  public Mono<Void> execute(ChatInputInteractionEvent event) {
    final String term = event.getOption("term").orElseThrow().getValue().orElseThrow().asString();

    return this.search.search(term)
      .flatMap(result -> {
        if (result.items() == null || result.items().size() == 0) {
          return event.reply(InteractionApplicationCommandCallbackSpec.builder()
            .addEmbed(EmbedCreateSpec.builder()
              .color(Color.RUST)
              .title("No results found for " + term)
              .thumbnail("https://favicon.sulu.me/icon.png")
              .build())
            .build());
        }

        if (result.items().size() == 1) {
          return event.reply(InteractionApplicationCommandCallbackSpec.builder()
            .addEmbed(this.embed(result.items().get(0), result.searchInformation()))
            .build()
          );
        }

        final String selectMenu = Integer.toHexString(result.hashCode());

        Mono<Void> listener = this.pencil().client().on(SelectMenuInteractionEvent.class, menuEvent -> {
            if (menuEvent.getCustomId().equals(selectMenu)) {
              int selection = Integer.parseInt(menuEvent.getValues().get(0));
              return menuEvent.edit(InteractionApplicationCommandCallbackSpec.builder()
                .addEmbed(this.embed(result.items().get(selection), result.searchInformation()))
                .addComponent(ActionRow.of(SelectMenu.of(selectMenu, this.options(result, selection))))
                .build());
            } else {
              return Mono.empty();
            }
          }).timeout(Duration.ofMinutes(2))
          .onErrorResume(TimeoutException.class, ignored -> event.getReply()
            .flatMap(reply -> reply.edit(MessageEditSpec.builder().addAllComponents(Collections.emptyList()).build()))
            .then()
          )
          .then();

        return event.reply(InteractionApplicationCommandCallbackSpec.builder()
            .addEmbed(this.embed(result.items().get(0), result.searchInformation()))
            .addComponent(ActionRow.of(SelectMenu.of(selectMenu, this.options(result, 0))))
            .build())
          .then(listener);
      });
  }

  private EmbedCreateSpec embed(final Item item, final SearchInformation info) {
    return EmbedCreateSpec.builder()
      .color(Color.SEA_GREEN)
      .title(item.title())
      .url(item.link())
      .thumbnail("https://favicon.sulu.me/icon.png?url=" + URLEncoder.encode(item.displayLink(), StandardCharsets.UTF_8).toLowerCase(Locale.ROOT))
      .description("<%s>\n\n%s".formatted(item.link(), item.snippet().replace('\n', ' ')))
      .footer("About %s results (%s seconds)".formatted(info.formattedTotalResults(), info.formattedSearchTime()), null)
      .build();
  }

  private List<SelectMenu.Option> options(Result result, int current) {
    final List<SelectMenu.Option> list = new ArrayList<>();

    for (int i = 0; i < result.items().size(); i++) {
      Item item = result.items().get(i);
      SelectMenu.Option option = SelectMenu.Option.of(StringUtil.left(item.displayLink().replace("www.", "") + " - " + item.title(), 100), Integer.toString(i))
        .withDescription(StringUtil.left(item.snippet(), 70))
        .withEmoji(Emojis.GOOGLE);

      if (i == current) option = option.withDefault(true);

      list.add(option);
    }

    return list;
  }

  @Override
  public Mono<Void> complete(ChatInputAutoCompleteEvent event) {
    String term = event.getFocusedOption().getValue().orElseThrow().asString();
    if (term.length() == 0) {
      return event.respondWithSuggestions(Collections.emptyList());
    }

    return this.completer.complete(term)
      .collectList()
      .flatMap(event::respondWithSuggestions);
  }

  @Override
  public ApplicationCommandRequest request() {
    return this.request;
  }
}
