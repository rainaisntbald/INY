plugins {
    `java-library`
    `maven-publish`
    id("xyz.jpenilla.run-paper") version "3.0.2"
}

val bundledCore = configurations.create("bundledCore") {
    isCanBeConsumed = false
    isCanBeResolved = true
    isTransitive = false
}

dependencies {
    compileOnly(project(":iny-core"))
    bundledCore(project(":iny-core"))
    testImplementation(project(":iny-core"))

    compileOnly("io.papermc.paper:paper-api:26.1.2.build.74-stable")
    testImplementation("io.papermc.paper:paper-api:26.1.2.build.74-stable")
    testImplementation("org.junit.jupiter:junit-jupiter:5.12.2")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher:1.12.2")
}

tasks {
    jar {
        dependsOn(bundledCore)
        from({ bundledCore.map(::zipTree) }) {
            exclude("META-INF/MANIFEST.MF")
        }
    }

    runServer {
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
