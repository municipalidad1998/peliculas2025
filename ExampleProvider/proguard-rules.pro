# CloudStream Extension ProGuard Rules
# Keep CloudStream API classes
-keep class com.lagradost.cloudstream3.** { *; }
-keep class com.lagradost.cloudstream3.utils.** { *; }
-keep class com.lagradost.cloudstream3.extractors.** { *; }

# Keep extension provider classes
-keep class com.cloudstreamext.base.** { *; }
-keep class com.cloudstreamext.util.** { *; }
-keep class com.cloudstreamext.example.** { *; }

# Keep data classes used in JSON parsing
-keep class com.cloudstreamext.example.** { *; }

# OkHttp
-dontwarn okhttp3.**
-dontwarn okio.**
-keep class okhttp3.** { *; }

# Jsoup
-keep class org.jsoup.** { *; }

# Gson
-keep class com.google.gson.** { *; }
-keepattributes Signature
-keepattributes *Annotation*
-keep class * implements com.google.gson.TypeAdapterFactory
-keep class * implements com.google.gson.JsonSerializer
-keep class * implements com.google.gson.JsonDeserializer
