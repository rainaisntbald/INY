plugins {
    `java-library`
    `maven-publish`
}

dependencies {
    testImplementation("org.junit.jupiter:junit-jupiter:5.12.2")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher:1.12.2")
}

tasks.withType<Javadoc>().configureEach {
    exclude("**/internal/**")
    dependsOn(tasks.named("classes"))
    (options as StandardJavadocDocletOptions).addStringOption(
        "-patch-module",
        "net.iridiummc.iny.core=${layout.buildDirectory.dir("classes/java/main").get().asFile.absolutePath}"
    )
}
