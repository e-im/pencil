package me.sulu.pencil.manager;

import discord4j.core.event.domain.interaction.ChatInputAutoCompleteEvent;
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import me.sulu.pencil.Pencil;
import me.sulu.pencil.commands.Command;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.Logger;
import reactor.util.Loggers;

import java.util.Set;

public class CommandManager {
  private static final Logger LOGGER = Loggers.getLogger(CommandManager.class);

  private final Set<Command> commands;

  public CommandManager(final Pencil pencil, final Set<Command> commands) {
    final long id = pencil.client().rest().getApplicationId().blockOptional().orElseThrow();

    this.commands = commands;

    for (final Command command : this.commands) {
      if (command.global()) {
        pencil.client().rest().getApplicationService()
          .createGlobalApplicationCommand(id, command.request())
          .doOnSuccess(cmd -> LOGGER.info("Successfully registered global command {}", cmd.name()))
          .doOnError(t -> LOGGER.warn("Failed to register global command {}", command.request().name(), t))
          .subscribe();
      } else {
        for (final long guild : command.guilds()) {
          pencil.client().rest().getApplicationService()
            .createGuildApplicationCommand(id, guild, command.request())
            .doOnSuccess(cmd -> LOGGER.info("Successfully registered command {} in {}", cmd.name(), guild))
            .doOnError(t -> LOGGER.warn("Failed to register command {} in {}", command.request().name(), guild, t))
            .subscribe();
        }

      }
    }

    pencil.client().on(ChatInputInteractionEvent.class, this::on).subscribe();
    pencil.client().on(ChatInputAutoCompleteEvent.class, this::on).subscribe();

  }

  private Mono<Void> on(ChatInputInteractionEvent event) {
    return Flux.fromIterable(this.commands)
      .filter(command -> event.getCommandName().equals(command.request().name()))
      .flatMap(command -> command.execute(event).log())
      .then();
  }

  private Mono<Void> on(ChatInputAutoCompleteEvent event) {
    return Flux.fromIterable(this.commands)
      .filter(command -> event.getCommandName().equals(command.request().name()))
      .flatMap(command -> command.complete(event))
      .then();
  }
}
