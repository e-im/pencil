package me.sulu.pencil.listeners;

import discord4j.core.event.domain.guild.GuildCreateEvent;
import me.sulu.pencil.Pencil;
import reactor.core.Disposable;
import reactor.core.publisher.Mono;
import reactor.util.Logger;
import reactor.util.Loggers;

public class GuildListener extends Listener {
  private static final Logger LOGGER = Loggers.getLogger(GuildListener.class);

  public GuildListener(final Pencil pencil) {
    super(pencil);
  }

  private Mono<Void> on(GuildCreateEvent event) {
    LOGGER.info("Bot joined new guild {} ({}) with {} members",
      event.getGuild().getName(),
      event.getGuild().getId().asString(),
      event.getGuild().getMemberCount()
    );

    if (!this.pencil().config().hasGuild(event.getGuild())) {
      LOGGER.warn("Leaving guild {} ({}) as no config is present!", event.getGuild().getName(), event.getGuild().getId().asString());

      return event.getGuild().leave();
    }

    return Mono.empty();
  }

  @Override
  public Disposable start() {
    return this.on(GuildCreateEvent.class, this::on).subscribe();
  }
}
