// NOTE: this project uses Gradle Kotlin DSL. More common build.gradle instructions can be found in
// the main README.

android {
    defaultConfig {
        multiDexEnabled = true

        // Set to 21 to support Compose samples
        minSdk = 21
    }

    buildTypes {
        named("release").configure {
            // For the purposes of the sample, allow testing of a proguarded release build
            // using the debug key
            signingConfig = signingConfigs["debug"]

            postprocessing {
                isRemoveUnusedCode = true
                isRemoveUnusedResources = true
                isObfuscate = true
                isOptimizeCode = true
            }
        }
    }

    lint {
        disable("ResourceName", "MissingTranslation", "DuplicateStrings")
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
}

dependencies {
    implementation(Config.Libs.Androidx.design)
    implementation(Config.Libs.Androidx.multidex)

    implementation(project(":auth"))
    implementation(project(":auth-compose"))
    implementation(project(":firestore"))
    implementation(project(":database"))
    implementation(project(":storage"))

    implementation(Config.Libs.Provider.facebook)
    // Needed to override Facebook
    implementation(Config.Libs.Androidx.cardView)
    implementation(Config.Libs.Androidx.customTabs)

    implementation(Config.Libs.Misc.glide)
    annotationProcessor(Config.Libs.Misc.glideCompiler)

    // Used for FirestorePagingActivity
    implementation(Config.Libs.Androidx.paging)

    // The following dependencies are not required to use the Firebase UI library.
    // They are used to make some aspects of the demo app implementation simpler for
    // demonstrative purposes, and you may find them useful in your own apps; YMMV.
    implementation(Config.Libs.Misc.permissions)
    implementation(Config.Libs.Misc.butterKnife)
    implementation(Config.Libs.Androidx.constraint)
    annotationProcessor(Config.Libs.Misc.butterKnifeCompiler)
    debugImplementation(Config.Libs.Misc.leakCanary)
    debugImplementation(Config.Libs.Misc.leakCanaryFragments)
    releaseImplementation(Config.Libs.Misc.leakCanaryNoop)
    testImplementation(Config.Libs.Misc.leakCanaryNoop)
}

apply(plugin = "com.google.gms.google-services")
