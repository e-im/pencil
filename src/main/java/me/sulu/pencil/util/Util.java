package me.sulu.pencil.util;

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
}
