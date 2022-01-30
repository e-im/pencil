package me.sulu.pencil.util;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.framework.qual.DefaultQualifier;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@DefaultQualifier(NonNull.class)
public final class RegexUtil {
  private static final Pattern CLICKABLE_URL = Pattern.compile(
    "(?:https?://)(?:\\S+(?::\\S*)?@)?(?:(?!10(?:\\.\\d{1,3}){3})(?!127(?:\\.\\d{1,3}){3})(?!169\\.254(?:\\.\\d{1,3}){2})(?!192\\.168(?:\\.\\d{1,3}){2})(?!172\\.(?:1[6-9]|2\\d|3[0-1])(?:\\.\\d{1,3}){2})(?:[1-9]\\d?|1\\d\\d|2[01]\\d|22[0-3])(?:\\.(?:1?\\d{1,2}|2[0-4]\\d|25[0-5])){2}(?:\\.(?:[1-9]\\d?|1\\d\\d|2[0-4]\\d|25[0-4]))|(?:(?:[a-z\\x{00a1}-\\x{ffff}0-9]+-?)*[a-z\\x{00a1}-\\x{ffff}0-9]+)(?:\\.(?:[a-z\\x{00a1}-\\x{ffff}0-9]+-?)*[a-z\\x{00a1}-\\x{ffff}0-9]+)*(?:\\.(?:[a-z\\x{00a1}-\\x{ffff}]{2,})))(?::\\d{2,5})?(?:/[^\\s]*)?",
    Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE | Pattern.UNICODE_CHARACTER_CLASS
  );

  public static List<String> urls(String string) {
    List<String> urls = new ArrayList<>();
    Matcher matcher = CLICKABLE_URL.matcher(string);
    while (matcher.find()) {
      urls.add(matcher.group());
    }
    return urls;
  }

  public static List<String> domains(String string) {
    return urls(string)
      .stream()
      .map(RegexUtil::domainFromUrl)
      .toList();
  }

  public static Map<String, String> urlsAndDomains(String string) {
    Map<String, String> map = new HashMap<>();
    Matcher matcher = CLICKABLE_URL.matcher(string);
    while (matcher.find()) {
      String url = matcher.group();
      map.put(url, domainFromUrl(url));
    }

    return map;
  }

  public static String domainFromUrl(String string) {
    return string.replaceFirst("(?i)^(https?://)(?:[^@/\\n]+@)?(?:www\\.)?([^:/?\\n ]+)(.+)?", "$2");
  }
}
