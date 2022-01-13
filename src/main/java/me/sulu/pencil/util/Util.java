package me.sulu.pencil.util;

import me.sulu.pencil.Pencil;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class Util {

  private static final String[] Adjectives = {
    "Fast",
    "Quick",
    "Slow",
    "Bright",
    "Noisy",
    "Loud",
    "Quiet",
    "Brave",
    "Sad",
    "Proud",
    "Happy",
    "Comfortable",
    "Clever",
    "Interesting",
    "Funny",
    "Kind",
    "Polite",
    "Fair",
    "Careful",
    "Safe",
    "Dangerous"
  };
  private static final String[] Animals = {
    "Aardvark",
    "Alligator",
    "Ant",
    "Crab",
    "Cricket",
    "Crow",
    "Deer",
    "Dog",
    "Flamingo",
    "Frog",
    "Fox",
    "Herring",
    "Llama",
    "Mongoose",
    "Quail",
    "Tiger",
    "Weasel",
    "Wolf",
    "Yak",
    "Zebra"
  };

  private static final Random rd = new Random();

  public static String randomName() {
    return Adjectives[rd.nextInt(Adjectives.length)] + Animals[rd.nextInt(Animals.length)];
  }

  public static String leftmost(final String string, final int length) {
    if (string == null || length < 0) return "";
    if (string.length() <= length) return string;
    return string.substring(0, length);
  }

  public static List<String> split(final String string, final int length) {
    final List<String> list = new ArrayList<>((string.length() + length - 1) / length);
    for (int i = 0; i < string.length(); i += length) {
      list.add(string.substring(i, Math.min(string.length(), i + length)));
    }
    return list;
  }

  public static String pastebin(String data, String contentType) throws InterruptedException, IOException {
    return "https://pastes.dev/" + Pencil.getHTTP().send(
      HttpRequest.newBuilder(URI.create("https://api.pastes.dev/post"))
        .POST(HttpRequest.BodyPublishers.ofString(data))
        .header("user-agent", "Pencil Discord Bot (PaperMC)")
        .headers("content-type", contentType)
        .build(),
      HttpResponse.BodyHandlers.ofString()
    ).headers().firstValue("location").orElseThrow();
  }
}
