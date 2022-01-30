package me.sulu.pencil.apis.safebrowsing;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import me.sulu.pencil.Pencil;
import reactor.core.publisher.Mono;

import java.time.Duration;

public class SafeBrowsing {
  private final Cache<String, SafeBrowsingStatus> cache;

  private final Pencil pencil;

  public SafeBrowsing(Pencil pencil) {
    this.pencil = pencil;
    this.cache = Caffeine.newBuilder()
      .expireAfterWrite(Duration.ofMinutes(5))
      .build();
  }

  public Mono<SafeBrowsingStatus> check(String url) {
    SafeBrowsingStatus status = this.cache.getIfPresent(url);
    if (status != null) return Mono.just(status);

    return this.pencil.http().get()
      .uri("https://transparencyreport.google.com/transparencyreport/api/v3/safebrowsing/status?site=" + url)
      .responseContent()
      .aggregate()
      .asString()
      .map(response -> {
        try {
          JsonNode node = this.pencil.jsonMapper().readTree(response.replace(")]}'\n", "")).get(0);
          final int overall = node.get(1).asInt();

          return new SafeBrowsingStatus(
            overall == 4 || overall == 6,
            node.get(2).asBoolean(),
            node.get(3).asBoolean(),
            node.get(4).asBoolean(),
            node.get(5).asBoolean(),
            node.get(6).asBoolean()
          );
        } catch (JsonProcessingException e) {
          throw new RuntimeException(e);
        }
      });
  }

  public record SafeBrowsingStatus(
    boolean unknown,
    boolean harmfulRedirect,
    boolean installsUnwantedSoftware,
    boolean phishing,
    boolean containsUnwantedSoftware,
    boolean hasUncommonDownload
  ) {
  }
}
