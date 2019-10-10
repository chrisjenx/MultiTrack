import com.chrisjenx.gradle.deps

plugins {
    `maven-publish`
    kotlin("jvm")
}

dependencies {
    implementation(kotlin("stdlib-jdk8"))
    implementation(deps.sqliteJdbc)
    api(project(":runtime"))

    testImplementation(project(":testing-utils"))
    testImplementation(deps.test.junit)
    testImplementation(deps.test.kotlin)
    testImplementation(deps.test.truth)
    testImplementation(deps.test.coroutines)
    testImplementation(deps.square.okio)
}

publishing {
    publications {
        create<MavenPublication>("default") {
            from(components["java"])
        }
    }
    repositories {
        maven {
            url = uri("$buildDir/repository")
        }
    }
}