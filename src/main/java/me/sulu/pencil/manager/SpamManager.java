package me.sulu.pencil.manager;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import discord4j.common.util.Snowflake;
import discord4j.core.object.entity.Message;
import discord4j.core.object.entity.User;
import discord4j.core.spec.EmbedCreateFields;
import discord4j.core.spec.EmbedCreateSpec;
import discord4j.rest.util.Image;
import me.sulu.pencil.Pencil;
import me.sulu.pencil.apis.antifish.AntiFishMatch;
import me.sulu.pencil.apis.antifish.AntiFishType;
import me.sulu.pencil.apis.safebrowsing.SafeBrowsing;
import me.sulu.pencil.util.Config;
import me.sulu.pencil.util.RegexUtil;
import me.sulu.pencil.util.StringUtil;
import me.sulu.pencil.apis.antifish.AntiFish;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.Logger;
import reactor.util.Loggers;

import java.time.Duration;
import java.util.*;

public class SpamManager {
  private static final Logger LOGGER = Loggers.getLogger(SpamManager.class);
  private final Pencil pencil;
  private final SafeBrowsing safeBrowsing;
  private final AntiFish antiFish;

  private final Cache<SpamKey, SpamStorage> cache = Caffeine.newBuilder()
    .expireAfterAccess(Duration.ofSeconds(10))
    .build();

  public SpamManager(Pencil pencil) {
    this.pencil = pencil;
    this.safeBrowsing = new SafeBrowsing(pencil);
    this.antiFish = new AntiFish(pencil);
  }

  public Mono<Void> handle(final Message message) {
    return this.handle(message, true);
  }

  public Mono<Void> handle(final Message message, final boolean edit) {
    if (message.getAuthor().isPresent() && !message.getAuthor().get().isBot() && message.getData().nonce().isAbsent()) {
      LOGGER.debug("Message {} sent (edit? {}) by {} ({}) with content {} in channel {} of guild {} has no nonce",
        message.getId().asString(),
        edit,
        message.getAuthor().get().getTag(),
        message.getAuthor().get().getId().asString(),
        message.getContent(),
        message.getChannelId().asString(),
        message.getGuildId().orElse(Snowflake.of(0)));
    }

    return check(message)
      .flatMap(response -> {
        if (response.classification() != SpamResponse.Classification.BAD) {
          return Mono.empty();
        }
        return caught(message, response);
      });
  }

  private Mono<SpamResponse> check(Message message) {
    if (message.getGuildId().isEmpty() || message.getAuthor().isPresent() && message.getAuthor().get().isBot()) {
      return Mono.empty();
    }


    final List<String> urls = RegexUtil.urls(message.getContent());

    return this.antiFish.check(String.join(" ", urls)).flatMap(fish -> {
      if (fish.match() && fish.matches() != null && fish.matches().size() != 0) {
        final AntiFishMatch match = fish.matches().get(0);
        try {
          final String matchJson = this.pencil.jsonMapper().writerWithDefaultPrettyPrinter().writeValueAsString(match);
          return Mono.just(new SpamResponse(match.trustRating() > 0.9 ? SpamResponse.Classification.BAD : SpamResponse.Classification.POSSIBLE,
            match.type() == AntiFishType.IP_LOGGER ? SpamResponse.Type.IP_LOGGER : SpamResponse.Type.PHISHING,
            "```json\n" + StringUtil.left(matchJson, 498) + "\n```"));
        } catch (JsonProcessingException e) {
          return Mono.error(e);
        }
      }

      return Flux.fromIterable(urls)
        .flatMap(url -> this.safeBrowsing.check(url)
          .map(safe -> {
            if (safe.phishing()) {
              return new SpamResponse(SpamResponse.Classification.BAD, SpamResponse.Type.PHISHING, "GSB Phishing URL: `" + url + "`");
            }

            final SpamKey key = new SpamKey(message.getUserData().id().asLong(), RegexUtil.domainFromUrl(url));
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
              return new SpamResponse(SpamResponse.Classification.POSSIBLE, SpamResponse.Type.UNKNOWN, "Domain `" + RegexUtil.domainFromUrl(url) + "` posted very frequently.");
            } else {
              spam.messages().add(message);
              spam.channels().add(message.getChannelId().asLong());
              this.cache.put(key, spam);
            }

            return new SpamResponse(SpamResponse.Classification.UNKNOWN, SpamResponse.Type.UNKNOWN, "Nothing found");
          }))
        .collectList()
        .map(all -> {
          for (SpamResponse response : all) {
            if (response.classification() == SpamResponse.Classification.BAD || response.classification() == SpamResponse.Classification.POSSIBLE) {
              return response;
            }
          }

          return all.size() == 0
            ? new SpamResponse(SpamResponse.Classification.UNKNOWN, SpamResponse.Type.UNKNOWN, "No urls present")
            : all.get(0);
        });
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
                .description("You have been automatically kicked for malicious or bot-like activity. Please ensure your Discord account is secure.")
                .addFields(EmbedCreateFields.Field.of("Message", "```\n%s\n```".formatted(StringUtil.left(message.getContent().replace("```", ""), 500)), false))
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
            .addField("Classification", reason.classification().name(), false)
            .addField("Type", reason.type().name(), false)
            .addField("Details", reason.details(), false)
            .addField("Mention", user.getMention(), false)
            .build()
            .asRequest()
          )
        ));
    }

    return delete;
  }

  record SpamResponse(
    Classification classification,
    Type type,
    String details
  ) {
    enum Classification {
      BAD,
      POSSIBLE,
      UNKNOWN,
      WHITELISTED
    }

    enum Type {
      PHISHING,
      IP_LOGGER,
      UNKNOWN
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
