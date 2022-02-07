package me.sulu.pencil.listeners;

import discord4j.core.event.domain.Event;
import me.sulu.pencil.Pencil;
import org.reactivestreams.Publisher;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;

import java.util.function.Function;

public abstract class Listener {
  private final Pencil pencil;

  public Listener(final Pencil pencil) {
    this.pencil = pencil;
  }

  protected Pencil pencil() {
    return this.pencil;
  }

  protected <E extends Event, T> Flux<T> on(Class<E> eventClass, Function<E, Publisher<T>> mapper) {
    return this.pencil().on(eventClass, mapper);
  }

  public abstract Disposable start();
}
