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

val coreJar = project(":iny-core").layout.buildDirectory.file(
    "libs/iny-core-${project.version}.jar"
)
val coreSources = project(":iny-core").layout.projectDirectory.dir("src/main/java")

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
    compileJava {
        dependsOn(":iny-core:jar")
        options.compilerArgs.addAll(listOf(
            "-Xlint:-requires-transitive-automatic",
            "--patch-module",
            "net.iridiummc.iny.bukkit=${coreJar.get().asFile.absolutePath}"
        ))
    }

    jar {
        dependsOn(bundledCore)
        from({ bundledCore.map(::zipTree) }) {
            exclude("META-INF/MANIFEST.MF", "module-info.class")
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

    sourcesJar {
        from(coreSources) {
            exclude("module-info.java")
        }
    }

    javadoc {
        source(coreSources.asFileTree.matching {
            exclude("module-info.java")
        })
        exclude("**/internal/**", "**/minecraft/**")
        dependsOn("classes")
        (options as StandardJavadocDocletOptions).addStringOption(
            "-patch-module",
            "net.iridiummc.iny.bukkit=${layout.buildDirectory.dir("classes/java/main").get().asFile.absolutePath}"
                    + File.pathSeparator + coreJar.get().asFile.absolutePath
        )
    }
}
