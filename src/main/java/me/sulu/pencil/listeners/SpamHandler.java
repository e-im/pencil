package me.sulu.pencil.listeners;

import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;
import me.sulu.pencil.Pencil;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.apache.commons.lang3.StringUtils;

import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SpamHandler extends ListenerAdapter {
  private final Matcher matcher = Pattern.compile(
    "(?:https?://)(?:\\S+(?::\\S*)?@)?(?:(?!10(?:\\.\\d{1,3}){3})(?!127(?:\\.\\d{1,3}){3})(?!169\\.254(?:\\.\\d{1,3}){2})(?!192\\.168(?:\\.\\d{1,3}){2})(?!172\\.(?:1[6-9]|2\\d|3[0-1])(?:\\.\\d{1,3}){2})(?:[1-9]\\d?|1\\d\\d|2[01]\\d|22[0-3])(?:\\.(?:1?\\d{1,2}|2[0-4]\\d|25[0-5])){2}(?:\\.(?:[1-9]\\d?|1\\d\\d|2[0-4]\\d|25[0-4]))|(?:(?:[a-z\\x{00a1}-\\x{ffff}0-9]+-?)*[a-z\\x{00a1}-\\x{ffff}0-9]+)(?:\\.(?:[a-z\\x{00a1}-\\x{ffff}0-9]+-?)*[a-z\\x{00a1}-\\x{ffff}0-9]+)*(?:\\.(?:[a-z\\x{00a1}-\\x{ffff}]{2,})))(?::\\d{2,5})?(?:/[^\\s]*)?",
    Pattern.CASE_INSENSITIVE
    | Pattern.UNICODE_CASE
    | Pattern.UNICODE_CHARACTER_CLASS
  ).matcher("");
  private final String apiKey = System.getenv("PHISHERMAN_KEY");
  private final LoadingCache<String, Boolean> domains = Caffeine.newBuilder()
    .expireAfterWrite(Duration.ofHours(8))
    .build(key -> Pencil.getHTTP().sendAsync(
          HttpRequest.newBuilder(URI.create("https://api.phisherman.gg/v1/domains/" + key))
            .header("Authorization", "Bearer " + apiKey)
            .build(),
          HttpResponse.BodyHandlers.ofString()
        )
        .thenApply(HttpResponse::body)
        .thenApply(Boolean::parseBoolean)
        .get(5, TimeUnit.SECONDS)
    );


  @Override
  public void onGuildMessageReceived(final GuildMessageReceivedEvent event) {
    if (event.isWebhookMessage()
      || event.getAuthor().isBot()
      || event.getAuthor().isSystem()
      || event.getMember().getRoles().stream().anyMatch(Role::isHoisted)) return;

    matcher.reset(event.getMessage().getContentRaw());
    while (matcher.find()) {
      String matchedUrl = matcher.group();
      String domain = matchedUrl
        .replaceFirst("https?://", "")
        .replaceFirst("/.+", "");

      if (domains.get(domain)) {
        handlePhish(event.getMember(), event.getMessage(), matchedUrl);
        break;
      }
    }
  }

  private void handlePhish(Member member, Message message, String url) {
    message.delete().queue();
    member.getUser().openPrivateChannel().queue(
      (channel) -> channel.sendMessageEmbeds(
        new EmbedBuilder()
          .setTitle("Automatically kicked due to posting malicious links")
          .setThumbnail("https://static.sulu.me/images/logos/paper/256.webp")
          .setDescription("You have been automatically kicked for posting malicious links. Should you have not sent this message, your account has likely been compromised. Please take measures to secure your Discord account, including changing your password.")
          .addField("Message", StringUtils.left("```\n%s\n```".formatted(message.getContentStripped()), 500), false)
          .addField("Url", StringUtils.left("```\n%s\n```".formatted(url), 500), false)
          .build()
      ).queue(
        (sent) -> notifyBatcave(true, member, message, url),
        (error) -> notifyBatcave(false, member, message, url)
      ),
      (error) -> notifyBatcave(false, member, message, url)
    );
  }

  private void notifyBatcave(boolean success, Member member, Message message, String url) {
    member.kick("link: " + url).queue();
    Pencil.getBatcave().sendMessageEmbeds(
      new EmbedBuilder()
        .setTitle("User kicked for posting malicious links")
        .setAuthor(message.getAuthor().getAsTag(), null, message.getAuthor().getEffectiveAvatarUrl())
        .setDescription(
          StringUtils.left(
            """
              **Last Message**:
              ```
              %s
              ```
              **Reason**: `%s`
              **User Mention**: %s
              **Sent DM**: %s
              """.formatted(message.getContentStripped(), url, message.getAuthor().getAsMention(), success ? "Yes" : "No"),
            4000
          )
        )
        .build()
    ).queue();
  }
}
