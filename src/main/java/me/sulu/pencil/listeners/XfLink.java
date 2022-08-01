package me.sulu.pencil.listeners;

import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.object.entity.Attachment;
import me.sulu.pencil.Pencil;
import me.sulu.pencil.util.Config.GuildConfig;
import reactor.core.Disposable;
import reactor.core.publisher.Mono;
import reactor.util.Logger;
import reactor.util.Loggers;

public class XfLink extends Listener {
  private static final Logger LOGGER = Loggers.getLogger(GuildListener.class);

  public XfLink(final Pencil pencil) {
    super(pencil);
  }

  private Mono<Void> on(final MessageCreateEvent event) {
    if (event.getGuildId().isEmpty() || !pencil().config().hasGuild(event.getGuildId().get()) || event.getMessage().getAuthor().isEmpty() || event.getMessage().getAuthor().get().isBot()) {
      return Mono.empty();
    }

    final GuildConfig.Features.XfLink config = pencil().config().guild(event.getGuildId().get()).features().xfLink();
    if (!event.getMessage().getChannelId().asString().equals(config.channelId())) {
      return Mono.empty();
    }

    final StringBuilder message = new StringBuilder().append(event.getMessage().getContent());

    for (final Attachment attachment : event.getMessage().getAttachments()) {
      if (attachment.getContentType().isPresent() && attachment.getContentType().get().contains("image")) {
        message.append("\n![](");
        message.append(attachment.getUrl());
        message.append(")");
        continue;
      }

      message.append("\n");
      message.append(attachment.getUrl());
    }

    return pencil().http()
      .headers(header -> header.add("XF-Api-Key", config.xfApiKey()))
      .post().uri(config.xfBaseUrl() + "/api/posts/")
      .sendForm((req, form) -> {
        form.attr("thread_id", String.valueOf(config.threadId()));
        form.attr("message", message.toString());
      })
      .response()
      .then();
  }

  @Override
  public Disposable start() {
    return this.on(MessageCreateEvent.class, this::on).subscribe();
  }
}
