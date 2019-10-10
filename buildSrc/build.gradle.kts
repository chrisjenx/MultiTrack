
plugins {
    `kotlin-dsl`
}

apply {
    plugin("kotlin")
}

repositories {
    google()
    mavenCentral()
    jcenter()
    maven("https://plugins.gradle.org/m2/")
}

dependencies {
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
}