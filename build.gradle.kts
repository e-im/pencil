plugins {
  id("com.gradleup.shadow") version "8.3.5"
  id("com.google.cloud.tools.jib") version "3.4.4"
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
  implementation("com.discord4j", "discord4j-core", "3.3.0-RC1") {
    exclude("com.fasterxml")
    exclude("com.github.ben-manes.caffeine")
  }

  implementation("com.algolia", "algoliasearch", "4.15.4")

  implementation("ch.qos.logback", "logback-classic", "1.5.12")

  implementation("com.fasterxml.jackson.core", "jackson-databind", "2.18.1")
  implementation("com.fasterxml.jackson.dataformat", "jackson-dataformat-xml", "2.18.1")
  implementation("com.fasterxml.jackson.dataformat", "jackson-dataformat-yaml", "2.18.1")

  implementation("com.github.ben-manes.caffeine", "caffeine", "3.1.8")

  implementation("com.github.anyascii", "anyascii", "0.3.2")
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
    languageVersion.set(JavaLanguageVersion.of(21))
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
