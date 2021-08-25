plugins {
  id("com.github.johnrengelman.shadow") version "7.1.0"
  id("application")
  id("java")
}

application {
  mainClass.set("me.sulu.pencil.Pencil")
}

repositories {
  mavenCentral()
  maven("https://m2.dv8tion.net/releases")
  maven("https://m2.chew.pro/releases")
}

dependencies {
  implementation("com.github.mizosoft.methanol", "methanol", "1.6.0")
  implementation("net.dv8tion:JDA:4.3.0_346") {
    exclude(module = "opus-java")
  }
  implementation("pw.chew:jda-chewtils:1.23.0")
  implementation("ch.qos.logback:logback-classic:1.2.10")
  implementation("com.google.code.gson:gson:2.8.9")
  implementation("com.google.guava:guava:31.0.1-jre")
  implementation("org.apache.commons:commons-lang3:3.12.0")
  implementation("org.json:json:20210307")
  implementation("com.github.ben-manes.caffeine:caffeine:3.0.5")
}

java {
  toolchain {
    languageVersion.set(JavaLanguageVersion.of(16))
  }
}
