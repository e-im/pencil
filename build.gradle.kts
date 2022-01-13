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
  maven("https://m2.chew.pro/snapshots")
  maven("https://jitpack.io")
}

dependencies {
  implementation("com.github.DV8FromTheWorld", "JDA", "master-SNAPSHOT") {
    exclude(module = "opus-java")
    exclude("com.fasterxml")
  }
  implementation("pw.chew", "jda-chewtils", "2.0-SNAPSHOT")
  implementation("org.apache.logging.log4j", "log4j-core", "2.17.1")
  implementation("org.apache.logging.log4j", "log4j-slf4j-impl", "2.17.1")
  implementation("com.fasterxml.jackson.core", "jackson-databind", "2.13.1")
  implementation("com.github.ben-manes.caffeine", "caffeine", "3.0.5")
}

tasks {
  jar {
    manifest {
      attributes(
        "Multi-Release" to "true"
      )
    }
  }
}

java {
  toolchain {
    languageVersion.set(JavaLanguageVersion.of(17))
  }
}
