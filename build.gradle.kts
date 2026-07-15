import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.api.tasks.testing.Test

plugins {
    base
}

allprojects {
    group = rootProject.group
    version = rootProject.version
}

subprojects {
    repositories {
        mavenCentral()
        maven("https://repo.papermc.io/repository/maven-public/")
    }

    plugins.withId("java") {
        extensions.configure<JavaPluginExtension> {
            toolchain.languageVersion = JavaLanguageVersion.of(25)
            withSourcesJar()
            withJavadocJar()
        }

        tasks.withType<Test>().configureEach {
            useJUnitPlatform()
        }
    }

    plugins.withId("maven-publish") {
        extensions.configure<PublishingExtension> {
            publications {
                create<MavenPublication>("mavenJava") {
                    from(components["java"])
                }
            }

            val repositoryUrl = providers.gradleProperty("mavenRepositoryUrl")
                .orElse(providers.environmentVariable("MAVEN_REPOSITORY_URL"))
            repositories {
                if (repositoryUrl.isPresent) {
                    maven {
                        name = "configured"
                        url = uri(repositoryUrl.get())
                        credentials {
                            username = providers.gradleProperty("mavenUsername")
                                .orElse(providers.environmentVariable("MAVEN_USERNAME")).orNull
                            password = providers.gradleProperty("mavenPassword")
                                .orElse(providers.environmentVariable("MAVEN_PASSWORD")).orNull
                        }
                    }
                }
            }
        }
    }
}
