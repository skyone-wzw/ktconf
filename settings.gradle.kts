pluginManagement {
    repositories {
        if (System.getenv("GRADLE_CHINA_MIRROR") == "true") {
            logger.log(LogLevel.INFO, "Build in Local, use mirror")
            maven(url = "https://maven.aliyun.com/repository/gradle-plugin/")
            mavenLocal()
        }
        gradlePluginPortal()
        mavenCentral()
    }
}

rootProject.name = "configure-manager"