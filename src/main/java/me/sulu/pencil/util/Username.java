package me.sulu.pencil.util;

import net.dv8tion.jda.api.entities.Member;

import java.text.Normalizer;

public class Username {
  public static boolean normalize(Member m) {
    if (!m.getGuild().getSelfMember().canInteract(m)) return false;

    String newName = Normalizer.normalize(m.getEffectiveName(), Normalizer.Form.NFKC);
    int codePoint = newName.codePointAt(0);

    while (codePoint < 48 && newName.length() > 0) {
      newName = newName.substring(1);
      if (!newName.isEmpty()) codePoint = newName.codePointAt(0);
    }

    if (newName.isBlank()) {
      newName = Util.randomName();
    }
    if (newName.length() > 32) newName = newName.substring(0, 31);
    if (!newName.equals(m.getEffectiveName())) {
      try {
        m.modifyNickname(newName).queue();
      } catch (Exception e) {
        return false;
      }
    }
    return true;
  }
}
