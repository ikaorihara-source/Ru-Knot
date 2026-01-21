import java.io.FileInputStream
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)

    id("com.google.devtools.ksp")
    id("org.jetbrains.kotlin.plugin.serialization")
}

android {
    namespace = "com.ikaorihara.ruknot"
    compileSdk {
        version = release(36)
    }

    defaultConfig {
        applicationId = "com.ikaorihara.ruknot"
        minSdk = 26
        targetSdk = 36
        versionCode = 6
        versionName = "0.9.2 (Release Candidate)"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // 读取 SHA1 并注入到代码中
        val properties = Properties()
        val localPropsFile = rootProject.file("local.properties")
        if (localPropsFile.exists()) {
            properties.load(FileInputStream(localPropsFile))
        }
        // 如果没读到（比如别人下载了代码），就给个空字符串，表示不校验
        val sha1 = properties.getProperty("app.sha1") ?: ""

        // 这行代码会在编译时生成一个 BuildConfig.APP_SHA1 常量
        buildConfigField("String", "APP_SHA1", "\"$sha1\"")
    }

    // 定义签名配置 (必须写在 buildTypes 前面)
    signingConfigs {
        create("release") {
            // 尝试读取项目根目录下的 local.properties
            val keystorePropertiesFile = rootProject.file("local.properties")
            val keystoreProperties = Properties()

            if (keystorePropertiesFile.exists()) {
                keystoreProperties.load(FileInputStream(keystorePropertiesFile))
            }

            // 从文件中读取值 (如果没读到就用空字符串，防止报错)
            keyAlias = keystoreProperties["key.alias"] as String? ?: ""
            keyPassword = keystoreProperties["key.password"] as String? ?: ""
            storePassword = keystoreProperties["store.password"] as String? ?: ""

            // 读取文件路径并转换
            val storeFilePath = keystoreProperties["store.file"] as String?
            if (storeFilePath != null) {
                storeFile = file(storeFilePath)
            }
        }
    }

    buildTypes {
        release {
//            isMinifyEnabled = false
            isMinifyEnabled = true
            isShrinkResources = true

            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )

            // 引用定义的签名
            signingConfig = signingConfigs.getByName("release")
        }

        debug { }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
}

ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)

    // 导航库 (必须有，否则 Tab 切换不了)
    implementation("androidx.navigation:navigation-compose:2.8.3")

    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("androidx.datastore:datastore-preferences:1.1.1")
    implementation("androidx.core:core-ktx:1.15.0")

    // 网络请求 (Retrofit + OkHttp) - 访问B站也需要它
    implementation("com.squareup.retrofit2:retrofit:2.11.0")
    implementation("com.squareup.retrofit2:converter-gson:2.11.0")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")

    // 数据库 (Room) - 存你的房间列表
    val roomVersion = "2.6.1"
    implementation("androidx.room:room-runtime:$roomVersion")
    implementation("androidx.room:room-ktx:$roomVersion")
    ksp("androidx.room:room-compiler:$roomVersion")

    // 图片加载 (Coil) - 显示主播头像
    implementation("io.coil-kt:coil-compose:2.7.0")
    // Coil 视频解码器 (用于显示视频文件的缩略图)
    implementation("io.coil-kt:coil-video:2.7.0")

    // 视频播放器 (用于动态背景)
    implementation("androidx.media3:media3-exoplayer:1.4.1")
    implementation("androidx.media3:media3-ui:1.4.1")

    // 图标库 - 更多好看图标
    implementation("androidx.compose.material:material-icons-extended:1.7.8")

    // Hilt 核心库
    implementation("com.google.dagger:hilt-android:2.51.1")
    // Hilt 编译器（生成代码用）
    ksp("com.google.dagger:hilt-android-compiler:2.51.1")

    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-core:1.7.3")

    ksp("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.3")
    ksp("org.jetbrains.kotlinx:kotlinx-serialization-core:1.6.3")

    // Reorderable 拖拽排序库
    implementation("sh.calvin.reorderable:reorderable:2.1.1")

    val work_version = "2.9.0"
    implementation("androidx.work:work-runtime-ktx:$work_version")
}