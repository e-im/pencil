package me.sulu.pencil.util;

import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;

public class RateLimiter<T> {
  private final LoadingCache<T, AtomicInteger> limiter;
  private final int allowedActions;

  public RateLimiter(final Duration period, final int allowedActions) {
    this.allowedActions = allowedActions;
    this.limiter = Caffeine.newBuilder()
      .expireAfterWrite(period)
      .build(key -> new AtomicInteger(0));
  }

  public boolean limit(final T key) {
    return this.limiter.get(key).incrementAndGet() > this.allowedActions;
  }
}
