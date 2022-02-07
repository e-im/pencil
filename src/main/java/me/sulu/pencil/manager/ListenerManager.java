package me.sulu.pencil.manager;

import me.sulu.pencil.listeners.Listener;
import reactor.core.Disposable;
import reactor.core.Disposables;
import reactor.core.publisher.Flux;

import java.util.Set;

public class ListenerManager {
  private Disposable disposable = null;

  public ListenerManager(final Set<? extends Listener> listeners) {
    Flux.fromIterable(listeners).map(Listener::start).collectList()
      .doOnNext(list -> disposable = Disposables.composite(list))
      .subscribe();
  }

  public void dispose() {
    if (this.disposable == null) {
      throw new IllegalStateException("Attempted to dispose disposable before it was initialized!");
    }

    this.disposable.dispose();
  }
}
