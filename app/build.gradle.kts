import com.android.build.api.dsl.ApplicationExtension
import org.gradle.kotlin.dsl.configure
import java.util.Properties
import java.io.FileInputStream

plugins {
    alias(libs.plugins.android.application)
}

configure<ApplicationExtension> {
    namespace = "com.zaitxcode.linkifyall"
    compileSdk = libs.versions.compileSdk.get().toInt()

    defaultConfig {
        applicationId = "com.zaitxcode.linkifyall"
        minSdk = libs.versions.minSdk.get().toInt()
        targetSdk = libs.versions.targetSdk.get().toInt()
        versionCode = libs.versions.versionCode.get().toInt()
        versionName = libs.versions.versionName.get()
    }

    // 👇 1. تحميل ملف إعدادات التوقيع
    val keystorePropertiesFile = listOf(
        rootProject.file("keystore.properties"),
        rootProject.file("key.properties")
    ).firstOrNull { it.exists() }
    val keystoreProperties = Properties()
    if (keystorePropertiesFile != null) {
        keystoreProperties.load(FileInputStream(keystorePropertiesFile))
    }

    // 👇 2. إعداد التوقيع (Signing Config)
    signingConfigs {
        create("release") {
            // Support both camelCase and snake_case keys so local files stay flexible
            val alias = (keystoreProperties["keyAlias"] ?: keystoreProperties["key_alias"]) as String?
            val keyPass = (keystoreProperties["keyPassword"] ?: keystoreProperties["key_password"]) as String?
            val storePath = (keystoreProperties["storeFile"] ?: keystoreProperties["store_file"]) as String?
            val storePass = (keystoreProperties["storePassword"] ?: keystoreProperties["store_password"]) as String?

            keyAlias = alias
            keyPassword = keyPass
            storeFile = storePath?.let { file(it) }
            storePassword = storePass
        }
    }

    buildFeatures {
        viewBinding = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }

    buildTypes {
        getByName("release") {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )

            // استخدام التوقيع إذا كان ملف المفتاح موجوداً محلياً، حتى يمكن بناء المشروع على GitHub بدون المفتاح
            val releaseSigning = signingConfigs.getByName("release")
            if (releaseSigning.storeFile?.exists() == true) {
                signingConfig = releaseSigning
            }
        }
    }
}

dependencies {
    compileOnly(fileTree(mapOf(
        "dir" to "libs",
        "include" to listOf("*.jar")
    )))
    compileOnly(libs.xposed.api)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.swiperefreshlayout)
    implementation(libs.androidhelper)
}
