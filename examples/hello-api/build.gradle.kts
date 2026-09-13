plugins {
    application
    kotlin("plugin.serialization")
}

application {
    mainClass.set("io.qastapi.example.MainKt")
}

dependencies {
    implementation(project(":qast-core"))
    implementation(project(":qast-http"))
    implementation(project(":qast-routing"))
    implementation(project(":qast-serialization"))
    implementation(project(":qast-config"))
}
