package me.sulu.pencil.listeners;

import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.event.domain.message.MessageUpdateEvent;
import me.sulu.pencil.Pencil;
import me.sulu.pencil.manager.SpamManager;
import reactor.core.Disposable;
import reactor.core.Disposables;
import reactor.core.publisher.Mono;

public class SpamListener extends Listener {
  private final SpamManager manager;

  public SpamListener(Pencil pencil) {
    super(pencil);
    this.manager = new SpamManager(pencil);
  }

  public Mono<Void> on(MessageCreateEvent event) {
    if (event.getGuildId().isEmpty()) return Mono.empty();
    return this.manager.handle(event.getMessage(), false);
  }

  public Mono<Void> on(MessageUpdateEvent event) {
    if (!event.isContentChanged() || event.getGuildId().isEmpty()) return Mono.empty();
    return event.getMessage().flatMap(this.manager::handle);
  }

  @Override
  public Disposable start() {
    return Disposables.composite(
      this.on(MessageCreateEvent.class, this::on).subscribe(),
      this.on(MessageUpdateEvent.class, this::on).subscribe()
    );
  }
}
