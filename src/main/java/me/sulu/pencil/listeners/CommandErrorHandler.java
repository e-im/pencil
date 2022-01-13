package me.sulu.pencil.listeners;

import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;
import com.jagrosh.jdautilities.command.CommandListener;
import com.jagrosh.jdautilities.command.SlashCommand;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;

import java.time.Instant;

public class CommandErrorHandler implements CommandListener {
  @Override
  public void onCommandException(CommandEvent event, Command command, Throwable th) {
    th.printStackTrace();
    event.getMessage().replyEmbeds(generateEmbed(th)).queue();
  }

  @Override
  public void onSlashCommandException(SlashCommandEvent event, SlashCommand command, Throwable th) {
    th.printStackTrace();
    event.getInteraction().getHook().editOriginalEmbeds(generateEmbed(th)).queue();
  }

  private MessageEmbed generateEmbed(Throwable th) {
    return new EmbedBuilder()
      .setTitle("An Error Occurred")
      .setDescription("```java\n%s: %s```".formatted(th.getClass().getName(), th.getMessage()))
      .setTimestamp(Instant.now())
      .setColor(9966604)
      .build();
  }
}
