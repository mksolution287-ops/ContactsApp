plugins {
    id("com.android.application") version "8.6.0" apply false
    id("org.jetbrains.kotlin.android") version "2.0.21" apply false
    id("com.google.devtools.ksp") version "2.0.21-1.0.25" apply false
    id("com.google.gms.google-services")  version "4.4.2" apply false
    id("com.google.firebase.crashlytics") version "3.0.2" apply false
    id("org.jetbrains.kotlin.plugin.compose") version "2.0.21" apply false
}

tasks.register("clean", Delete::class) {
    delete(rootProject.buildDir)
}

//plugins {
//    id("com.android.application") version "8.3.2" apply false
//    id("org.jetbrains.kotlin.android") version "1.9.22" apply false
//    id("com.google.devtools.ksp") version "1.9.22-1.0.17" apply false
//    id("com.google.gms.google-services") version "4.4.4" apply false
//    id("com.google.firebase.crashlytics") version "3.0.6" apply false
//
//}
//
//tasks.register("clean", Delete::class) {
//    delete(rootProject.buildDir)
//}
