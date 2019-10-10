package com.chrisjenx.gradle

object deps {

    object versions {
        const val compileSdk = 29
        const val minSdk = 21
        const val kotlin = "1.3.50"
        const val coroutines = "1.3.2"
        const val okio = "2.2.2"
        const val archDb = "1.1.0-beta2"
    }

    object android {
        const val classpath = "com.android.tools.build:gradle:3.5.1"
        const val sqlite = "android.arch.persistence:db:${versions.archDb}"
        const val sqliteFramework = "android.arch.persistence:db-framework:${versions.archDb}"
    }

    object kotlin {
        const val classpath = "org.jetbrains.kotlin:kotlin-gradle-plugin:${versions.kotlin}"
        const val runtime = "org.jetbrains.kotlin:kotlin-jvm-stdlib-jdk8"
        const val coroutines = "org.jetbrains.kotlinx:kotlinx-coroutines-core:${versions.coroutines}"
        const val coroutinesRxJava = "org.jetbrains.kotlinx:kotlinx-coroutines-rx2:${versions.coroutines}"
    }

    object square {
        const val okio = "com.squareup.okio:okio:${versions.okio}"
    }

    object jitpack {
        const val classpath = "com.github.dcendents:android-maven-gradle-plugin:2.1"
    }

    object test {
        const val junit = "junit:junit:4.12"
        const val kotlin = "org.jetbrains.kotlin:kotlin-test-junit"
        const val truth = "com.google.truth:truth:1.0"
        const val coroutines = "org.jetbrains.kotlinx:kotlinx-coroutines-test:${versions.coroutines}"
        const val robolectric = "org.robolectric:robolectric:4.1"
        const val androixCore = "androidx.test:core:1.2.0"
        const val androidRunnner = "androidx.test.ext:junit:1.1.1"
    }

    const val sqliteJdbc = "org.xerial:sqlite-jdbc:3.21.0.1"
    const val rxjava = "io.reactivex.rxjava2:rxjava:2.2.12"
}
