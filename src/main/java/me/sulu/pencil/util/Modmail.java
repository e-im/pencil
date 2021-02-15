package me.sulu.pencil.util;

import com.google.common.base.Splitter;
import com.google.common.collect.Iterables;
import me.sulu.pencil.Pencil;
import net.dv8tion.jda.api.entities.User;

public class Modmail {
  public static boolean send(String message, User user, String footer) {
    try {
      Iterable<String> result = Splitter.fixedLength(2000).split(message);
      String[] msgs = Iterables.toArray(result, String.class);
      Pencil.getModMailChannel().sendMessage(
        String.format(
          "**New message from `%s` - %s (`%s`) - %s:**",
          user.getAsTag(),
          user.getAsMention(),
          user.getId(),
          footer
        )
      ).queue();
      for (String msg : msgs) {
        Pencil.getModMailChannel().sendMessage(msg).queue();
      }
    } catch (Exception e) {
      return false;
    }
    return true;
  }
}
