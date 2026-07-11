# EstasBienVE ProGuard Rules
-keepattributes Signature
-keepattributes *Annotation*

# Moshi
-keep class com.direccionve.estasbien.data.model.** { *; }
-keepclassmembers class com.direccionve.estasbien.data.model.** { *; }

# Retrofit
-dontwarn retrofit2.**
-keep class retrofit2.** { *; }

# OkHttp
-dontwarn okhttp3.**
-dontwarn okio.**
