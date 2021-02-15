package me.sulu.pencil;

import com.github.mizosoft.methanol.Methanol;
import com.github.mizosoft.methanol.MutableRequest;
import com.google.gson.Gson;
import com.jagrosh.jdautilities.command.CommandClientBuilder;
import com.jagrosh.jdautilities.commons.waiter.EventWaiter;
import me.sulu.pencil.util.customsearch.CustomSearch;
import me.sulu.pencil.commands.DmCommand;
import me.sulu.pencil.commands.GoogleCommand;
import me.sulu.pencil.commands.ModmailCommand;
import me.sulu.pencil.commands.PingCommand;
import me.sulu.pencil.listeners.*;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.OnlineStatus;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.requests.restaction.MessageAction;
import org.json.JSONArray;

import javax.security.auth.login.LoginException;
import java.net.http.HttpResponse;
import java.text.DecimalFormat;
import java.time.Duration;
import java.util.Collections;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.Executors;


public class Pencil {
  public static boolean DEVELOPMENT = System.getenv("DEVELOPMENT") != null;
  private static JDA jda;
  private static EventWaiter waiter;
  private static CustomSearch customSearch;
  private static Methanol DiscordAPI;
  private static Methanol http;
  private static TextChannel modMailChannel;
  private static TextChannel voiceLogChannel;
  private static TextChannel exploitNotificationChannel;
  private static String pasteggkey;
  private static final Gson gson = new Gson();
  private static final Timer timer = new Timer();

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
        new DmCommand()
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
        client.build()
      )
      .build();

    jda.awaitReady();

    DiscordAPI = Methanol.newBuilder()
      .baseUri("https://discord.com/api/v9/")
      .executor(Executors.newFixedThreadPool(4))
      .connectTimeout(Duration.ofSeconds(5))
      .readTimeout(Duration.ofSeconds(10))
      .defaultHeader("Authorization", jda.getToken())
      .build();

    http = Methanol.newBuilder()
      .executor(Executors.newFixedThreadPool(4))
      .readTimeout(Duration.ofSeconds(10))
      .build();

    timer.schedule(
      new TimerTask() {
        @Override
        public void run() {
          JSONArray servers;
          try {
            servers = new JSONArray(
              Pencil.http.send(
                MutableRequest.GET("https://bstats.org/api/v1/plugins/580/charts/servers/data"),
                HttpResponse.BodyHandlers.ofString()
              ).body()
            );
            String serverCount = new DecimalFormat("#,###").format(servers.getJSONArray(servers.length() - 1).getInt(1));
            jda.getPresence().setPresence(OnlineStatus.ONLINE, Activity.playing("on " + serverCount + " servers."));
          } catch (Exception e) {
            e.printStackTrace();
          }
        }
      },
      5 * 1000, 300 * 1000
    );

    customSearch = new CustomSearch(System.getenv("CUSTOMSEARCH_CX"), System.getenv("CUSTOMSEARCH_KEY"));
    pasteggkey = System.getenv("PASTE_GG_KEY");
    modMailChannel = jda.getTextChannelById(System.getenv("MODMAIL_CHANNEL_ID"));
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

  public static TextChannel getModMailChannel() {
    return modMailChannel;
  }

  public static TextChannel getVoiceLogChannel() {
    return voiceLogChannel;
  }

  public static TextChannel getExploitNotificationChannel() {
    return exploitNotificationChannel;
  }

  public static Methanol getDiscordAPI() {
    return DiscordAPI;
  }

  public static Methanol getHTTP() {
    return http;
  }

  public static String getPasteggkey() {
    return pasteggkey;
  }

  public static Gson getGson() {
    return gson;
  }
}
