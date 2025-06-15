plugins {
    id("fabric-loom") version "1.10-SNAPSHOT"
    id("com.gradleup.shadow") version "8.3.5"
    id("maven-publish")
}

val minecraftVersion = property("minecraft_version") as String
val fabricLoaderVersion = property("fabric_loader_version") as String

version = property("version") as String
group = property("maven_group") as String

loom {
    mods {
        create("torque") {
            sourceSet(sourceSets["main"])
        }
    }
    accessWidenerPath = file("src/main/resources/torque.accesswidener")
}

dependencies {
    implementation(project(":torque"))
    minecraft("com.mojang:minecraft:$minecraftVersion")
    modImplementation("net.fabricmc:fabric-loader:$fabricLoaderVersion")
    mappings(loom.officialMojangMappings())
}

tasks {
    assemble {
        dependsOn(shadowJar)
    }
    shadowJar {
        archiveClassifier.set("")
        dependencies {
            include(project(":torque"))
        }
    }
    processResources {
        val props = mapOf(
            "version" to version,
            "minecraft_version" to minecraftVersion,
            "fabric_loader_version" to fabricLoaderVersion
        )
        inputs.properties(props)
        filteringCharset = "UTF-8"
        filesMatching("fabric.mod.json") {
            expand(props)
        }
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
