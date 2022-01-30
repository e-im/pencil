package me.sulu.pencil.util;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.framework.qual.DefaultQualifier;

import java.util.Random;

@DefaultQualifier(NonNull.class)
public final class StringUtil {

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

  private static final Random random = new Random();


  public static String left(final @Nullable String string, final int length) {
    if (string == null || length <= 0) {
      return "";
    }

    if (string.length() <= length) {
      return string;
    }

    return string.substring(0, length);
  }

  public static String randomName() {
    return Adjectives[random.nextInt(Adjectives.length)] + Animals[random.nextInt(Animals.length)];
  }
}
