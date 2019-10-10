import com.chrisjenx.gradle.deps
import org.gradle.jvm.tasks.Jar

plugins {
    `maven-publish`
    id("com.android.library")
    kotlin("android")
}

android {
    compileSdkVersion(deps.versions.compileSdk)
    testOptions {
        unitTests.isIncludeAndroidResources = true
    }
    defaultConfig {
        minSdkVersion(deps.versions.minSdk)
    }

}

dependencies {
    implementation(kotlin("stdlib-jdk8"))
    implementation(deps.android.sqlite)
    implementation(deps.android.sqliteFramework)
    api(project(":runtime"))

    testImplementation(project(":testing-utils"))
    testImplementation(deps.test.junit)
    testImplementation(deps.test.kotlin)
    testImplementation(deps.test.truth)
    testImplementation(deps.square.okio)
    testImplementation(deps.test.robolectric)
    testImplementation(deps.test.androixCore)
    testImplementation(deps.test.androidRunnner)
}

val sourcesJar = task<Jar>("androidSourcesJar") {
    archiveClassifier.convention("sources")
    archiveClassifier.set("sources")
    from(android.sourceSets["main"].java.srcDirs)
}

afterEvaluate {
    publishing {
        publications {
            create<MavenPublication>("MyPublication") {
                artifact(tasks["bundleReleaseAar"])
                artifact(sourcesJar).apply { classifier = "sources" }
                pom.withXml {
                    val dependenciesNode = asNode().appendNode("dependencies")
                    //Iterate over the compile dependencies (we don't want the test ones), adding a <dependency> node for each
                    configurations.implementation.get().allDependencies.forEach {
                        if (it.group != null && it.version != null) {
                            val dependencyNode = dependenciesNode.appendNode("dependency")
                            dependencyNode.appendNode("groupId", it.group)
                            dependencyNode.appendNode("artifactId", it.name)
                            dependencyNode.appendNode("version", it.version)
                            dependencyNode.appendNode("scope", "runtime")
                        }
                    }
                    configurations.api.get().allDependencies.forEach {
                        if (it.group != null && it.version != null) {
                            val dependencyNode = dependenciesNode.appendNode("dependency")
                            dependencyNode.appendNode("groupId", it.group)
                            dependencyNode.appendNode("artifactId", it.name)
                            dependencyNode.appendNode("version", it.version)
                            dependencyNode.appendNode("scope", "compile")
                        }
                    }
                }
            }
        }
        repositories {
            maven {
                url = uri("$buildDir/repository")
            }
        }
    }
}
