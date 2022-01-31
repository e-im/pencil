package me.sulu.pencil.listeners;

import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.object.entity.User;
import me.sulu.pencil.Pencil;
import me.sulu.pencil.apis.pastegg.PasteGG;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.time.Duration;
import java.util.Arrays;
import java.util.concurrent.TimeoutException;

import static java.nio.charset.StandardCharsets.UTF_8;

public class AttachmentListener {
  private final Pencil pencil;
  private final PasteGG pasteGG;

  public AttachmentListener(Pencil pencil) {
    this.pencil = pencil;
    this.pasteGG = new PasteGG(pencil);
    this.pencil.on(MessageCreateEvent.class, this::on).subscribe();
  }

  private Mono<Void> on(MessageCreateEvent event) {
    if (event.getGuildId().isEmpty()
      || event.getMessage().getAuthor().isEmpty()
      || event.getMessage().getAuthor().get().isBot()
      || !this.pencil.config().guild(event.getGuildId().get()).features().fileUploading()) {
      return Mono.empty();
    }

    final User author = event.getMessage().getAuthor().get();

    return Flux.fromIterable(event.getMessage().getAttachments())
      .publishOn(Schedulers.boundedElastic())
      .filter(attachment -> attachment.getSize() <= 1e7)
      .flatMap(attachment -> this.pencil.http().get()
        .uri(attachment.getUrl())
        .responseContent()
        .aggregate()
        .asByteArray()
        .timeout(Duration.ofSeconds(6))
        .onErrorResume(TimeoutException.class, ignored -> Mono.empty())
        .zipWhen(bytes -> Mono.just(new String(bytes, UTF_8)))
        .filter(tuple -> Arrays.equals(tuple.getT1(), tuple.getT2().getBytes(UTF_8))) // Checks if this is actually something we want to pastebin. TODO: Find something better/faster?
        .flatMap(content -> this.pasteGG.pastebin("Content by %s (%s)".formatted(author.getTag(), author.getId().asString()), "", attachment.getFilename(), content.getT2()))
        .zipWith(event.getMessage().getChannel())
        .flatMap(tuple -> tuple.getT2().createMessage("%s by %s: %s".formatted(attachment.getFilename(), author.getMention(), tuple.getT1()))
          .withMessageReference(event.getMessage().getId()))
      ).then();
  }
}
