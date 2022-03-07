package me.sulu.pencil.apis.antifish;

import com.fasterxml.jackson.core.JsonProcessingException;
import io.netty.handler.codec.http.HttpHeaderNames;
import me.sulu.pencil.Pencil;
import reactor.core.publisher.Mono;
import reactor.netty.ByteBufFlux;

import java.io.IOException;

public class AntiFish {
  private final Pencil pencil;

  public AntiFish(final Pencil pencil) {
    this.pencil = pencil;
  }

  public Mono<AntiFishResponse> check(final String message) {
    return this.check(new AntiFishRequest(message));
  }

  public Mono<AntiFishResponse> check(final AntiFishRequest request) {
    try {
      return this.pencil.http()
        .headers(h -> h.set(HttpHeaderNames.USER_AGENT, "Pencil (https://github.com/PaperMC)"))
        .post()
        .uri("https://anti-fish.bitflow.dev/check")
        .send(ByteBufFlux.fromString(Mono.just(this.pencil.jsonMapper().writeValueAsString(request))))
        .responseContent()
        .aggregate()
        .asByteArray()
        .map(data -> {
          try {
            return this.pencil.jsonMapper().readValue(data, AntiFishResponse.class);
          } catch (IOException e) {
            throw new RuntimeException(e);
          }
        });
    } catch (JsonProcessingException e) {
      throw new RuntimeException(e);
    }
  }
}
