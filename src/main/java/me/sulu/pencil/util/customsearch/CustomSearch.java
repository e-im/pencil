package me.sulu.pencil.util.customsearch;

import me.sulu.pencil.Pencil;
import me.sulu.pencil.util.customsearch.entity.Result;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

import static java.lang.String.format;

public class CustomSearch {
  public static final Logger LOGGER = LogManager.getLogger();
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

  public Result executeSearch(String term) throws IOException {
    return Pencil.getMapper().readValue(new URL(getUrl(term)), Result.class);
  }
}
