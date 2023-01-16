import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    `java-library`
    `maven-publish`
    kotlin("jvm") version Versions.kotlin
    kotlin("plugin.serialization") version Versions.kotlin
    id("org.jetbrains.dokka") version Versions.kotlin
}

group = "host.skyone"
version = "1.2.0"

allprojects {
    repositories {
        mavenCentral()
        mavenLocal()
    }
}

dependencies {
    compileOnly("org.jetbrains.kotlinx:kotlinx-coroutines-core:${Versions.coroutines}")
    compileOnly("org.slf4j:slf4j-log4j12:${Versions.slf4j}")

    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:${Versions.coroutines}")
    testImplementation("org.jetbrains.kotlinx:kotlinx-serialization-json:${Versions.serialization}")
    testImplementation("org.slf4j:slf4j-log4j12:${Versions.slf4j}")
    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "1.8"
}

tasks.jar {
    makeManifast()
}

tasks.create<Jar>("sourcesJar") {
    archiveClassifier.set("sources")
    from(sourceSets.main.get().allSource)
    makeManifast()
}

val dokkaJavadocJar by tasks.register<Jar>("dokkaJavadocJar") {
    dependsOn(tasks.dokkaJavadoc)
    from(tasks.dokkaJavadoc.flatMap { it.outputDirectory })
    archiveClassifier.set("javadoc")
    makeManifast()
}

val dokkaHtmlJar by tasks.register<Jar>("dokkaHtmlJar") {
    dependsOn(tasks.dokkaHtml)
    from(tasks.dokkaHtml.flatMap { it.outputDirectory })
    archiveClassifier.set("html-doc")
    makeManifast()
}

publishing {
    repositories {
        maven {
            name = "GitHubPackages"
            url = uri("https://maven.pkg.github.com/skyone-wzw/ktconf")
            credentials {
                username = System.getenv("USERNAME")
                password = System.getenv("TOKEN")
            }
        }
    }
    publications {
        register<MavenPublication>("ConfigureManager") {
            from(components["java"])
            artifact(tasks.getByName("sourcesJar"))
            artifact(dokkaJavadocJar)
            artifact(dokkaHtmlJar)

            groupId = project.group as String
            artifactId = project.name
            version = project.version as String
            pom {
                packaging = "jar"
                name.set("Skyone Configure Manager")
                description.set("Skyone Configure Manager ${project.version}")
                url.set("https://github.com/skyone-wzw/ktconf")
                scm {
                    connection.set("scm:git@github.com:skyone-wzw/ktconf.git")
                    developerConnection.set("scm:git@github.com:skyone-wzw/ktconf.git")
                    url.set("https://github.com/skyone-wzw/ktconf.git")
                }
                issueManagement {
                    url.set("https://github.com/skyone-wzw/ktconf/issues")
                }
                developers {
                    developer {
                        id.set("skyone")
                        name.set("skyone-wzw")
                    }
                }
            }
        }
    }
}