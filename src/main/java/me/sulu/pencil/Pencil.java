package me.sulu.pencil;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import discord4j.core.DiscordClientBuilder;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.event.domain.Event;
import discord4j.core.event.domain.lifecycle.ReadyEvent;
import discord4j.core.object.presence.ClientActivity;
import discord4j.core.object.presence.ClientPresence;
import discord4j.gateway.intent.IntentSet;
import discord4j.rest.util.AllowedMentions;
import me.sulu.pencil.commands.Google;
import me.sulu.pencil.commands.Modmail;
import me.sulu.pencil.listeners.*;
import me.sulu.pencil.manager.CommandManager;
import me.sulu.pencil.manager.ListenerManager;
import me.sulu.pencil.util.Config;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.netty.http.client.HttpClient;
import reactor.util.Logger;
import reactor.util.Loggers;

import java.nio.file.Path;
import java.time.Duration;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;

public class Pencil {
  private static final Logger LOGGER = Loggers.getLogger(Pencil.class);
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

  private Config config;
  private GatewayDiscordClient client;

  public void start() {
    this.config = new Config(YAML_MAPPER, Path.of(Objects.requireNonNullElse(System.getenv("PENCIL_CONFIG_FILE"), "config.yaml")));

    this.client = DiscordClientBuilder.create(System.getenv("PENCIL_DISCORD_TOKEN"))
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

    new CommandManager(this, Set.of(
      new Google(this),
      new Modmail(this)
    ));

    new ListenerManager(Set.of(
      new AttachmentListener(this),
      new DirectMessageListener(this),
      new ExploitListener(this),
      new GuildListener(this),
      new SpamListener(this),
      new UserChangeListener(this),
      new VoiceStateListener(this)
    ));

    this.client().onDisconnect().block();
  }

  public <E extends Event, T> Flux<T> on(Class<E> eventClass, Function<E, Publisher<T>> mapper) {
    return this.client().on(eventClass, mapper);
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
