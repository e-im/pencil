package me.sulu.pencil.listeners;

import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.object.entity.User;
import me.sulu.pencil.Pencil;
import me.sulu.pencil.apis.pastegg.PasteGG;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeoutException;

import static java.nio.charset.StandardCharsets.UTF_8;

public class AttachmentListener extends Listener {
  private static final List<String> excludedExtensions = List.of(
    ".nbt"
  );
  private final PasteGG pasteGG;

  public AttachmentListener(Pencil pencil) {
    super(pencil);
    this.pasteGG = new PasteGG(pencil);
  }

  private Mono<Void> on(MessageCreateEvent event) {
    if (event.getGuildId().isEmpty()
      || event.getMessage().getAuthor().isEmpty()
      || event.getMessage().getAuthor().get().isBot()
      || !this.pencil().config().guild(event.getGuildId().get()).features().fileUploading()) {
      return Mono.empty();
    }

    final User author = event.getMessage().getAuthor().get();

    return Flux.fromIterable(event.getMessage().getAttachments())
      .publishOn(Schedulers.boundedElastic())
      .filter(a -> a.getSize() <= 1e7)
      .filter(a -> excludedExtensions.stream().noneMatch(s -> a.getFilename().endsWith(s)))
      .flatMap(attachment -> this.pencil().http().get()
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

  @Override
  public Disposable start() {
    return this.on(MessageCreateEvent.class, this::on).subscribe();
  }
}
