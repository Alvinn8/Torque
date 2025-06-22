plugins {
    id("xyz.jpenilla.run-paper") version "2.3.1"
    id("com.gradleup.shadow") version "8.3.5"
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
    implementation(project(":torque"))
    compileOnly("io.papermc.paper:paper-api:${paperApiVersion}")
    compileOnly("io.netty:netty-transport:${project.property("netty_version")}")
}

tasks {
    runServer {
        minecraftVersion(minecraftVersion)
        jvmArgs("-XX:+AllowEnhancedClassRedefinition", "-Ddisable.watchdog=true", "-Dtorque.assets=../../torque/src/main/resources")
    }
    assemble {
        dependsOn(shadowJar)
    }
    shadowJar {
        archiveClassifier.set("")
    }
    processResources {
        val props = mapOf("version" to version)
        inputs.properties(props)
        filteringCharset = "UTF-8"
        filesMatching("plugin.yml") {
            expand(props)
        }
    }
}
