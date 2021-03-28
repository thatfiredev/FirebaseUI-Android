plugins {
    id("kotlin")
}

dependencies {
    compileOnly(Config.Libs.Lint.api)
    compileOnly(Config.Libs.Kotlin.jvm)

    testImplementation(Config.Libs.Lint.api)
    testImplementation(Config.Libs.Lint.tests)
    testImplementation(Config.Libs.Test.junit)
}

tasks.withType<Jar>().configureEach {
    manifest {
        attributes(mapOf("Lint-Registry-v2" to "com.firebaseui.lint.internal.LintIssueRegistry"))
    }
}
