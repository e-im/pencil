package me.sulu.pencil.listeners;

import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.event.domain.message.MessageUpdateEvent;
import me.sulu.pencil.Pencil;
import me.sulu.pencil.manager.SpamManager;
import reactor.core.publisher.Mono;

public class SpamListener {
  private final SpamManager manager;

  public SpamListener(Pencil pencil) {
    this.manager = new SpamManager(pencil);
    pencil.on(MessageCreateEvent.class, this::on).subscribe();
    pencil.on(MessageUpdateEvent.class, this::on).subscribe();
  }

  public Mono<Void> on(MessageCreateEvent event) {
    if (event.getGuildId().isEmpty()) return Mono.empty();
    return this.manager.handle(event.getMessage());
  }

  public Mono<Void> on(MessageUpdateEvent event) {
    if (!event.isContentChanged() || event.getGuildId().isEmpty()) return Mono.empty();
    return event.getMessage().flatMap(this.manager::handle);
  }
}
