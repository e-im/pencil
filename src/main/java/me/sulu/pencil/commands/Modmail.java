package me.sulu.pencil.commands;

import discord4j.common.util.Snowflake;
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.object.command.ApplicationCommandOption;
import discord4j.core.spec.EmbedCreateSpec;
import discord4j.discordjson.json.ApplicationCommandOptionData;
import discord4j.discordjson.json.ApplicationCommandRequest;
import discord4j.rest.util.Color;
import me.sulu.pencil.Pencil;
import me.sulu.pencil.util.StringUtil;
import reactor.core.publisher.Mono;
import reactor.util.Logger;
import reactor.util.Loggers;

import java.time.Instant;

public class Modmail extends Command {
  private static final Logger LOGGER = Loggers.getLogger(Modmail.class);
  private final ApplicationCommandRequest request = ApplicationCommandRequest.builder()
    .name("modmail")
    .description("Send a private message to the moderators")
    .addOption(ApplicationCommandOptionData.builder()
      .name("message")
      .description("Message to send the moderators")
      .type(ApplicationCommandOption.Type.STRING.getValue())
      .required(true)
      .build()
    )
    .build();

  public Modmail(Pencil pencil) {
    super(pencil);
  }

  @Override
  public Mono<Void> execute(ChatInputInteractionEvent event) {
    final Snowflake id;
    if (event.getInteraction().getGuildId().isEmpty()) {
      return event.reply("/modmail may not be used in direct messages. Please run from within a guild. Don't worry! Your message will be hidden.")
        .withEphemeral(true);
    } else {
      id = event.getInteraction().getGuildId().get();
    }

    final String content;
    if (event.getOption("message").isEmpty()
      || event.getOption("message").get().getValue().isEmpty()
      || event.getOption("message").get().getValue().get().asString().isBlank()) {
      return event.reply("Please provide a message to send. Invalid message provided.").withEphemeral(true);
    } else {
      content = event.getOption("message").get().getValue().get().asString();
    }

    final long modmailChannelId = this.pencil().config().guild(id).channels().modmail();

    if (modmailChannelId == 0L) {
      return event.getInteraction().getGuild()
        .flatMap(guild -> event.reply(guild.getName() + " has not properly configured modmail. Please contact a moderator directly.").withEphemeral(true));
    }

    return this.pencil().client().rest().getChannelById(Snowflake.of(modmailChannelId)).createMessage(EmbedCreateSpec.builder()
        .title("New Modmail Message")
        .author(event.getInteraction().getUser().getTag(), null, event.getInteraction().getUser().getAvatarUrl())
        .description(StringUtil.left(content, 4096))
        .color(Color.SUBMARINE)
        .footer("User ID: " + event.getInteraction().getUser().getId().asString(), null)
        .timestamp(Instant.now())
        .build()
        .asRequest()
      )
      .then(event.reply("Successfully messaged the moderators.").withEphemeral(true))
      .onErrorResume(t -> {
        LOGGER.warn("Failed to deliver modmail message", t);
        return event.reply("Failed to deliver message. Please contact a moderator directly.");
      });
  }

  @Override
  public ApplicationCommandRequest request() {
    return this.request;
  }
}
