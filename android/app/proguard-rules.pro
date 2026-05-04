# Add project specific ProGuard rules here.

# Keep all project classes
-keep class com.quickspeech.** { *; }

# Hilt
-keep class dagger.hilt.** { *; }
-keep class javax.inject.** { *; }
-keep class * extends dagger.hilt.android.lifecycle.HiltViewModel
-keep class **_HiltComponents { *; }
-keep class **_GeneratedInjector { *; }
-keep class **_Factory { *; }
-keep class **_Provide*Factory { *; }
-keep class **_LazyClassKeyProvider { *; }

# Room
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *

# Retrofit
-keepattributes Signature
-keepattributes *Annotation*
-keep class retrofit2.** { *; }
-keepclasseswithmembers class * {
    @retrofit2.http.* <methods>;
}

# Gson
-keep class com.google.gson.** { *; }

# Coroutines
-keep class kotlin.coroutines.** { *; }
