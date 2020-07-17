import groovy.json.JsonSlurper
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.jetbrains.dokka.gradle.DokkaTask
import org.gradle.api.GradleException

buildscript {
    repositories {
        mavenCentral()
        google()
        jcenter()
    }

    dependencies {
        classpath(Libs.androidToolsBuildGradle)
        classpath(Libs.kotlinGradlePlugin)
        classpath(Libs.gradleBintrayPlugin)
        classpath(Libs.dokkaGradlePlugin)
        classpath(Libs.kotlinSerialization)
        classpath(Libs.buildInfoExtractorGradle)
    }
}

plugins {
    id("com.jfrog.artifactory")
    id("maven-publish")
    id("com.vanniktech.android.junit.jacoco") version "0.16.0"
    id("org.jetbrains.dokka") version Versions.dokkaGradlePlugin
}

allprojects {
    repositories {
        google()
        jcenter()
        maven("https://dl.bintray.com/salesforce-mobile/android")
    }
    group = getSettingValue(PublishingSettingsKey.group) ?: ""
    val versionFile = file("$rootDir/../../lerna.json")
    val parsedFile = org.json.JSONObject(versionFile.readText())
    version = parsedFile.getString("version")
    tasks.withType<KotlinCompile> {
        kotlinOptions.jvmTarget = "1.8"
    }
}

junitJacoco {
    jacocoVersion = Versions.jacoco
}

tasks.register("clean", Delete::class) {
    delete(rootProject.buildDir)
}

tasks {
    val dokka by getting(DokkaTask::class) {
        outputFormat = "html"
        outputDirectory = "$buildDir/docs"
        subProjects = listOf("core", "core-plugins", "bridge-webview", "bridge-v8")
        configuration {
            moduleName = "nimbus"
        }
    }
}

tasks.register("publishSnapshot") {
    val publishTask = tasks.findByPath("artifactoryPublish")
    if (publishTask != null) {
        this.finalizedBy(publishTask)
    } else {
        throw GradleException("Unable to find the publish task")
    }
    doLast {
        val stringVersion = version.toString()
        val regex = "[0-9]+\\.[0-9]+\\.[0-9]+".toRegex()
        val numericVersion = regex.find(stringVersion)
        if (numericVersion != null) {
            version = numericVersion.value + "-SNAPSHOT"
        } else {
            logger.error("Version in project is invalid")
            throw GradleException("Version in project is invalid")
        }
    }
}

artifactory {
    setContextUrl("http://oss.jfrog.org")
    publish(delegateClosureOf<org.jfrog.gradle.plugin.artifactory.dsl.PublisherConfig> {
        repository(delegateClosureOf<groovy.lang.GroovyObject> {
            val targetRepoKey = "oss-${buildTagFor(project.version as String)}-local"
            setProperty("repoKey", targetRepoKey)
            setProperty("username", System.getenv("BINTRAY_USER"))
            setProperty("password", System.getenv("BINTRAY_API_KEY"))
            setProperty("maven", true)
        })
        defaults(delegateClosureOf<groovy.lang.GroovyObject> {
            invokeMethod("publications", "mavenPublication")
        })
    })
}