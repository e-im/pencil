package me.sulu.pencil.commands;

import discord4j.common.util.Snowflake;
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.object.command.ApplicationCommandOption;
import discord4j.core.object.entity.channel.TextChannel;
import discord4j.core.spec.EmbedCreateSpec;
import discord4j.discordjson.json.ApplicationCommandOptionData;
import discord4j.discordjson.json.ApplicationCommandRequest;
import me.sulu.pencil.Pencil;
import me.sulu.pencil.util.StringUtil;
import reactor.core.publisher.Mono;

public class Modmail extends Command {
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

    return pencil().client().getChannelById(Snowflake.of(this.pencil().config().guild(id).channels().modmail()))
      .map(channel -> (TextChannel) channel)
      .flatMap(channel -> channel.createMessage(EmbedCreateSpec.builder()
        .title("New Modmail Message")
        .author(event.getInteraction().getUser().getTag(), null, event.getInteraction().getUser().getAvatarUrl())
        .description(StringUtil.left(content, 4096))
        .build()
      ))
      .flatMap(__ -> event.reply("Successfully messaged the moderators.").withEphemeral(true));
  }

  @Override
  public ApplicationCommandRequest request() {
    return this.request;
  }
}
