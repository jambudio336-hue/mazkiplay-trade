# Mazkiplay Trade - R8/ProGuard rules
# Minification is disabled for the release build type, but these rules keep the
# app shippable if you enable it later.

-keepattributes Signature, InnerClasses, EnclosingMethod, *Annotation*

# --- Gson / Retrofit model classes -----------------------------------------
-keep class com.mazkiplay.trade.data.model.** { *; }
-keep class com.mazkiplay.trade.data.api.dto.** { *; }
-keepattributes RuntimeVisibleAnnotations, RuntimeVisibleParameterAnnotations

-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn retrofit2.**
-dontwarn javax.annotation.**
-dontwarn kotlin.**
