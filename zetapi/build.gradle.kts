plugins {
   `java-library`
   kotlin("jvm")
   kotlin("plugin.serialization") version "2.1.0"
}

java {
   sourceCompatibility = JavaVersion.VERSION_1_8
   targetCompatibility = JavaVersion.VERSION_1_8
}

kotlin {
   jvmToolchain(8)
   explicitApi()
}

dependencies {
   implementation("com.opencsv:opencsv:5.9")
   implementation("org.mobilitydata:gtfs-realtime-bindings:0.0.8")
   implementation("androidx.collection:collection:1.4.5")
   implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3")
}
