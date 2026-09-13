plugins {
    kotlin("plugin.serialization")
}

dependencies {
    api(project(":qast-routing"))
    api("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3")
}
