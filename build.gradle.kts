plugins {
  id("com.github.johnrengelman.shadow") version "7.1.0"
  id("application")
  id("java")
}

application {
  mainClass.set("me.sulu.pencil.Main")
}

repositories {
  mavenCentral()
  maven("https://oss.sonatype.org/content/repositories/snapshots/")
  maven("https://jitpack.io") {
    content {
      includeGroup("com.github.anyascii")
    }
  }
}

dependencies {
  implementation("com.discord4j", "discord4j-core", "3.3.0-SNAPSHOT") {
    exclude("com.fasterxml")
    exclude("com.github.ben-manes.caffeine")
  }

  implementation("ch.qos.logback", "logback-classic", "1.2.10")

  implementation("com.fasterxml.jackson.core", "jackson-databind", "2.13.1")
  implementation("com.fasterxml.jackson.dataformat", "jackson-dataformat-xml", "2.13.1")
  implementation("com.fasterxml.jackson.dataformat", "jackson-dataformat-yaml", "2.13.1")

  implementation("com.github.ben-manes.caffeine", "caffeine", "3.0.5")

  implementation("com.beust", "jcommander", "1.82")

  implementation("com.github.anyascii", "anyascii", "0.3.0")
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
