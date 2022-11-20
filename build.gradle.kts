plugins {
  id("com.github.johnrengelman.shadow") version "7.1.0"
  id("com.google.cloud.tools.jib") version "3.2.1"
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
  implementation("com.discord4j", "discord4j-core", "3.3.0-20220730.193136-54") {
    exclude("com.fasterxml")
    exclude("com.github.ben-manes.caffeine")
  }

  implementation("com.algolia", "algoliasearch-core", "3.16.5")
  implementation("com.algolia", "algoliasearch-java-net", "3.16.5")

  implementation("ch.qos.logback", "logback-classic", "1.2.10")

  implementation("com.fasterxml.jackson.core", "jackson-databind", "2.13.1")
  implementation("com.fasterxml.jackson.dataformat", "jackson-dataformat-xml", "2.13.1")
  implementation("com.fasterxml.jackson.dataformat", "jackson-dataformat-yaml", "2.13.1")

  implementation("com.github.ben-manes.caffeine", "caffeine", "3.0.5")

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

jib {
  from {
    platforms {
      platform {
        architecture = "amd64"
        os = "linux"
      }
      platform {
        architecture = "arm64"
        os = "linux"
      }
    }
  }
  to {
    image = "ghcr.io/e-im/pencil"
    auth {
      username = System.getenv("USERNAME")
      password = System.getenv("PASSWORD")
    }
  }
  container {
    workingDirectory = "/pencil"
  }
}
