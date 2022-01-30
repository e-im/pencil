package me.sulu.pencil.listeners;

import discord4j.common.util.Snowflake;
import discord4j.core.event.domain.VoiceStateUpdateEvent;
import discord4j.core.object.entity.channel.TextChannel;
import discord4j.core.spec.EmbedCreateSpec;
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

    return event.getCurrent().getGuild()
      .flatMap(guild -> guild.getChannelById(Snowflake.of(voiceLogId)))
      .cast(TextChannel.class)
      .zipWith(event.getCurrent().getUser())
      .flatMap(tuple -> {
        EmbedCreateSpec.Builder builder = EmbedCreateSpec.builder()
          .author(tuple.getT2().getTag(), null, tuple.getT2().getAvatarUrl())
          .addField("User Mention", tuple.getT2().getMention(), false)
          .addField("Channel Mention", tuple.getT2().getMention(), false);

        if (event.isLeaveEvent()) {
          return event.getOld().get().getChannel()
            .map(old -> builder.title("Left " + old.getName())
              .color(Color.RUST)
              .build())
            .flatMap(tuple.getT1()::createMessage);
        }

        return event.getCurrent().getChannel()
          .flatMap(current -> {
            if (event.isJoinEvent()) {
              return tuple.getT1().createMessage(builder.title("Joined " + current.getName())
                .color(Color.ENDEAVOUR)
                .build());
            } else if (event.isMoveEvent() && event.getOld().isPresent()) {
              return event.getOld().get().getChannel()
                .map(oldChannel -> builder.title("Moved from " + oldChannel.getName() + " to " + current.getName())
                  .color(Color.MOON_YELLOW)
                  .addField("Old Channel Mention", oldChannel.getMention(), false)
                  .build())
                .flatMap(tuple.getT1()::createMessage);
            }

            return Mono.empty();
          });
      })
      .then();
  }
}
