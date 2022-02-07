package me.sulu.pencil.listeners;

import discord4j.common.util.Snowflake;
import discord4j.core.event.domain.message.MessageCreateEvent;
import me.sulu.pencil.Pencil;
import reactor.core.Disposable;
import reactor.core.publisher.Mono;

public class DirectMessageListener extends Listener {

  public DirectMessageListener(Pencil pencil) {
    super(pencil);
  }

  private Mono<Void> on(MessageCreateEvent event) {
    if (event.getGuildId().isPresent() || event.getMessage().getAuthor().isEmpty() || event.getMessage().getAuthor().get().isBot()) {
      return Mono.empty();
    }

    return event.getMessage().getRestChannel().createMessage("Hey " + event.getMessage().getAuthor().get().getMention() + "! " +
        "My private messages are not actively watched. Please use the `/modmail` command from within a guild. " +
        "Don't worry! Your message will not be shown to the public.")
      .and(this.pencil().client().rest().getChannelById(Snowflake.of(this.pencil().config().global().debug().dm()))
        .createMessage(event.getMessage().getAuthor().get().getTag() + ": " + event.getMessage().getContent())
      );
  }

  @Override
  public Disposable start() {
    return this.on(MessageCreateEvent.class, this::on).subscribe();
  }
}
