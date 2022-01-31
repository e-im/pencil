package me.sulu.pencil.util;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import discord4j.common.util.Snowflake;
import discord4j.core.object.entity.Guild;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.framework.qual.DefaultQualifier;
import reactor.util.Logger;
import reactor.util.Loggers;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

@DefaultQualifier(NonNull.class)
public class Config {
  private static final Logger LOGGER = Loggers.getLogger(Config.class);
  private final GlobalConfig config;
  private final Map<Long, GuildConfig> guilds = new HashMap<>();

  public Config(final ObjectMapper mapper, Path path) {
    try {
      JsonNode tree = mapper.readTree(Files.readAllBytes(path));

      this.config = mapper.treeToValue(tree.get("config"), GlobalConfig.class);

      tree.get("guilds").fields().forEachRemaining(entry -> {
        try {
          guilds.put(Long.parseLong(entry.getKey()), mapper.treeToValue(entry.getValue(), GuildConfig.class));
        } catch (JsonProcessingException e) {
          LOGGER.warn("Failed to deserialize configuration for guild {}!", entry.getKey(), e);
        }
      });
    } catch (IOException e) {
      throw new RuntimeException("Failed to init configuration!", e);
    }
  }

  public boolean hasGuild(final long id) {
    return this.guilds.containsKey(id);
  }

  public boolean hasGuild(final Snowflake id) {
    return this.hasGuild(id.asLong());
  }

  public boolean hasGuild(final Guild guild) {
    return this.hasGuild(guild.getId());
  }

  public GuildConfig guild(final long id) {
    final GuildConfig config = this.guilds.get(id);
    if (config == null) {
      throw new IllegalStateException("Bot is in guild with no configuration associated!");
    }
    return config;
  }

  public GuildConfig guild(Snowflake id) {
    return this.guild(id.asLong());
  }

  public GuildConfig guild(Guild guild) {
    return this.guild(guild.getId());
  }

  public boolean command(long id, String command) {
    return this.guild(id).features().commands().getOrDefault(command, false);
  }

  public boolean command(Snowflake id, String command) {
    return this.command(id.asLong(), command);
  }

  public boolean command(Guild guild, String command) {
    return this.command(guild.getId(), command);
  }

  public Set<Long> guilds() {
    return this.guilds.keySet();
  }

  public GlobalConfig global() {
    return this.config;
  }

  public record GuildConfig(
    Channels channels,
    Features features
  ) {
    public record Channels(
      @JsonProperty("modmail")
      long modmail,
      @JsonProperty("auto-action-log")
      long autoActionLog,
      @JsonProperty("voice-log")
      long voiceLog,
      @JsonProperty("exploit-report")
      long exploitReport
    ) {
    }

    public record Features(
      Map<String, Boolean> commands,
      @JsonProperty("file-uploading")
      boolean fileUploading,
      @JsonProperty("name-normalization")
      NameNormalization nameNormalization,
      Phishing phishing
    ) {
      public record NameNormalization(
        boolean normalize,
        boolean aggressive,
        boolean dehoist
      ) {
      }

      public record Phishing(
        boolean enabled,
        @JsonProperty("domain-spam")
        DomainSpam domainSpam
      ) {
        public record DomainSpam(
          int channels,
          int uniques
        ) {
        }
      }
    }
  }

  public record GlobalConfig(
    @JsonProperty("domain-allowlist")
    Set<String> domainAllowlist,
    @JsonProperty("debug")
    Debug debug,
    @JsonProperty("secrets")
    Secrets secrets
  ) {

    public record Debug(
      long dm
    ) {
    }

    public record Secrets(
      @JsonProperty("pastegg-key")
      String pasteggKey,
      @JsonProperty("custom-search")
      CustomSearch customSearch
    ) {
      public record CustomSearch(
        String cx,
        String key
      ) {
      }
    }
  }
}
