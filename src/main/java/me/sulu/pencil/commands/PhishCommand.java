package me.sulu.pencil.commands;

import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;
import me.sulu.pencil.Pencil;
import me.sulu.pencil.util.Util;
import net.dv8tion.jda.api.Permission;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class PhishCommand extends Command {
  public static final Logger LOGGER = LogManager.getLogger();

  public PhishCommand() {
    this.name = "phish";
    this.botPermissions = new Permission[]{Permission.MESSAGE_SEND, Permission.MESSAGE_EMBED_LINKS};
    this.userPermissions = new Permission[]{Permission.MESSAGE_MANAGE};
    this.guildOnly = true;
  }

  @Override
  protected void execute(CommandEvent event) {
    try {
      event.getMessage().reply(
        Util.pastebin(
          Pencil.getMapper().writerWithDefaultPrettyPrinter().writeValueAsString(Pencil.getSpamHandler().domains.asMap()),
          "application/json"
        )
      ).queue();
    } catch (Exception e) {
      LOGGER.error("Failed to get phish domains!");
      throw new RuntimeException(e);
    }
  }
}
