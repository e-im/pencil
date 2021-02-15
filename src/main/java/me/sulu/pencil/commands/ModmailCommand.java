package me.sulu.pencil.commands;

import com.jagrosh.jdautilities.command.SlashCommand;
import me.sulu.pencil.util.Modmail;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;

import java.util.Collections;
import java.util.Objects;


public class ModmailCommand extends SlashCommand {
  public ModmailCommand() {
    this.name = "modmail";
    this.arguments = "[message]";
    this.help = "Send the moderators a private message.";
    this.options = Collections.singletonList(
      new OptionData(OptionType.STRING, "message", "Message to send").setRequired(true)
    );
  }

  @Override
  protected void execute(SlashCommandEvent event) {
    String message = Objects.requireNonNull(event.getOption("message")).getAsString();
    if (Modmail.send(message, event.getUser(), "Sent via the modmail command")) {
      event.reply("Your message has been sent.").setEphemeral(true).queue();
    } else {
      event.reply("Your message failed send. Please contact a moderator directly.").setEphemeral(true).queue();
    }
  }
}
