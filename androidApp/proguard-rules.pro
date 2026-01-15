# Add project specific ProGuard rules here.

# Keep source file names and line numbers for better crash reports
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# Kotlin
-keep class kotlin.** { *; }
-keep class kotlin.Metadata { *; }
-dontwarn kotlin.**
-keepclassmembers class **$WhenMappings {
    <fields>;
}
-keepclassmembers class kotlin.Metadata {
    public <methods>;
}

# Kotlinx Serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
-keepclassmembers class kotlinx.serialization.json.** {
    *** Companion;
}
-keepclasseswithmembers class kotlinx.serialization.json.** {
    kotlinx.serialization.KSerializer serializer(...);
}
-keep,includedescriptorclasses class com.woojin.paymanagement.**$$serializer { *; }
-keepclassmembers class com.woojin.paymanagement.** {
    *** Companion;
}
-keepclasseswithmembers class com.woojin.paymanagement.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# Compose
-keep class androidx.compose.runtime.** { *; }
-keep class androidx.compose.ui.** { *; }
-dontwarn androidx.compose.**

# Koin
-keep class org.koin.** { *; }
-keep class org.koin.core.** { *; }
-keepclassmembers class * {
    @org.koin.core.annotation.* <methods>;
}

# SQLDelight
-keep class app.cash.sqldelight.** { *; }
-keep class com.woojin.paymanagement.db.** { *; }

# Google Play Billing
-keep class com.android.billingclient.** { *; }
-keepclassmembers class com.android.billingclient.** {
    *;
}

# AdMob
-keep class com.google.android.gms.ads.** { *; }
-keep class com.google.ads.** { *; }
-dontwarn com.google.android.gms.ads.**

# Keep all model/data classes
-keep class com.woojin.paymanagement.domain.** { *; }
-keep class com.woojin.paymanagement.data.** { *; }

# For native methods
-keepclasseswithmembernames class * {
    native <methods>;
}

# Keep custom view classes
-keepclasseswithmembers class * {
    public <init>(android.content.Context, android.util.AttributeSet);
}
-keepclasseswithmembers class * {
    public <init>(android.content.Context, android.util.AttributeSet, int);
}

# Enum
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# Parcelable
-keep class * implements android.os.Parcelable {
    public static final android.os.Parcelable$Creator *;
}

# Remove logging in release
-assumenosideeffects class android.util.Log {
    public static *** d(...);
    public static *** v(...);
    public static *** i(...);
}

# Facebook Audience Network - Annotation 관련
-dontwarn com.facebook.infer.annotation.Nullsafe
-dontwarn com.facebook.infer.annotation.Nullsafe$Mode
-keep class com.facebook.ads.** { *; }
-dontwarn com.facebook.ads.**