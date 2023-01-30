import org.jetbrains.intellij.IntelliJPluginExtension

plugins {
    java
    id("org.jetbrains.intellij") version "1.10.1"
    id("org.jetbrains.kotlin.jvm") version "1.8.0"
}

group = "org.jetbrains.research.refactorinsight"
version = "2022.3-1.0"

repositories {
    mavenCentral()
    maven(url = "https://jitpack.io")
}

subprojects {
    parent?.repositories?.forEach { repositories.add(it) }

    pluginManager.withPlugin("org.jetbrains.intellij") {
        intellij(configureIntelliJ)
    }

    tasks.withType<Test> {
        useJUnitPlatform()
    }
}

val configureIntelliJ: Action<IntelliJPluginExtension> = Action {
    version.set("IC-2022.3")
    plugins.set(listOf("com.intellij.java", "Git4Idea", "org.jetbrains.plugins.github"))
    downloadSources.set(true)
}

intellij(configureIntelliJ)