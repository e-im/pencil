package me.sulu.pencil.commands;

import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;
import me.sulu.pencil.util.Argument;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.User;
import org.apache.commons.lang3.exception.ExceptionUtils;

public class DmCommand extends Command {
  public DmCommand() {
    this.name = "dm";
    this.aliases = new String[]{"modreply"};
    this.arguments = "<user> <message>";
    this.help = "Sends a message to the specified user as the bot.";
    this.guildOnly = true;
    this.userPermissions = new Permission[]{Permission.MESSAGE_MANAGE};
    this.botPermissions = new Permission[]{Permission.MESSAGE_WRITE};
  }

  @Override
  protected void execute(CommandEvent event) {
    String[] args = event.getArgs().split(" ", 2);
    if (args.length < 2) {
      event.getMessage().reply("You must supply both a user and message to " +
        "send.").queue();
      return;
    }
    User user = Argument.user(args[0]);

    if (user == null) {
      event.getMessage().reply(String.format("Could not convert %s to user.",
        args[0])).queue();
      return;
    }

    user.openPrivateChannel().queue(
      (channel) -> channel.sendMessage(args[1]).queue(
        (message) -> this.success(event.getMessage(), args[1], user),
        (error) -> this.fail(event.getMessage(), error)
      ),
      (error) -> this.fail(event.getMessage(), error)
    );
  }

  private void fail(Message m, Throwable t) {
    m.reply(String.format(
      "```java\n%s```",
      ExceptionUtils.getMessage(t)
    )).queue();
  }

  protected void success(Message m, String content, User user) {
    m.reply(String.format("Successfully sent ```\n%s``` to %s", content, user.getAsMention())).queue();
  }
}
