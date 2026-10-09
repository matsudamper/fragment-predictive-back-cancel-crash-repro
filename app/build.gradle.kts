plugins {
    id("com.android.application")
}

android {
    namespace = "com.example.fragmentrepro"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.example.fragmentrepro"
        minSdk = 24
        targetSdk = 37
        versionCode = 1
        versionName = "1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    testOptions {
        unitTests {
            isIncludeAndroidResources = true
            all {
                it.testLogging { showStandardStreams = true }
                // Robolectric needs this to set up SDK 37 on recent JDKs
                it.jvmArgs("--add-exports=java.base/jdk.internal.access=ALL-UNNAMED")
            }
        }
        managedDevices {
            localDevices {
                // Android 14, same as the real device where the bug was seen.
                // Do not use ATD images: they strip SystemUI, so the back gesture may not work.
                create("pixel6Api34") {
                    device = "Pixel 6"
                    apiLevel = 34
                    systemImageSource = "google"
                    testedAbi = "x86_64"
                }
                create("pixel6Api37") {
                    device = "Pixel 6"
                    apiLevel = 37
                    systemImageSource = "google"
                    testedAbi = "x86_64"
                }
            }
        }
    }
}

dependencies {
    implementation("androidx.fragment:fragment-ktx:1.9.1")
    implementation("androidx.activity:activity:1.13.0")
    implementation("androidx.viewpager2:viewpager2:1.1.0")

    testImplementation("junit:junit:4.13.2")
    testImplementation("androidx.test.ext:junit:1.3.0")
    testImplementation("org.robolectric:robolectric:4.17")

    androidTestImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test:runner:1.7.0")
    androidTestImplementation("androidx.test.ext:junit:1.3.0")
}
