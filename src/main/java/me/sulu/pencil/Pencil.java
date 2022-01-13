package me.sulu.pencil;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.jagrosh.jdautilities.command.CommandClientBuilder;
import com.jagrosh.jdautilities.commons.waiter.EventWaiter;
import me.sulu.pencil.commands.*;
import me.sulu.pencil.listeners.*;
import me.sulu.pencil.util.customsearch.CustomSearch;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.OnlineStatus;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.requests.restaction.MessageAction;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.security.auth.login.LoginException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.text.DecimalFormat;
import java.time.Duration;
import java.util.Collections;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;


public class Pencil {
  private static final SpamHandler spamHandler = new SpamHandler();
  private static final ObjectMapper mapper = new JsonMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
  public static boolean DEVELOPMENT = System.getenv("DEVELOPMENT") != null;
  public static Logger LOGGER = LogManager.getLogger();
  private static JDA jda;
  private static EventWaiter waiter;
  private static CustomSearch customSearch;
  private static HttpClient http;
  private static TextChannel batcave;
  private static TextChannel voiceLogChannel;
  private static TextChannel exploitNotificationChannel;

  public static void main(String[] args) throws LoginException, InterruptedException {
    waiter = new EventWaiter();

    CommandClientBuilder client = new CommandClientBuilder()
      .setOwnerId("0")
      .setStatus(OnlineStatus.IDLE)
      .setActivity(Activity.watching("things load..."))
      .setPrefixes(new String[]{";", "pencil", "hi pencil"}) // should implement same regex as before
      .useHelpBuilder(false)
      .addCommands(
        new GoogleCommand(),
        new PingCommand(),
        new DmCommand(),
        new PhishCommand()
      )
      .addSlashCommands(
        new GoogleCommand(),
        new ModmailCommand()
      )
      .setListener(new CommandErrorHandler())
      .forceGuildOnly(System.getenv("GUILD_ID"));


    // Default to no pinging anything
    MessageAction.setDefaultMentionRepliedUser(false);
    MessageAction.setDefaultMentions(Collections.emptyList());

    jda = JDABuilder.createDefault(System.getenv("DISCORD_TOKEN"))
      .enableIntents(
        GatewayIntent.GUILD_MEMBERS,
        GatewayIntent.GUILD_MESSAGES,
        GatewayIntent.GUILD_MESSAGE_REACTIONS,
        GatewayIntent.DIRECT_MESSAGES,
        GatewayIntent.DIRECT_MESSAGE_REACTIONS,
        GatewayIntent.GUILD_VOICE_STATES
      )
      .addEventListeners(
        waiter,
        new AttachmentHandler(),
        new DirectMessageHandler(),
        new UsernameHandler(),
        new VoiceStateLogger(),
        new ExploitHandler(),
        spamHandler,
        client.build()
      )
      .build();

    jda.awaitReady();

    http = HttpClient.newBuilder()
      .executor(Executors.newFixedThreadPool(4))
      .connectTimeout(Duration.ofSeconds(10))
      .build();

    Executors.newSingleThreadScheduledExecutor().scheduleWithFixedDelay(
      () -> {
        try {
          ArrayNode root = (ArrayNode) mapper.readTree(
            Pencil.http.send(
              HttpRequest.newBuilder(URI.create("https://bstats.org/api/v1/plugins/580/charts/servers/data")).build(),
              HttpResponse.BodyHandlers.ofString()
            ).body()
          );

          jda.getPresence().setPresence(OnlineStatus.ONLINE, Activity.playing("on " + new DecimalFormat("#,###").format(root.get(root.size() - 1).get(1).asInt()) + " servers"));
        } catch (Exception e) {
          LOGGER.warn("Failed to fetch data from bStats", e);
        }
      },
      15,
      300,
      TimeUnit.SECONDS
    );

    customSearch = new CustomSearch(System.getenv("CUSTOMSEARCH_CX"), System.getenv("CUSTOMSEARCH_KEY"));
    batcave = jda.getTextChannelById(System.getenv("BATCAVE_ID"));
    exploitNotificationChannel = jda.getTextChannelById(System.getenv("EXPLOIT_REPORT_CHANNEL_ID"));
    voiceLogChannel = jda.getTextChannelById(System.getenv("VOICE_LOG_CHANNEL_ID"));
  }

  public static JDA getJDA() {
    return jda;
  }

  public static EventWaiter getWaiter() {
    return waiter;
  }

  public static CustomSearch getCustomSearch() {
    return customSearch;
  }

  public static TextChannel getBatcave() {
    return batcave;
  }

  public static TextChannel getVoiceLogChannel() {
    return voiceLogChannel;
  }

  public static TextChannel getExploitNotificationChannel() {
    return exploitNotificationChannel;
  }

  public static HttpClient getHTTP() {
    return http;
  }

  public static ObjectMapper getMapper() {
    return mapper;
  }

  public static SpamHandler getSpamHandler() {
    return spamHandler;
  }
}
