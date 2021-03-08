android {
    buildTypes {
        named("release").configure {
            isMinifyEnabled = false
            consumerProguardFiles("auth-compose-proguard.pro")
        }
    }

    lint {
        disable("UnusedQuantity")
        disable("UnknownNullness")  // TODO fix in future PR
        disable("TypographyQuotes") // Straight versus directional quotes
        disable("DuplicateStrings")
        disable("LocaleFolder")
        disable("IconLocation")
        disable("VectorPath")
    }

    testOptions {
        unitTests {
            isIncludeAndroidResources = true
        }
    }

    buildFeatures {
        compose = true
    }

    composeOptions {
        kotlinCompilerExtensionVersion = Config.Libs.Androidx.Compose.composeVersion
    }
}

dependencies {
    implementation(project(":auth"))

    implementation(Config.Libs.Androidx.activity)
    // The new activity result APIs force us to include Fragment 1.3.0
    // See https://issuetracker.google.com/issues/152554847
    implementation(Config.Libs.Androidx.fragment)

    implementation(Config.Libs.Androidx.Compose.ui)
    implementation(Config.Libs.Androidx.Compose.ui_tooling)
    implementation(Config.Libs.Androidx.Compose.foundation)
    implementation(Config.Libs.Androidx.Compose.material)
    implementation(Config.Libs.Androidx.Compose.activity)

    testImplementation(Config.Libs.Test.junit)
    testImplementation(Config.Libs.Test.truth)
    testImplementation(Config.Libs.Test.mockito)
    testImplementation(Config.Libs.Test.core)
    testImplementation(Config.Libs.Test.robolectric)

    debugImplementation(project(":internal:lintchecks"))
}
