package me.sulu.pencil.listeners;

import discord4j.core.event.domain.guild.GuildCreateEvent;
import me.sulu.pencil.Pencil;
import reactor.core.publisher.Mono;
import reactor.util.Logger;
import reactor.util.Loggers;

public class GuildListener {
  private static final Logger LOGGER = Loggers.getLogger(GuildListener.class);

  private final Pencil pencil;

  public GuildListener(final Pencil pencil) {
    this.pencil = pencil;
    this.pencil.on(GuildCreateEvent.class, this::on).subscribe();
  }

  private Mono<Void> on(GuildCreateEvent event) {
    LOGGER.info("Bot joined new guild {} ({}) with {} members",
      event.getGuild().getName(),
      event.getGuild().getId().asString(),
      event.getGuild().getMemberCount()
    );

    if (!this.pencil.config().hasGuild(event.getGuild())) {
      LOGGER.warn("Leaving guild {} ({}) as no config is present!", event.getGuild().getName(), event.getGuild().getId().asString());

      return event.getGuild().leave();
    }

    return Mono.empty();
  }
}
