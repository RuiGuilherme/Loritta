plugins {
    kotlin("multiplatform") apply true
}

kotlin {
    jvm()
    js()

    presets.withType<org.jetbrains.kotlin.gradle.plugin.mpp.KotlinJsTargetPreset>().forEach {
        targetFromPreset(it) {
            this.nodejs
        }
    }

    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(kotlin("stdlib-common"))
                implementation(project(":loritta-api"))
            }
        }

        // Default source set for JVM-specific sources and dependencies:
        jvm().compilations["main"].defaultSourceSet {
            dependencies {
                implementation(kotlin("stdlib-jdk8"))
                implementation(project(":loritta-discord"))
            }
        }

        // Default source set for JVM-specific sources and dependencies:
        js().compilations["main"].defaultSourceSet {
            dependencies {
                implementation(kotlin("stdlib-js"))
            }
        }
    }
}