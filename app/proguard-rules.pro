# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Uncomment this to preserve the line number information for
# debugging stack traces.
#-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile


# --- Comprehensive rules for libraries that use reflection ---

# Keep Ktor, Supabase, and their dependencies (SLF4J, Kotlinx Serialization, Coroutines) from being removed by R8.
# These libraries use reflection, which can confuse the code shrinker.

-keep class org.slf4j.** { *; }
-dontwarn org.slf4j.**

-keepattributes *Annotation*,Signature
-keepclassmembers class ** {
    @kotlinx.serialization.Serializable <fields>;
    @kotlinx.serialization.Transient <fields>;
}
-keep class **$$*Serializer { *; }
-keep class * implements kotlinx.serialization.KSerializer { *; }
-keep class kotlinx.serialization.internal.** { *; }
-dontwarn kotlinx.serialization.**

-keepclassmembers class kotlin.coroutines.jvm.internal.BaseContinuationImpl {
    private java.lang.Object[] getStackTrace();
}

-keep class io.ktor.client.engine.android.** { *; }
-dontwarn io.ktor.**

# OkHttp3 (via Glide okhttp3-integration) - conscrypt is optional TLS provider
-dontwarn org.conscrypt.Conscrypt
-dontwarn org.conscrypt.OpenSSLProvider
-dontwarn org.bouncycastle.**
-dontwarn org.openjsse.**
