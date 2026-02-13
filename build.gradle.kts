// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.gms.google.services) apply false
    alias(libs.plugins.ktlint) apply false
    alias(libs.plugins.firebase.crashlytics) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.hilt.android) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.sonarqube)
}

sonar {
    properties {
        property("sonar.projectKey", "bujin2502_HonNoMachi")
        property("sonar.organization", "bujin2502")
        property("sonar.host.url", "https://sonarcloud.io")
        property(
            "sonar.coverage.jacoco.xmlReportPaths",
            listOf(
                "${project(":app").layout.buildDirectory.get()}/reports/jacoco/jacocoTestReport/jacocoTestReport.xml",
                "${project(":image_uploader").layout.buildDirectory.get()}/reports/jacoco/jacocoTestReport/jacocoTestReport.xml"
            ).joinToString(",")
        )
    }
}
