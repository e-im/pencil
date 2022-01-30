package me.sulu.pencil.listeners;

import discord4j.common.util.Snowflake;
import discord4j.core.event.domain.message.MessageCreateEvent;
import me.sulu.pencil.Pencil;
import reactor.core.publisher.Mono;

public class DirectMessageListener {
  private final Pencil pencil;

  public DirectMessageListener(Pencil pencil) {
    this.pencil = pencil;
    this.pencil.client().on(MessageCreateEvent.class, this::on).subscribe();
  }

  private Mono<Void> on(MessageCreateEvent event) {
    if (event.getGuildId().isPresent() || event.getMessage().getAuthor().isEmpty() || event.getMessage().getAuthor().get().isBot())
      return Mono.empty();

    return event.getMessage().getRestChannel().createMessage("Hey " + event.getMessage().getAuthor().get().getMention() + "! " +
        "My private messages are not actively watched. Please use the `/modmail` command from within a guild. " +
        "Don't worry! Your message will not be shown to the public.")
      .then(this.pencil.client().rest().getChannelById(Snowflake.of(this.pencil.config().global().debug().dm()))
        .createMessage(event.getMessage().getAuthor().get().getTag() + ": " + event.getMessage().getContent()))
      .then();
  }
}
