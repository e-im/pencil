package me.sulu.pencil;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import discord4j.core.DiscordClientBuilder;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.event.domain.lifecycle.ReadyEvent;
import discord4j.core.object.presence.ClientActivity;
import discord4j.core.object.presence.ClientPresence;
import discord4j.gateway.intent.IntentSet;
import discord4j.rest.util.AllowedMentions;
import me.sulu.pencil.listeners.*;
import me.sulu.pencil.manager.CommandManager;
import me.sulu.pencil.util.Config;
import reactor.netty.http.client.HttpClient;
import reactor.util.Logger;
import reactor.util.Loggers;

import java.nio.file.Path;
import java.time.Duration;

public class Pencil {
  private final Logger LOGGER = Loggers.getLogger(Pencil.class);
  private final ObjectMapper JSON_MAPPER = JsonMapper.builder()
    .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
    .configure(MapperFeature.ACCEPT_CASE_INSENSITIVE_ENUMS, true)
    .build();
  private final ObjectMapper XML_MAPPER = XmlMapper.xmlBuilder()
    .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
    .build();
  private final ObjectMapper YAML_MAPPER = YAMLMapper.builder()
    .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
    .build();

  private final HttpClient HTTP = HttpClient.create();

  @SuppressWarnings("FieldMayBeFinal")
  @Parameter(names = {"--config", "-c"}, description = "Configuration file path")
  private String configFileName = "config.yaml";

  @Parameter(names = {"--token", "-t"}, description = "Discord Bot Token", password = true)
  private String token;

  private Config config;
  private GatewayDiscordClient client;

  public void start(final String[] args) {
    JCommander.newBuilder()
      .addObject(this)
      .build()
      .parse(args);

    this.config = new Config(YAML_MAPPER, Path.of(configFileName));

    this.client = DiscordClientBuilder.create(token)
      .setDefaultAllowedMentions(AllowedMentions.suppressAll())
      .build()
      .gateway()
      .setEnabledIntents(IntentSet.all())
      .setInitialPresence(info -> ClientPresence.online(
        ClientActivity.watching("/modmail")
      ))
      .login()
      .block(Duration.ofSeconds(10L));

    if (this.client == null) throw new IllegalStateException("Failed to init Discord client");

    this.client().on(ReadyEvent.class)
      .doOnNext(ready -> LOGGER.info("Successfully logged in as {}", ready.getSelf().getTag()))
      .subscribe();

    new CommandManager(this);

    new SpamListener(this);
    new AttachmentListener(this);
    new VoiceStateListener(this);
    new UserChangeListener(this);
    new ExploitListener(this);
    new DirectMessageListener(this);

    this.client().onDisconnect().log().block();
  }

  public Config config() {
    return this.config;
  }

  public GatewayDiscordClient client() {
    return this.client;
  }

  public ObjectMapper jsonMapper() {
    return this.JSON_MAPPER;
  }

  public ObjectMapper xmlMapper() {
    return this.XML_MAPPER;
  }

  public HttpClient http() {
    return this.HTTP;
  }
}
