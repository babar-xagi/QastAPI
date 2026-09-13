plugins {
    application
}

application {
    mainClass.set("io.qastapi.cli.MainKt")
}

tasks.named<JavaExec>("run") {
    val cliCwd = System.getenv("QAST_CWD")
    if (cliCwd != null) {
        workingDir = file(cliCwd)
    }
}

dependencies {
    implementation(project(":qast-core"))
    implementation(project(":qast-http"))
    implementation(project(":qast-routing"))
    implementation(project(":qast-serialization"))
    implementation(project(":qast-config"))
}
