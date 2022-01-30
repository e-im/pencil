package me.sulu.pencil.apis.pastegg;

import me.sulu.pencil.Pencil;
import reactor.core.publisher.Mono;
import reactor.netty.ByteBufFlux;

import java.io.IOException;

public class PasteGG {
  private final Pencil pencil;

  public PasteGG(Pencil pencil) {
    this.pencil = pencil;
  }

  public Mono<String> pastebin(String name, String description, String fileName, String content) {
    return this.pencil.http()
      .headers(headers -> {
        headers.add("Authorization", "Key " + this.pencil.config().global().secrets().pasteggKey());
        headers.add("Content-Type", "application/json");
      })
      .post()
      .uri("https://api.paste.gg/v1/pastes")
      .send(ByteBufFlux.fromString(Mono
          .just(
            this.pencil.jsonMapper().createObjectNode()
              .put("name", name)
              .put("description", description)
              .set("files", this.pencil.jsonMapper().createArrayNode()
                .add(this.pencil.jsonMapper().createObjectNode()
                  .put("name", fileName)
                  .set("content", this.pencil.jsonMapper().createObjectNode()
                    .put("format", "text")
                    .put("value", content)
                  )
                )
              )
              .toPrettyString()
          )
        )
      )
      .responseContent()
      .aggregate()
      .asByteArray()
      .map(data -> {
        try {
          return "https://paste.gg/" + this.pencil.jsonMapper().readTree(data).get("result").get("id").asText();
        } catch (IOException e) {
          throw new RuntimeException(e);
        }
      });
  }
}
