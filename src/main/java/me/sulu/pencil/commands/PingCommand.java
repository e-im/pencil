package me.sulu.pencil.commands;

import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;

public class PingCommand extends Command {
  public PingCommand() {
    this.name = "ping";
    this.botPermissions = new Permission[]{Permission.MESSAGE_WRITE, Permission.MESSAGE_EMBED_LINKS};
    this.guildOnly = true;
  }

  @Override
  protected void execute(CommandEvent event) {
    long time = event.getMessage().getTimeCreated().toInstant().toEpochMilli();
    event.getMessage().replyEmbeds(new EmbedBuilder().setTitle("pong").build()).queue(m -> m.editMessageEmbeds(new EmbedBuilder().setTitle(":ping_pong: " + (m.getTimeCreated().toInstant().toEpochMilli() - time) + "ms\n:heartpulse: " + event.getJDA().getGatewayPing() + "ms").setFooter("Requested by " + event.getAuthor().getName(), event.getAuthor().getEffectiveAvatarUrl()).build()).queue());
  }
}
