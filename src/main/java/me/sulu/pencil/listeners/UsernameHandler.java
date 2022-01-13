package me.sulu.pencil.listeners;

import me.sulu.pencil.util.Username;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.events.guild.member.GuildMemberJoinEvent;
import net.dv8tion.jda.api.events.guild.member.GuildMemberUpdateEvent;
import net.dv8tion.jda.api.events.guild.member.update.GuildMemberUpdateNicknameEvent;
import net.dv8tion.jda.api.events.user.update.UserUpdateNameEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

public class UsernameHandler extends ListenerAdapter {

  @Override
  public void onGuildMemberJoin(GuildMemberJoinEvent event) {
    Username.normalize(event.getMember());
  }

  @Override
  public void onGuildMemberUpdateNickname(GuildMemberUpdateNicknameEvent event) {
    Username.normalize(event.getMember());
  }

  @Override
  public void onUserUpdateName(UserUpdateNameEvent event) {
    for (Guild g : event.getUser().getMutualGuilds()) {
      Member m = g.getMemberById(event.getUser().getId());
      if (m != null) Username.normalize(m);
    }
  }

  @Override
  public void onGuildMemberUpdate(GuildMemberUpdateEvent event) {
    Username.normalize(event.getMember());
  }
}
