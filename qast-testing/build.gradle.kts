plugins {
    kotlin("plugin.serialization")
}

dependencies {
    api(project(":qast-serialization"))
    api(project(":qast-config"))
    api("org.jetbrains.kotlin:kotlin-test:2.1.0")
    api("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.10.1")
}
