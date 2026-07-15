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

val moduleInfoClasspath = configurations.create("moduleInfoClasspath") {
    isCanBeConsumed = false
    isCanBeResolved = true
}
val moduleInfoModules = moduleInfoClasspath.incoming.artifactView {
    componentFilter {
        it is org.gradle.api.artifacts.component.ModuleComponentIdentifier
                && it.group == "io.papermc.paper"
                && it.module == "paper-api"
    }
}.files

val coreJar = project(":iny-core").layout.buildDirectory.file(
    "libs/iny-core-${project.version}.jar"
)
val coreSources = project(":iny-core").layout.projectDirectory.dir("src/main/java")
val moduleInfoSource = layout.projectDirectory.file("src/module/module-info.java")
val moduleInfoOutput = layout.buildDirectory.dir("classes/java/module")
val paperApi = "io.papermc.paper:paper-api:26.1.2.build.74-stable"

dependencies {
    compileOnly(project(":iny-core"))
    bundledCore(project(":iny-core"))
    testImplementation(project(":iny-core"))

    compileOnly(paperApi)
    moduleInfoClasspath(paperApi)
    testImplementation(paperApi)
    testImplementation("org.junit.jupiter:junit-jupiter:5.12.2")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher:1.12.2")
}

val compileModuleInfo = tasks.register<JavaCompile>("compileModuleInfo") {
    dependsOn("classes", ":iny-core:jar")
    source(moduleInfoSource.asFile.parentFile)
    destinationDirectory.set(moduleInfoOutput)
    javaCompiler.set(javaToolchains.compilerFor {
        languageVersion.set(JavaLanguageVersion.of(25))
    })
    classpath = files()
    inputs.files(moduleInfoModules)
    options.compilerArgs.addAll(listOf(
        "-Xlint:-requires-transitive-automatic",
        "--module-path",
        moduleInfoModules.asPath,
        "--patch-module",
        "net.iridiummc.iny.bukkit=${sourceSets.main.get().output.classesDirs.asPath}"
                + File.pathSeparator + coreJar.get().asFile.absolutePath
    ))
}

tasks {
    jar {
        dependsOn(bundledCore, compileModuleInfo)
        from(moduleInfoOutput)
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
        from(moduleInfoSource)
        from(coreSources) {
            exclude("module-info.java")
        }
    }

    javadoc {
        source(coreSources.asFileTree.matching {
            exclude("module-info.java")
        })
        exclude("**/internal/**", "**/minecraft/**")
        modularity.inferModulePath.set(false)
    }

    test {
        dependsOn(compileModuleInfo)
    }
}
