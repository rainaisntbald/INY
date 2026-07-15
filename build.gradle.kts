plugins {
    id("java-library")
    id("maven-publish")
    id("xyz.jpenilla.run-paper") version "3.0.2"
}

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:26.1.1.build.+")
    testImplementation("io.papermc.paper:paper-api:26.1.1.build.+")
    testImplementation("org.junit.jupiter:junit-jupiter:5.12.2")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher:1.12.2")
}

tasks.test { useJUnitPlatform() }

java {
    toolchain.languageVersion = JavaLanguageVersion.of(25)
    withSourcesJar()
    withJavadocJar()
}

publishing {
    publications {
        create<MavenPublication>("mavenJava") { from(components["java"]) }
    }
    val repositoryUrl = providers.gradleProperty("mavenRepositoryUrl").orElse(providers.environmentVariable("MAVEN_REPOSITORY_URL"))
    repositories {
        if (repositoryUrl.isPresent) maven {
            name = "configured"
            url = uri(repositoryUrl.get())
            credentials {
                username = providers.gradleProperty("mavenUsername").orElse(providers.environmentVariable("MAVEN_USERNAME")).orNull
                password = providers.gradleProperty("mavenPassword").orElse(providers.environmentVariable("MAVEN_PASSWORD")).orNull
            }
        }
    }
}

tasks {
    runServer {
        // Configure the Minecraft version for our task.
        // This is the only required configuration besides applying the plugin.
        // Your plugin's jar (or shadowJar if present) will be used automatically.
        minecraftVersion("26.1.1")
        jvmArgs("-Xms2G", "-Xmx2G")
    }

    processResources {
        val props = mapOf("version" to version)
        filesMatching("plugin.yml") {
            expand(props)
        }
    }
}
