subprojects {
    tasks.withType<Sign>().configureEach {
        enabled = false
    }
}