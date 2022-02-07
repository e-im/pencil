package me.sulu.pencil.listeners;

import com.anyascii.AnyAscii;
import discord4j.core.event.domain.guild.MemberChunkEvent;
import discord4j.core.event.domain.guild.MemberJoinEvent;
import discord4j.core.event.domain.guild.MemberUpdateEvent;
import discord4j.core.object.entity.Member;
import discord4j.core.spec.GuildMemberEditSpec;
import me.sulu.pencil.Pencil;
import me.sulu.pencil.util.Config.GuildConfig.Features.NameNormalization;
import me.sulu.pencil.util.StringUtil;
import reactor.core.Disposable;
import reactor.core.Disposables;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.text.Normalizer;

public class UserChangeListener extends Listener {

  public UserChangeListener(Pencil pencil) {
    super(pencil);
  }

  private Mono<Void> on(MemberChunkEvent event) {
    return Flux.fromIterable(event.getMembers())
      .flatMap(this::normalize)
      .then();
  }

  private Mono<Void> on(MemberJoinEvent event) {
    return this.normalize(event.getMember());
  }

  private Mono<Void> on(MemberUpdateEvent event) {
    return event.getMember().flatMap(this::normalize);
  }

  private Mono<Void> normalize(Member member) {
    NameNormalization config = this.pencil().config().guild(member.getGuildId()).features().nameNormalization();

    String newName = member.getDisplayName();

    if (config.normalize()) {
      newName = Normalizer.normalize(newName, Normalizer.Form.NFKC);
    }

    if (config.aggressive()) {
      newName = AnyAscii.transliterate(newName);
    }

    if (config.dehoist()) {
      int codePoint = newName.codePointAt(0);

      while (codePoint < 48 && newName.length() > 0) {
        newName = newName.substring(1);
        if (!newName.isEmpty()) codePoint = newName.codePointAt(0);
      }
    }

    if (newName.isEmpty()) {
      newName = StringUtil.randomName();
    }

    newName = StringUtil.left(newName, 32);

    if (newName.equals(member.getDisplayName())) return Mono.empty();

    return member.edit(GuildMemberEditSpec.builder()
        .nicknameOrNull(newName)
        .build())
      .then();
  }

  @Override
  public Disposable start() {
    return Disposables.composite(
      this.on(MemberChunkEvent.class, this::on).subscribe(),
      this.on(MemberJoinEvent.class, this::on).subscribe(),
      this.on(MemberUpdateEvent.class, this::on).subscribe()
    );
  }
}
