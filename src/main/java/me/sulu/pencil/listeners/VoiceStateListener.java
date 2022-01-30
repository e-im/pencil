package me.sulu.pencil.listeners;

import discord4j.common.util.Snowflake;
import discord4j.core.event.domain.VoiceStateUpdateEvent;
import discord4j.core.spec.EmbedCreateSpec;
import discord4j.rest.entity.RestChannel;
import discord4j.rest.util.Color;
import me.sulu.pencil.Pencil;
import reactor.core.publisher.Mono;

public class VoiceStateListener {
  private final Pencil pencil;

  public VoiceStateListener(Pencil pencil) {
    this.pencil = pencil;
    this.pencil.client().on(VoiceStateUpdateEvent.class, this::on).subscribe();
  }

  private Mono<Void> on(VoiceStateUpdateEvent event) {
    long voiceLogId = this.pencil.config().guild(event.getCurrent().getGuildId()).channels().voiceLog();
    if (voiceLogId == 0L) return Mono.empty();

    final RestChannel logChannel = this.pencil.client().rest().getChannelById(Snowflake.of(voiceLogId));

    return event.getCurrent().getUser()
      .flatMap(user -> {
        EmbedCreateSpec.Builder builder = EmbedCreateSpec.builder()
          .author(user.getTag(), null, user.getAvatarUrl())
          .addField("User Mention", user.getMention(), false);

        if (event.isLeaveEvent()) {
          return event.getOld().get().getChannel()
            .map(old -> builder.title("Left " + old.getName())
              .color(Color.RUST)
              .addField("Channel mention", old.getMention(), false)
              .build()
              .asRequest()
            )
            .flatMap(logChannel::createMessage);
        }

        return event.getCurrent().getChannel()
          .flatMap(current -> {
            if (event.isJoinEvent()) {
              return logChannel.createMessage(builder.title("Joined " + current.getName())
                .addField("Channel Mention", current.getMention(), false)
                .color(Color.ENDEAVOUR)
                .build()
                .asRequest()
              );
            } else if (event.isMoveEvent() && event.getOld().isPresent()) {
              return event.getOld().get().getChannel()
                .map(oldChannel -> builder.title("Moved from " + oldChannel.getName() + " to " + current.getName())
                  .color(Color.MOON_YELLOW)
                  .addField("New Channel Mention", current.getMention(), false)
                  .addField("Old Channel Mention", oldChannel.getMention(), false)
                  .build()
                  .asRequest()
                )
                .flatMap(logChannel::createMessage);
            }

            return Mono.empty();
          });
      })
      .then();
  }
}
