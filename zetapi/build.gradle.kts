plugins {
   `java-library`
   kotlin("jvm")
   kotlin("plugin.serialization") version "2.1.0"
}

java {
   sourceCompatibility = JavaVersion.VERSION_21
   targetCompatibility = JavaVersion.VERSION_21
}

kotlin {
   explicitApi()
}

dependencies {
   implementation("com.opencsv:opencsv:5.11.2")
   implementation("org.mobilitydata:gtfs-realtime-bindings:0.0.8")
   implementation("androidx.collection:collection:1.5.0")
   implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.9.0")
   implementation("com.charleskorn.kaml:kaml:0.85.0")
}
