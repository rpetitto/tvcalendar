# Keep Gson model classes (Calendar API responses) and their field names.
-keep class com.rpetitto.tvcalendar.data.remote.** { *; }

# Retrofit / OkHttp / Gson standard rules.
-keepattributes Signature
-keepattributes *Annotation*
-dontwarn okhttp3.**
-dontwarn retrofit2.**
-dontwarn javax.annotation.**
