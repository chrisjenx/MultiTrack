import com.chrisjenx.gradle.deps

plugins {
    kotlin("jvm")
}

dependencies {
    implementation(kotlin("stdlib-jdk8"))
    implementation(project(":base"))
    implementation(deps.test.junit)
    implementation(deps.test.kotlin)
    implementation(deps.square.okio)
    implementation(deps.test.truth)
    implementation(deps.test.coroutines)
}
