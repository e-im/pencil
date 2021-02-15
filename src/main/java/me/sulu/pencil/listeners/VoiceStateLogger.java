package me.sulu.pencil.listeners;

import me.sulu.pencil.Pencil;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.guild.voice.GuildVoiceJoinEvent;
import net.dv8tion.jda.api.events.guild.voice.GuildVoiceLeaveEvent;
import net.dv8tion.jda.api.events.guild.voice.GuildVoiceMoveEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

import java.time.Instant;

public class VoiceStateLogger extends ListenerAdapter {
  @Override
  public void onGuildVoiceJoin(GuildVoiceJoinEvent event) {
    Pencil.getVoiceLogChannel().sendMessageEmbeds(
      new EmbedBuilder()
        .setAuthor(event.getMember().getUser().getAsTag(), null, event.getMember().getUser().getEffectiveAvatarUrl())
        .setTitle("Joined " + event.getChannelJoined().getName())
        .setDescription(String.format("User Mention: %s\nUser ID: %s\nChannel Mention: %s", event.getMember().getUser().getAsMention(), event.getMember().getUser().getId(), event.getChannelJoined().getAsMention()))
        .setTimestamp(Instant.now())
        .setColor(2792847)
        .build()
    ).queue();
  }

  @Override
  public void onGuildVoiceLeave(GuildVoiceLeaveEvent event) {
    Pencil.getVoiceLogChannel().sendMessageEmbeds(
      new EmbedBuilder()
        .setAuthor(event.getMember().getUser().getAsTag(), null, event.getMember().getUser().getEffectiveAvatarUrl())
        .setTitle("Left " + event.getChannelLeft().getName())
        .setDescription(String.format("User Mention: %s\nUser ID: %s\nChannel Mention: %s", event.getMember().getUser().getAsMention(), event.getMember().getUser().getId(), event.getChannelLeft().getAsMention()))
        .setTimestamp(Instant.now())
        .setColor(15167313)
        .build()
    ).queue();
  }

  @Override
  public void onGuildVoiceMove(GuildVoiceMoveEvent event) {
    Pencil.getVoiceLogChannel().sendMessageEmbeds(
      new EmbedBuilder()
        .setAuthor(event.getMember().getUser().getAsTag(), null, event.getMember().getUser().getEffectiveAvatarUrl())
        .setTitle("Moved from " + event.getChannelLeft().getName() + " to " + event.getChannelJoined().getName())
        .setDescription(String.format("User Mention: %s\nUser ID: %s\nOld Channel Mention: %s\nNew Channel Mention: %s", event.getMember().getUser().getAsMention(), event.getMember().getUser().getId(), event.getChannelLeft().getAsMention(), event.getChannelJoined().getAsMention()))
        .setTimestamp(Instant.now())
        .setColor(15320170)
        .build()
    ).queue();
  }
}
