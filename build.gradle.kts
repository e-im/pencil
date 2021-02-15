import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
  id("com.github.johnrengelman.shadow") version "7.0.0"
  id("application")
  id("java")
}

application {
  mainClass.set("me.sulu.pencil.Pencil")
}

// makes jar small but breaks methanol compression. Probably need to manually include a class or something
//val shadowJar: ShadowJar by tasks
//shadowJar.apply {
//  minimize()
//}

repositories {
  mavenCentral()
  maven("https://m2.dv8tion.net/releases")
  maven("https://m2.chew.pro/releases")
}

dependencies {
  // HttpClient with more stuff
  implementation("com.github.mizosoft.methanol", "methanol", "1.6.0")

  // JDA
  implementation("net.dv8tion:JDA:4.3.0_299") {
    exclude(module = "opus-java")
  }
  implementation("pw.chew:jda-chewtils:1.20.2")

  // Logging
  implementation("ch.qos.logback:logback-classic:1.2.3")

  // TODO implement methanol-gson
  implementation("com.google.code.gson:gson:2.8.7")

  implementation("com.google.guava:guava:30.1.1-jre")
  implementation("org.apache.commons:commons-lang3:3.12.0")
}

java {
  toolchain {
    languageVersion.set(JavaLanguageVersion.of(16))
  }
}
