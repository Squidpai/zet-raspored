plugins {
   id("com.android.application")
   id("org.jetbrains.kotlin.android")
   id("org.jetbrains.kotlin.plugin.compose")
}

android {
   namespace = "hr.squidpai.zetlive"
   compileSdk = 34

   defaultConfig {
      applicationId = "hr.squidpai.zetlive"
      minSdk = 26
      targetSdk = 35
      versionCode = 16
      versionName = "0.8.2"

      testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
      vectorDrawables {
         useSupportLibrary = true
      }
   }

   buildTypes {
      release {
         isMinifyEnabled = false
         proguardFiles(
            getDefaultProguardFile("proguard-android-optimize.txt"),
            "proguard-rules.pro"
         )
      }
   }
   compileOptions {
      sourceCompatibility = JavaVersion.VERSION_1_8
      targetCompatibility = JavaVersion.VERSION_1_8
   }
   kotlinOptions {
      jvmTarget = "1.8"
   }
   buildFeatures {
      compose = true
   }
   packaging {
      resources {
         excludes += "/META-INF/{AL2.0,LGPL2.1}"
      }
   }
}

dependencies {
   implementation(project(":zetapi"))
   implementation("androidx.core:core-ktx:1.13.1")
   implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.6")
   implementation("androidx.activity:activity-compose:1.9.2")
   implementation(platform("androidx.compose:compose-bom:2024.09.02"))
   implementation("androidx.compose.ui:ui:1.7.2")
   implementation("androidx.compose.ui:ui-graphics:1.7.2")
   implementation("androidx.compose.ui:ui-tooling-preview:1.7.2")
   implementation("androidx.compose.material3:material3:1.3.0")
   implementation("com.google.android.play:app-update:2.1.0")
   implementation("com.google.android.play:app-update-ktx:2.1.0")
   implementation("androidx.lifecycle:lifecycle-process:2.8.7")
   testImplementation("junit:junit:4.13.2")
   androidTestImplementation("androidx.test.ext:junit:1.2.1")
   androidTestImplementation("androidx.test.espresso:espresso-core:3.6.1")
   androidTestImplementation(platform("androidx.compose:compose-bom:2024.09.02"))
   androidTestImplementation("androidx.compose.ui:ui-test-junit4:1.7.2")
   debugImplementation("androidx.compose.ui:ui-tooling:1.7.2")
   debugImplementation("androidx.compose.ui:ui-test-manifest:1.7.2")
}