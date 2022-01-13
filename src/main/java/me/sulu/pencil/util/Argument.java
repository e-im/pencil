package me.sulu.pencil.util;

import me.sulu.pencil.Pencil;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.User;

public class Argument {
  public static Member member(String string, Guild guild) {
    Member member = null;
    try {
      member = guild.retrieveMemberById(string).complete();
    } catch (Exception ignored) {
    }
    if (member == null) try {
      guild.retrieveMemberById(idFromUserMention(string)).complete();
    } catch (Exception ignored) {
    }
    if (member == null) try {
      guild.getMemberByTag(string);
    } catch (Exception ignored) {
    }
    return member;
  }

  public static User user(String string) {
    User user = null;
    try {
      user = Pencil.getJDA().retrieveUserById(string).complete();
    } catch (Exception ignored) {
    }
    if (user == null) try {
      user = Pencil.getJDA().retrieveUserById(idFromUserMention(string)).complete();
    } catch (Exception ignored) {
    }
    if (user == null) try {
      user = Pencil.getJDA().getUserByTag(string);
    } catch (Exception ignored) {
    }
    return user;
  }

  private static String idFromUserMention(String mention) {
    return mention.replaceAll("\\D", "");
  }
}
