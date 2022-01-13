package me.sulu.pencil.util;

import me.sulu.pencil.Pencil;
import net.dv8tion.jda.api.entities.User;

import java.util.List;

public class Modmail {
  public static boolean send(String message, User user, String footer) {
    try {
      List<String> msgs = Util.split(message, 2000);
      Pencil.getBatcave().sendMessage(
        String.format(
          "**New message from `%s` - %s (`%s`) - %s:**",
          user.getAsTag(),
          user.getAsMention(),
          user.getId(),
          footer
        )
      ).queue();
      for (String msg : msgs) {
        Pencil.getBatcave().sendMessage(msg).queue();
      }
    } catch (Exception e) {
      return false;
    }
    return true;
  }
}
