plugins {
    id("java")
}

version = property("version") as String
group = property("maven_group") as String

repositories {
    mavenCentral()
}

dependencies {
    compileOnly("org.jetbrains:annotations:${project.property("jetbrains_annotations_version")}")
    compileOnly("org.joml:joml:${project.property("joml_version")}")
    compileOnly("com.google.code.gson:gson:${project.property("gson_version")}")
    compileOnly("it.unimi.dsi:fastutil:${project.property("fastutil_version")}")
    testImplementation(platform("org.junit:junit-bom:5.10.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
}

tasks.test {
    useJUnitPlatform()
}