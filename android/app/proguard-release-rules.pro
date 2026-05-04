# Keep Java 17 StringConcatFactory for R8 compatibility
-dontwarn java.lang.invoke.StringConcatFactory
-dontwarn java.lang.invoke.MethodHandles$Lookup
-dontwarn java.lang.invoke.MethodHandles

# Keep data classes from being stripped
-keep class com.quickspeech.common.db.** { *; }
-keep class com.quickspeech.wubi.data.** { *; }
-keep class com.quickspeech.wubi.engine.** { *; }
-keep class com.quickspeech.input.ai.data.** { *; }

# Keep Room entities
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *

# Keep Hilt generated classes
-keep class dagger.hilt.** { *; }
-keep class javax.inject.** { *; }
-keep class * extends dagger.hilt.android.lifecycle.HiltViewModel

# Keep Compose
-keep class androidx.compose.** { *; }
