plugins {
    `java-library`
    `maven-publish`
    id("me.champeau.gradle.japicmp") version "0.4.6"
}

dependencies {
    testImplementation("org.junit.jupiter:junit-jupiter:5.12.2")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher:1.12.2")
    compileOnly("com.google.errorprone:error_prone_annotations:2.50.0")
}

tasks.withType<Javadoc>().configureEach {
    exclude("**/internal/**")
    dependsOn(tasks.named("classes"))
    (options as StandardJavadocDocletOptions).addStringOption(
        "-patch-module",
        "net.iridiummc.iny.core=${layout.buildDirectory.dir("classes/java/main").get().asFile.absolutePath}"
    )
}

tasks.register<me.champeau.gradle.japicmp.JapicmpTask>("japicmp") {
    oldClasspath.from(files("build/libs/iny-core-1.0.0.jar"))
    newClasspath.from(tasks.named("jar"))
    packageExcludes.add("net.iridiummc.iny.internal.*")
    packageExcludes.add("net.iridiummc.iny.internal.**")
    onlyModified = true
    failOnModification = false
    failOnSourceIncompatibility = true
    txtOutputFile = layout.buildDirectory.file("reports/japi.txt")
}