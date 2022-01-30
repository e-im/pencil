package me.sulu.pencil.manager;

import discord4j.core.event.domain.interaction.ChatInputAutoCompleteEvent;
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import io.github.classgraph.ClassGraph;
import io.github.classgraph.ScanResult;
import me.sulu.pencil.Pencil;
import me.sulu.pencil.commands.Command;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.Logger;
import reactor.util.Loggers;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

public class CommandManager {
  private static final Logger LOGGER = Loggers.getLogger(CommandManager.class);

  private final Set<Command> commands = new HashSet<>();

  public CommandManager(Pencil pencil) {
    final long id = pencil.client().rest().getApplicationId().blockOptional().orElseThrow();

    try (final ScanResult scan = new ClassGraph()
      .enableClassInfo()
      .acceptPackages("me.sulu.pencil.commands")
      .rejectClasses(Command.class.getName())
      .scan()) {

      this.commands.addAll(scan.getSubclasses(Command.class).loadClasses(Command.class).stream()
        .map(clazz -> {
          try {
            return clazz.getDeclaredConstructor(Pencil.class).newInstance(pencil);
          } catch (Exception e) {
            LOGGER.warn("Failed to load command {}", clazz.getName(), e);
            throw new RuntimeException(e);
          }
        })
        .collect(Collectors.toSet())
      );

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
    } catch (Exception e) {
      LOGGER.error("Failed to register command!", e);
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
