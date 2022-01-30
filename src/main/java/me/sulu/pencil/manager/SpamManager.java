package me.sulu.pencil.manager;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import discord4j.common.util.Snowflake;
import discord4j.core.object.entity.Message;
import discord4j.core.object.entity.User;
import discord4j.core.spec.EmbedCreateFields;
import discord4j.core.spec.EmbedCreateSpec;
import discord4j.rest.util.Image;
import me.sulu.pencil.Pencil;
import me.sulu.pencil.apis.safebrowsing.SafeBrowsing;
import me.sulu.pencil.util.Config;
import me.sulu.pencil.util.RegexUtil;
import me.sulu.pencil.util.StringUtil;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.Logger;
import reactor.util.Loggers;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class SpamManager {
  private static final Logger LOGGER = Loggers.getLogger(SpamManager.class);

  private final Pencil pencil;
  private final Set<String> domains = Collections.newSetFromMap(new ConcurrentHashMap<>());
  private final SafeBrowsing safeBrowsing;

  private final Cache<SpamKey, SpamStorage> cache = Caffeine.newBuilder()
    .expireAfterAccess(Duration.ofSeconds(10))
    .build();

  public SpamManager(Pencil pencil) {
    this.pencil = pencil;
    this.safeBrowsing = new SafeBrowsing(pencil);

    final Mono<Void> websocket = this.pencil.http()
      .headers(headers -> headers.add("X-Identity", "Pencil Discord Bot (PaperMC)"))
      .websocket()
      .uri("wss://phish.sinking.yachts/feed")
      .receive()
      .asByteArray()
      .doOnNext(data -> {
        try {
          SinkingYachtsUpdate update = this.pencil.jsonMapper().readValue(data, SinkingYachtsUpdate.class);
          LOGGER.info("Got update to {} {} domain(s) from sinking.yachts containing {}", update.type(), update.domains().size(), update.domains());
          if (update.type() == SinkingYachtsUpdate.SinkingYachtsUpdateType.DELETE) {
            this.domains.removeAll(update.domains());
          } else {
            this.domains.addAll(update.domains());
          }
        } catch (Exception e) {
          LOGGER.warn("Failed to handle update from sinking.yachts", e);
        }
      })
      .then();

    this.pencil.http()
      .headers(headers -> headers.add("X-Identity", "Pencil Discord Bot (PaperMC)"))
      .get()
      .uri("https://phish.sinking.yachts/v2/text")
      .responseContent()
      .aggregate()
      .asString()
      .doOnNext(data -> this.domains.addAll(List.of(data.split("\n"))))
      .then(websocket)
      .subscribe();
  }

  public Mono<Void> handle(Message message) {
    return check(message)
      .flatMap(response -> {
        if (response.status() != SpamResponse.SpamStatus.BAD && response.status() != SpamResponse.SpamStatus.POSSIBLE) {
          return Mono.empty();
        }
        return caught(message, response);
      });
  }

  private Mono<SpamResponse> check(Message message) {
    if (message.getGuildId().isEmpty() || message.getAuthor().isPresent() && message.getAuthor().get().isBot()) {
      return Mono.empty();
    }

    Map<String, String> found = RegexUtil.urlsAndDomains(message.getContent());

    return Flux.fromIterable(found.entrySet())
      .flatMap(entry -> {
        if (this.pencil.config().global().domainAllowlist().contains(entry.getValue())) {
          return Mono.just(new SpamResponse(SpamResponse.SpamStatus.WHITELISTED, "Allowlisted domain: `" + entry.getValue() + "`"));
        }

        if (this.domains.contains(entry.getValue())) {
          return Mono.just(new SpamResponse(SpamResponse.SpamStatus.BAD, "Blocklisted domain: `" + entry.getKey() + "`"));
        }

        return this.safeBrowsing.check(entry.getKey())
          .map(safe -> {
            if (safe.phishing()) {
              return new SpamResponse(SpamResponse.SpamStatus.BAD, "Unsafe url (GSB): `" + entry.getKey() + "`");
            }

            final SpamKey key = new SpamKey(message.getUserData().id().asLong(), entry.getValue());
            SpamStorage spam = this.cache.getIfPresent(key);

            Config.GuildConfig.Features.Phishing.DomainSpam domainSpam = this.pencil.config()
              .guild(message.getGuildId().get())
              .features().phishing().domainSpam();

            if (spam == null) {
              this.cache.put(key, new SpamStorage(new HashSet<>(Set.of(message.getChannelId().asLong())), new HashSet<>(Set.of(message))));
            } else if (
              spam.channels().size()
                + (spam.channels().contains(message.getChannelId().asLong()) ? 0 : 1)
                >= domainSpam.channels()
                || spam.messages().size() >= domainSpam.uniques() - 1
            ) {
              spam.messages().forEach(msg -> msg.delete("Domain posted too frequently").subscribe());
              this.cache.invalidate(key);
              return new SpamResponse(SpamResponse.SpamStatus.POSSIBLE, "Domain `" + entry.getValue() + "` posted very frequently.");
            } else {
              spam.messages().add(message);
              spam.channels.add(message.getChannelId().asLong());
              this.cache.put(key, spam);
            }

            return new SpamResponse(SpamResponse.SpamStatus.UNKNOWN, "Nothing found");
          });
      })
      .collectList()
      .map(all -> {
        for (SpamResponse response : all) {
          if (response.status == SpamResponse.SpamStatus.BAD || response.status == SpamResponse.SpamStatus.POSSIBLE) {
            return response;
          }
        }

        return all.size() == 0
          ? new SpamResponse(SpamResponse.SpamStatus.UNKNOWN, "No urls present")
          : all.get(0);
      });
  }

  private Mono<Void> caught(Message message, SpamResponse reason) {
    if (message.getGuildId().isEmpty()) {
      return Mono.empty();
    }

    Mono<Void> delete = message.delete("Possible spam");

    if (message.getAuthor().isPresent()) {
      final User user = message.getAuthor().get();
      return delete.and(message.getAuthorAsMember()
        .flatMap(member -> member.getGuild()
          .flatMap(guild -> member.getPrivateChannel()
            .flatMap(channel -> channel.createMessage(EmbedCreateSpec.builder()
                .title("Are you a robot?")
                .thumbnail(guild.getIconUrl(Image.Format.PNG).orElse(""))
                .description("You have been automatically kicked for bot-like activity. Please ensure your Discord account is secure.")
                .addFields(EmbedCreateFields.Field.of("Message", "```\n%s\n```".formatted(StringUtil.left(message.getContent().replace("```", ""), 500)), false))
                .addFields(EmbedCreateFields.Field.of("Reason", reason.reason(), false))
                .build()
              )
            )
            .then(member.kick("Possible spam"))
          )
        )
        .and(this.pencil.client().rest()
          .getChannelById(Snowflake.of(this.pencil.config().guild(message.getGuildId().get()).channels().autoActionLog()))
          .createMessage(EmbedCreateSpec.builder()
            .author(user.getTag(), null, user.getAvatarUrl())
            .title("User kicked for possible spam")
            .addField("Last Message", "```\n%s\n```".formatted(StringUtil.left(message.getContent(), 500).replace("```", "")), false)
            .addField("Reason", reason.reason(), false)
            .addField("Mention", user.getMention(), false)
            .build()
            .asRequest()
          )
        ));
    }

    return delete;
  }


  record SinkingYachtsUpdate(
    SinkingYachtsUpdateType type,
    Set<String> domains
  ) {
    enum SinkingYachtsUpdateType {
      ADD,
      DELETE
    }
  }

  record SpamResponse(
    SpamStatus status,
    String reason
  ) {
    enum SpamStatus {
      BAD,
      POSSIBLE,
      UNKNOWN,
      WHITELISTED
    }
  }

  record SpamStorage(
    Set<Long> channels,
    Set<Message> messages
  ) {
  }

  record SpamKey(
    long user,
    String domain
  ) {
  }
}
