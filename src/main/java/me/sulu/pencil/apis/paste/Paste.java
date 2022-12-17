package me.sulu.pencil.apis.paste;

import io.netty.handler.codec.http.HttpHeaderNames;
import me.sulu.pencil.Pencil;
import reactor.core.publisher.Mono;
import reactor.netty.ByteBufFlux;

public class Paste {
  private final Pencil pencil;

  public Paste(Pencil pencil) {
    this.pencil = pencil;
  }

  public Mono<String> paste(final String type, final String content) {
    return this.pencil.http()
      .headers(headers -> headers.add(HttpHeaderNames.CONTENT_TYPE, type))
      .post()
      .uri("https://api.pastes.dev/post")
      .send(ByteBufFlux.fromString(Mono.just(content)))
      .response()
      .mapNotNull(response -> "https://pastes.dev/" + response.responseHeaders().get(HttpHeaderNames.LOCATION));
  }
}
