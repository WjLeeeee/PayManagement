import java.util.Properties

plugins {
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.kotlinAndroid)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.play.publisher)
    alias(libs.plugins.google.services)
}

// Play Store 배포 설정
play {
    serviceAccountCredentials.set(file("../play-store-credentials.json"))
    track.set("internal")  // internal, alpha, beta, production 중 선택
    defaultToAppBundles.set(true)  // AAB 파일 사용
    releaseStatus.set(com.github.triplet.gradle.androidpublisher.ReleaseStatus.COMPLETED)
    releaseName.set(provider { "${android.defaultConfig.versionCode} (${android.defaultConfig.versionName})" })
}

android {
    namespace = "com.woojin.paymanagement.android"
    compileSdk = 35
    defaultConfig {
        applicationId = "com.woojin.paymanagement.android"
        minSdk = 28
        targetSdk = 35
        versionCode = 34
        versionName = "1.8"
    }

    // 서명 설정
    signingConfigs {
        create("release") {
            val keystorePropertiesFile = rootProject.file("keystore.properties")
            if (keystorePropertiesFile.exists()) {
                // 로컬 개발: keystore.properties 파일 사용
                val keystoreProperties = Properties()
                keystoreProperties.load(keystorePropertiesFile.inputStream())

                storeFile = rootProject.file(keystoreProperties["storeFile"] as String)
                storePassword = keystoreProperties["storePassword"] as String
                keyAlias = keystoreProperties["keyAlias"] as String
                keyPassword = keystoreProperties["keyPassword"] as String
            } else {
                // Jenkins CI: 환경변수 사용
                storeFile = System.getenv("KEYSTORE_FILE")?.let { file(it) }
                    ?: rootProject.file("KeyStorePath.jks")
                storePassword = System.getenv("KEYSTORE_PASSWORD")
                keyAlias = System.getenv("KEY_ALIAS")
                keyPassword = System.getenv("KEY_PASSWORD")
            }
        }
    }

    buildFeatures {
        compose = true
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
    buildTypes {
        getByName("release") {
            isMinifyEnabled = true
            signingConfig = signingConfigs.getByName("release")
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        getByName("debug") {
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName("release")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }
}

dependencies {
    implementation(projects.shared)
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.compose.material3)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.work.runtime)
    implementation(libs.koin.android)
    implementation(libs.koin.compose)
    implementation(libs.kotlinx.datetime)
    implementation(libs.billing)

    // AdMob
    implementation("com.google.android.gms:play-services-ads:23.0.0")
    //Meta Audience Network 어뎁터
    implementation("com.google.ads.mediation:facebook:6.21.0.0")

    // Firebase
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.analytics)

    debugImplementation(libs.compose.ui.tooling)
}