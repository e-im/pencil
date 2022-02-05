package me.sulu.pencil.commands;

import discord4j.core.event.domain.interaction.ChatInputAutoCompleteEvent;
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.discordjson.json.ApplicationCommandRequest;
import me.sulu.pencil.Pencil;
import reactor.core.publisher.Mono;

import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;

public abstract class Command {
  private final Pencil pencil;

  public Command(final Pencil pencil) {
    this.pencil = pencil;
  }

  protected Pencil pencil() {
    return this.pencil;
  }

  public abstract Mono<Void> execute(ChatInputInteractionEvent event);

  public Mono<Void> complete(ChatInputAutoCompleteEvent event) {
    return event.respondWithSuggestions(Collections.emptyList());
  }

  public abstract ApplicationCommandRequest request();

  public boolean global() {
    return false;
  }

  public Set<Long> guilds() {
    return this.pencil.config().guilds().stream()
      .filter(id -> this.pencil.config().command(id, this.request().name()))
      .collect(Collectors.toUnmodifiableSet());
  }
}
