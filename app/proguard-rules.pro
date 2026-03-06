# ============================================================
# 耗材屋 ProGuard/R8 规则
# ============================================================

# --- Kotlin Serialization ---
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt

-keepclassmembers class kotlinx.serialization.json.** {
    *** Companion;
}
-keepclasseswithmembers class kotlinx.serialization.json.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# Keep @Serializable classes and their generated serializers
-keep,includedescriptorclasses class com.hv.cabinet.data.api.**$$serializer { *; }
-keepclassmembers class com.hv.cabinet.data.api.** {
    *** Companion;
}
-keepclasseswithmembers class com.hv.cabinet.data.api.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# --- Retrofit ---
-dontwarn retrofit2.**
-keep class retrofit2.** { *; }
-keepattributes Signature
-keepattributes Exceptions
-keepclasseswithmembers class * {
    @retrofit2.http.* <methods>;
}

# --- OkHttp ---
-dontwarn okhttp3.**
-dontwarn okio.**
-keep class okhttp3.** { *; }
-keep interface okhttp3.** { *; }

# --- Hilt / Dagger ---
-dontwarn dagger.**
-keep class dagger.** { *; }
-keep class javax.inject.** { *; }
-keep class * extends dagger.hilt.android.internal.managers.ViewComponentManager$FragmentContextWrapper { *; }

# --- Paho MQTT ---
-dontwarn org.eclipse.paho.**
-keep class org.eclipse.paho.** { *; }
-keep interface org.eclipse.paho.** { *; }

# --- Kotlin Coroutines ---
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keepclassmembers class kotlinx.coroutines.** {
    volatile <fields>;
}

# --- Compose ---
# Compose is handled by AGP automatically, but keep @Immutable/@Stable annotations
-keep class androidx.compose.runtime.Immutable
-keep class androidx.compose.runtime.Stable

# --- General ---
-keepclassmembers class * extends java.lang.Enum {
    <fields>;
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# Keep CabinetApi interface (Retrofit)
-keep interface com.hv.cabinet.data.api.CabinetApi { *; }

# Keep data classes used in API responses
-keep class com.hv.cabinet.data.api.** { *; }

# Keep custom serializers
-keep class com.hv.cabinet.core.IntOrStringSerializer { *; }
-keep class com.hv.cabinet.core.StringOrNumberSerializer { *; }
