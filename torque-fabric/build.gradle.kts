plugins {
    id("fabric-loom") version "1.10-SNAPSHOT"
    id("maven-publish")
}

val minecraftVersion = property("minecraft_version") as String
val yarnMappings = property("yarn_mappings") as String
val fabricLoaderVersion = property("fabric_loader_version") as String

version = property("version") as String
group = property("maven_group") as String

loom {
    mods {
        create("torque") {
            sourceSet(sourceSets["main"])
        }
    }
}

dependencies {
    minecraft("com.mojang:minecraft:$minecraftVersion")
    mappings("net.fabricmc:yarn:$yarnMappings:v2")
    modImplementation("net.fabricmc:fabric-loader:$fabricLoaderVersion")
}

tasks.named<ProcessResources>("processResources") {
    inputs.property("version", version)
    inputs.property("minecraft_version", minecraftVersion)
    inputs.property("fabric_loader_version", fabricLoaderVersion)
    filteringCharset = "UTF-8"

    filesMatching("fabric.mod.json") {
        expand(
            mapOf(
                "version" to version,
                "minecraft_version" to minecraftVersion,
                "fabric_loader_version" to fabricLoaderVersion
            )
        )
    }
}

publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            from(components["java"])
        }
    }

    // See https://docs.gradle.org/current/userguide/publishing_maven.html for information on how to set up publishing.
    repositories {
        // Add repositories to publish to here.
    }
}
