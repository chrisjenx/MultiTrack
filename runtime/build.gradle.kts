import com.chrisjenx.gradle.deps

plugins {
    `maven-publish`
    kotlin("jvm")
}

dependencies {
    implementation(kotlin("stdlib-jdk8"))
    implementation(deps.kotlin.coroutines)
    implementation(deps.square.okio)
    api(project(":base"))

    testImplementation(project(":testing-utils"))
    testImplementation(deps.test.junit)
    testImplementation(deps.test.kotlin)
    testImplementation(deps.test.truth)
    testImplementation(deps.test.coroutines)
}

publishing {
    publications {
        create<MavenPublication>("default") {
            from(components["java"])
//            artifact(dokkaJar)
        }
    }
    repositories {
        maven {
            url = uri("$buildDir/repository")
        }
    }
}