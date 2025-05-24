plugins {
    id("com.android.library")
}

android {
    namespace = "com.medrick.mlkit"
    compileSdk = 34

    defaultConfig {
        minSdk = 23
        targetSdk = 34
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
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
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    packagingOptions {
        resources {
            pickFirsts.add("META-INF/*")
        }
    }

}
configurations.all {
    resolutionStrategy {
    }
}

dependencies {
    implementation(libs.appcompat)
    implementation(libs.face.mesh.detection)
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
    compileOnly(files("libs/classes.jar"))
    implementation(libs.face.detection)
    implementation(libs.camera.core)
    implementation(libs.camera.camera2)
    implementation(libs.camera.lifecycle)
    implementation(libs.camera.view)
    implementation(libs.camera.video)
}
afterEvaluate {
    tasks.named("assembleRelease").configure {
        doLast {
            val aarName = "mlkitplugin-release.aar"
            val aarOutput = file("$buildDir/outputs/aar/$aarName")

            // Get export path from Gradle property
            val exportPath = project.findProperty("exportPath")?.toString()
                ?: throw GradleException("Missing -PexportPath=...")

            val finalPath = file("$exportPath/$aarName")

            // Ensure parent directory exists
            finalPath.parentFile.mkdirs()

            // Delete old AAR if exists
            if (finalPath.exists()) {
                finalPath.delete()
            }

            // Copy new AAR
            copy {
                from(aarOutput)
                into(finalPath.parent)
            }

            println("✅ Exported AAR to: ${finalPath.absolutePath}")
        }
    }
}

