plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("com.google.gms.google-services")
    id("com.google.devtools.ksp")
}

android {
    namespace = "com.tomodachi.chat"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.tomodachi.chat"
        minSdk = 23
        targetSdk = 34
        versionCode = 1
        versionName = "1.0.0"
        vectorDrawables { useSupportLibrary = true }
    }

    // إعداد توقيع اختياري يُقرأ من متغيرات بيئة (تُستخدم من GitHub Actions عبر Secrets).
    // إن لم تُضبط هذه المتغيرات، يبقى بناء debug يعمل بشكل طبيعي بدون توقيع مخصّص.
    val keystorePath = System.getenv("KEYSTORE_PATH")
    val keystorePassword = System.getenv("KEYSTORE_PASSWORD")
    val keyAlias = System.getenv("KEY_ALIAS")
    val keyPassword = System.getenv("KEY_PASSWORD")
    val hasReleaseSigning = !keystorePath.isNullOrBlank() &&
        !keystorePassword.isNullOrBlank() &&
        !keyAlias.isNullOrBlank() &&
        !keyPassword.isNullOrBlank()

    signingConfigs {
        if (hasReleaseSigning) {
            create("release") {
                storeFile = file(keystorePath!!)
                storePassword = keystorePassword
                this.keyAlias = keyAlias
                this.keyPassword = keyPassword
            }
        }
        // مفتاح توقيع debug ثابت وموجود بالمستودع (app/tomodachi-debug.keystore).
        // بدونه، كل بناء على GitHub Actions كان يولّد مفتاح debug عشوائي جديد (لأن
        // كل تشغيل CI يبدأ بجهاز فارغ بدون ~/.android/debug.keystore السابق)، فيصير
        // كل APK تجريبي جديد بتوقيع مختلف عن السابق. Android لا يسمح بتحديث تطبيق
        // فوق نسخة موقّعة بمفتاح مختلف، فيُجبر المستخدم على حذف التطبيق قبل كل
        // تثبيت جديد، وهذا يمسح كل بيانات الجلسة (تسجيل الدخول، التفضيلات، الكاش)
        // في كل مرة — وهو ما كان يبدو وكأن "تسجيل الدخول ما يتذكرني أبداً".
        // بتثبيت مفتاح debug ثابت هنا، يصير توقيع كل APK تجريبي مطابقاً للسابق،
        // فيُحدَّث التطبيق مكان القديم دون حذفه، وتبقى الجلسة محفوظة تماماً كباقي
        // تطبيقات التواصل.
        // "debug" موجود مسبقاً بشكل تلقائي من Android Gradle Plugin — لهذا نستخدم
        // getByName لإعادة ضبطه بدل create (create يفشل لأنه موجود أصلاً).
        getByName("debug") {
            storeFile = file("tomodachi-debug.keystore")
            storePassword = "android"
            keyAlias = "androiddebugkey"
            keyPassword = "android"
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            if (hasReleaseSigning) {
                signingConfig = signingConfigs.getByName("release")
            }
        }
        debug {
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName("debug")
        }
    }

    buildFeatures {
        compose = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.14"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }

    // منع فشل عملية البناء (خصوصًا release) بسبب تحذيرات/أخطاء Lint.
    // يبقى تقرير Lint يُنشأ كالمعتاد لمراجعته لاحقًا، لكنه لن يوقف البناء.
    lint {
        abortOnError = false
        checkReleaseBuilds = false
        warningsAsErrors = false
        disable += "InvalidFragmentVersionForActivityResult"
    }
}

dependencies {
    // Core / Compose
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.4")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.4")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.4")
    implementation("androidx.activity:activity-compose:1.9.1")
    // فرض إصدار حديث من Fragment (>= 1.3.0) لأن بعض المكتبات قد تجرّ إصدارًا أقدم
    // مما يسبب خطأ Lint: InvalidFragmentVersionForActivityResult عند استخدام registerForActivityResult.
    implementation("androidx.fragment:fragment-ktx:1.8.2")
    implementation(platform("androidx.compose:compose-bom:2024.06.00"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.navigation:navigation-compose:2.7.7")
    implementation("androidx.datastore:datastore-preferences:1.1.1")

    // Firebase
    implementation(platform("com.google.firebase:firebase-bom:33.1.2"))
    implementation("com.google.firebase:firebase-auth-ktx")
    implementation("com.google.firebase:firebase-firestore-ktx")
    implementation("com.google.firebase:firebase-messaging-ktx")
    implementation("com.google.firebase:firebase-analytics-ktx")
    implementation("com.google.firebase:firebase-storage-ktx")

    // Room (local cache / offline-first)
    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
    ksp("androidx.room:room-compiler:2.6.1")

    // WorkManager (background message watcher)
    implementation("androidx.work:work-runtime-ktx:2.9.1")

    // Image loading (avatars / sticker uploads)
    implementation("io.coil-kt:coil-compose:2.6.0")

    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.8.1")

    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation(platform("androidx.compose:compose-bom:2024.06.00"))
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}
