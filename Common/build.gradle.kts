apply(plugin = "com.android.library")
apply(plugin = "org.jetbrains.kotlin.android")

android {
    namespace = "com.cloudstreamext.common"
    compileSdk = 34

    defaultConfig {
        minSdk = 21
        targetSdk = 34
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }
}

dependencies {
    implementation("com.github.recloudstream:cs3sources:3.0.0-alpha1")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("org.jsoup:jsoup:1.17.1")
    implementation("com.google.code.gson:gson:2.10.1")
}
