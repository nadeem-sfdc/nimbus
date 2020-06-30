import org.jetbrains.dokka.gradle.DokkaTask

plugins {
    id("com.android.library")
    kotlin("android")
    kotlin("kapt")
    kotlin("plugin.serialization")
    id("org.jetbrains.dokka")
    `maven-publish`
    id("com.jfrog.bintray")
    id("com.jfrog.artifactory")
}

android {
    setDefaults()
    packagingOptions {
        excludes.add("META-INF/LICENSE.md")
        excludes.add("META-INF/LICENSE-notice.md")
        excludes.add("META-INF/DEPENDENCIES")
        excludes.add("META-INF/LICENSE")
        excludes.add("META-INF/LICENSE.txt")
        excludes.add("META-INF/license.txt")
        excludes.add("META-INF/NOTICE")
        excludes.add("META-INF/NOTICE.txt")
        excludes.add("META-INF/notice.txt")
        excludes.add("META-INF/ASL2.0")
        excludes.add("META-INF/*.kotlin_module")
    }
}

dependencies {
    implementation(nimbusModule("annotations"))
    api(nimbusModule("core"))
    implementation(Libs.k2v8)
    kapt(nimbusModule("compiler-v8"))

    api(Libs.j2v8)
    implementation(Libs.kotlinx_serialization_runtime)
    api(Libs.kotlin_stdlib)

    androidTestImplementation("io.mockk:mockk-android:${Versions.mockk}")
    androidTestImplementation(Libs.junit)
    androidTestImplementation(Libs.espresso_core)
    androidTestImplementation(Libs.androidx_test_rules) {
        exclude("com.android.support", "support-annotations")
    }
    androidTestImplementation(Libs.truth)
    androidTestImplementation(Libs.kotest_runner_junit5)
    androidTestImplementation("io.kotest:kotest-property-jvm:${Versions.kotest_runner_junit5}")
    kaptAndroidTest(nimbusModule("compiler-v8"))

    testImplementation(Libs.mockk)
    testImplementation(Libs.truth)
    testImplementation(Libs.kotest_runner_junit5)
    testImplementation("io.kotest:kotest-property-jvm:${Versions.kotest_runner_junit5}")
    kaptTest(nimbusModule("compiler-v8"))
}

tasks {
    val dokka by getting(DokkaTask::class) {
        outputFormat = "html"
        outputDirectory = "$buildDir/dokka"
    }
}

apply(from = rootProject.file("gradle/android-publishing-tasks.gradle"))

afterEvaluate {
    publishing {
        setupAllPublications(project)
    }

    bintray {
        setupPublicationsUpload(project, publishing)
    }
//    artifactory {
//        setupSnapshots(project)
//    }
}

apply(from = rootProject.file("gradle/lint.gradle"))
