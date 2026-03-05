import org.gradle.api.tasks.compile.JavaCompile

fun gitVersionName(): String =
    try {
        val tag =
            ProcessBuilder("git", "describe", "--tags", "--abbrev=0")
                .directory(rootDir)
                .start()
                .inputStream
                .bufferedReader()
                .readLine()
                ?.trim()
                ?: "v1.0.0"
        if (tag.startsWith("v")) tag.substring(1) else tag
    } catch (_: Exception) {
        "1.0.0"
    }

fun gitVersionCode(): Int {
    val parts = gitVersionName().split(".")
    val major = parts.getOrNull(0)?.toIntOrNull() ?: 1
    val minor = parts.getOrNull(1)?.toIntOrNull() ?: 0
    val patch = parts.getOrNull(2)?.toIntOrNull() ?: 0
    return major * 10000 + minor * 100 + patch
}

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.gms.google.services)
    alias(libs.plugins.ktlint)
    alias(libs.plugins.firebase.crashlytics)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt.android)
    jacoco
}

ktlint {
    reporters {
        reporter(org.jlleitschuh.gradle.ktlint.reporter.ReporterType.HTML)
        reporter(org.jlleitschuh.gradle.ktlint.reporter.ReporterType.PLAIN)
    }
}

android {
    namespace = "hr.foi.air.honnomachi"
    compileSdk = 36

    signingConfigs {
        create("release") {
            val keystorePath = System.getenv("KEYSTORE_PATH")
            val keystorePassword = System.getenv("KEYSTORE_PASSWORD")
            val keyAliasValue = System.getenv("KEY_ALIAS")
            val keyPasswordValue = System.getenv("KEY_PASSWORD")
            if (keystorePath != null &&
                keystorePassword != null &&
                keyAliasValue != null &&
                keyPasswordValue != null
            ) {
                storeFile = file(keystorePath)
                storePassword = keystorePassword
                keyAlias = keyAliasValue
                keyPassword = keyPasswordValue
            }
        }
    }

    defaultConfig {
        applicationId = "hr.foi.air.honnomachi"
        minSdk = 26
        targetSdk = 36
        versionCode = gitVersionCode()
        versionName = gitVersionName()

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        manifestPlaceholders["imageUploaderAuthority"] = "hr.foi.air.honnomachi.provider"

        multiDexEnabled = true
    }

    buildTypes {
        release {
            val releaseSigningConfig = signingConfigs.getByName("release")
            if (releaseSigningConfig.storeFile != null) {
                signingConfig = releaseSigningConfig
            }
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
        debug {
            isMinifyEnabled = false
            enableUnitTestCoverage = true
            enableAndroidTestCoverage = true
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlin {
        jvmToolchain(17)
    }
    buildFeatures {
        compose = true
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
            excludes += "META-INF/LICENSE.md"
            excludes += "META-INF/LICENSE-notice.md"
        }
    }
}

configurations.all {
    resolutionStrategy {
        force("com.google.android.gms:play-services-auth:21.4.0")
    }
}

dependencies {
    // Core
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.activity.compose)
    implementation(libs.multidex)
    implementation(project(":image_uploader"))

    // Compose
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.material.icons.extended)
    implementation(libs.androidx.compose.foundation.layout)
    implementation(libs.androidx.compose.foundation)

    // Firebase
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.auth)
    implementation(libs.firebase.firestore)
    implementation(libs.firebase.analytics)
    implementation(libs.firebase.crashlytics)

    // Auth
    implementation(libs.credentials)
    implementation(libs.credentials.play.services.auth)
    implementation(libs.googleid)
    implementation(libs.play.services.auth)
    implementation(libs.kotlinx.coroutines.play.services)

    // Hilt
    implementation(libs.hilt.android)
    implementation(libs.hilt.navigation.compose)

    // Other
    implementation(libs.ads.mobile.sdk)
    implementation(libs.coil.compose)
    implementation(libs.accompanist.permissions)

    // Annotation processors
    ksp(libs.hilt.compiler)

    // Unit tests
    testImplementation(libs.junit)
    testImplementation(platform(libs.androidx.compose.bom))
    testImplementation(libs.androidx.compose.ui.test.junit4)
    testImplementation(libs.androidx.compose.ui.test)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.androidx.navigation.testing)
    testImplementation(libs.mockk)

    // Instrumented tests
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.compose.ui.test)
    androidTestImplementation(libs.androidx.navigation.testing)
    androidTestImplementation(libs.mockk.android)

    // Debug
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}

tasks.withType<JavaCompile>().configureEach {
    if (name.startsWith("hiltJavaCompile")) {
        options.compilerArgs.addAll(listOf("-Xlint:none", "-nowarn"))
    }
}

tasks.register<JacocoReport>("jacocoTestReport") {
    group = "verification"
    description = "Generates Jacoco code coverage report for debug unit tests."
    dependsOn("testDebugUnitTest")

    reports {
        xml.required.set(true)
        html.required.set(true)
    }

    val fileFilter =
        listOf(
            "**/R.class",
            "**/R$*.class",
            "**/BuildConfig.*",
            "**/Manifest*.*",
            "**/*Test*.*",
            "android/**/*.*",
            "**/*_Hilt*.*",
            "**/Hilt_*.*",
            "**/*_Factory.*",
            "**/*_MembersInjector.*",
            "**/*Module.*",
            "**/*Dagger*.*",
            "**/*_Provide*Factory*.*",
        )

    val debugTree =
        fileTree("${layout.buildDirectory.get()}/tmp/kotlin-classes/debug") {
            exclude(fileFilter)
        }

    val mainSrc = "${project.projectDir}/src/main/java"

    sourceDirectories.setFrom(files(mainSrc))
    classDirectories.setFrom(files(debugTree))
    executionData.setFrom(
        fileTree(layout.buildDirectory.get()) {
            include(
                "jacoco/testDebugUnitTest.exec",
                "outputs/unit_test_code_coverage/debugUnitTest/testDebugUnitTest.exec",
            )
        },
    )
}
