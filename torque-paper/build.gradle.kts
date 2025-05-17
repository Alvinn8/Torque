plugins {
    id("xyz.jpenilla.run-paper") version "2.3.1"
}

val minecraftVersion = property("minecraft_version") as String
val paperApiVersion = property("paper_api_version") as String

version = property("version") as String
group = property("maven_group") as String

repositories {
    mavenCentral()
    maven {
        name = "papermc"
        url = uri("https://repo.papermc.io/repository/maven-public/")
    }
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:${paperApiVersion}")
}

tasks {
  runServer {
    minecraftVersion(minecraftVersion)
  }
}

tasks.named<ProcessResources>("processResources") {
    val props = mapOf("version" to version)
    inputs.properties(props)
    filteringCharset = "UTF-8"
    filesMatching("plugin.yml") {
        expand(props)
    }
}
