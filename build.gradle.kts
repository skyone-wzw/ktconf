import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    `java-library`
    `maven-publish`
    kotlin("jvm") version Versions.kotlin
    kotlin("plugin.serialization") version Versions.kotlin
}

group = "host.skyone"
version = "1.0.0"

allprojects {
    repositories {
        if (System.getenv("GRADLE_CHINA_MIRROR") == "true") {
            logger.log(LogLevel.INFO, "Build in Local, use mirror")
            maven(url = "https://maven.aliyun.com/repository/public/")
            mavenLocal()
        }
        mavenCentral()
    }
}

dependencies {
    api("org.jetbrains.kotlinx:kotlinx-coroutines-core:${Versions.coroutines}")
    api("org.slf4j:slf4j-log4j12:${Versions.slf4j}")

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

tasks.create<Jar>("sourcesJar") {
    archiveClassifier.set("sources")
    from(sourceSets.main.get().allSource)
}

tasks.create<Jar>("javadocJar") {
    dependsOn.add(tasks.getByName("javadoc"))
    archiveClassifier.set("javadoc")
    from(tasks.getByName("javadoc"))
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
        register<MavenPublication>("gpr") {
            from(components["java"])
            artifact(tasks.getByName("sourcesJar"))
            artifact(tasks.getByName("javadocJar"))

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
                        email.set("skyone.wzw@qq.com")
                    }
                }
            }
        }
    }
}