# OnAir Studio - Android Studio Compilation & Release Guide

### Prerequisites
- Android Studio Iguana (2023.2.1+) or Ladybug (2024.2.1+)
- JDK 17 (Eclipse Temurin or Android Studio bundled OpenJDK)
- Android SDK Platform 34 (Android 14) and Android Build-Tools 34.0.0
- NDK (Side by Side) 26.1+ for low-latency audio pipelines

---

### Step 1: Open the Project in Android Studio
1. Extract the downloaded `OnAirStudio_Android_Project.zip` bundle.
2. In Android Studio, select **File > Open...** and choose the extracted root folder.
3. Allow Gradle Sync to complete all dependency downloads.

---

### Step 2: Build APK via Terminal
Open Android Studio terminal or system shell:

```bash
# Clean previous artifacts
./gradlew clean

# Build Debug APK
./gradlew assembleDebug

# Build Production Release APK (Minified with R8 & ProGuard)
./gradlew assembleRelease
```

---

### Step 3: Sign Release APK & Generate Google Play AAB Bundle
For Google Play Store submission, generate an Android App Bundle (.aab):

```bash
# Generate AAB bundle
./gradlew bundleRelease
```

The production bundle will be created at:
`app/build/outputs/bundle/release/app-release.aab`

#### Generating a Production Release Keystore:
```bash
keytool -genkey -v -keystore onair-release-key.jks \
  -keyalg RSA -keysize 2048 -validity 10000 \
  -alias onair-key-alias
```

Configure your `app/build.gradle.kts`:
```kotlin
signingConfigs {
    create("release") {
        storeFile = file("../onair-release-key.jks")
        storePassword = System.getenv("KEYSTORE_PASSWORD")
        keyAlias = "onair-key-alias"
        keyPassword = System.getenv("KEY_PASSWORD")
    }
}
```
