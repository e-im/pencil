package me.sulu.pencil.util.customsearch;

import com.github.mizosoft.methanol.MutableRequest;
import me.sulu.pencil.util.customsearch.entity.Result;
import me.sulu.pencil.Pencil;

import java.net.URLEncoder;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.concurrent.ExecutionException;

import static java.lang.String.format;

public class CustomSearch {
  private final String cx;
  private final String key;

  public CustomSearch(String cx, String key) {
    this.cx = cx;
    this.key = key;
  }

  private String getUrl(String term) {
    return format(
      "https://www.googleapis.com/customsearch/v1?q=%s&key=%s&cx=%s&num=10&safe=Medium",
      URLEncoder.encode(term.toLowerCase(Locale.ROOT), StandardCharsets.UTF_8).replace("%20", "+"),
      this.key,
      this.cx
    );
  }

  public Result executeSearch(String term) throws ExecutionException, InterruptedException {
    String data = Pencil.getHTTP().sendAsync(
      MutableRequest.GET(this.getUrl(term)),
      HttpResponse.BodyHandlers.ofString()
    )
      .thenApply(HttpResponse::body)
      .get();

    return Pencil.getGson().fromJson(data, Result.class);
  }
}
