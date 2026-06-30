# ── Hilt / Dagger ─────────────────────────────────────────────────────────────
-keep class dagger.** { *; }
-keep class javax.inject.** { *; }
-keep class * extends dagger.hilt.android.internal.managers.ViewComponentManager$FragmentContextWrapper { *; }
-keepclassmembers class * { @dagger.hilt.android.internal.lifecycle.HiltViewModelMap <fields>; }
-keepclassmembers class * { @dagger.hilt.android.internal.lifecycle.DefaultViewModelFactories <fields>; }
-keep class * extends androidx.lifecycle.ViewModel { *; }
-keep class * extends androidx.lifecycle.AndroidViewModel { *; }
-keep class * extends dagger.hilt.android.components.ActivityComponent { *; }
-keep class * extends dagger.hilt.android.components.FragmentComponent { *; }
-keep class * extends dagger.hilt.android.components.ViewComponent { *; }
-keep class * extends dagger.hilt.android.components.ServiceComponent { *; }

# Keep Hilt generated classes
-keep class * extends dagger.hilt.internal.GeneratedComponent { *; }
-keep class * extends dagger.hilt.internal.GeneratedComponentManager { *; }
-keep class **Hilt_* { *; }
-keep class **_HiltComponents { *; }
-keep class **_HiltModules { *; }
-keep class * extends dagger.hilt.internal.definecomponent.DefineComponentClasses { *; }

# ── Kotlin ────────────────────────────────────────────────────────────────────
-dontwarn kotlin.**
-keepclassmembers class **$WhenMappings { <fields>; }
-keepnames class kotlinx.** { *; }
-keep class kotlin.Metadata { *; }

# ── Coroutines ────────────────────────────────────────────────────────────────
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keepclassmembers class kotlinx.coroutines.** { volatile <fields>; }

# ── Room ──────────────────────────────────────────────────────────────────────
-keep class * extends androidx.room.RoomDatabase { *; }
-keep @androidx.room.Entity class ** { *; }
-keep @androidx.room.Dao class ** { *; }
-keep class * extends androidx.room.RoomDatabase { <init>(...); }
-keepclassmembers class * { @androidx.room.* <methods>; }
-keep class **._Impl { *; }
-keep class * extends androidx.room.TypeConverter { *; }
-dontwarn androidx.room.paging.**

# ── Navigation Compose ────────────────────────────────────────────────────────
-keep class androidx.navigation.** { *; }
-keepclassmembers class * { @androidx.navigation.NavDestination <fields>; }

# ── Compose ───────────────────────────────────────────────────────────────────
-keepclassmembers class * { @androidx.compose.runtime.Composable <methods>; }
-keep class androidx.compose.** { *; }
-dontwarn androidx.compose.**

# ── Material Icons ────────────────────────────────────────────────────────────
-keep class androidx.compose.material.icons.** { *; }

# ── SQLCipher ─────────────────────────────────────────────────────────────────
-keep class net.zetetic.database.sqlcipher.** { *; }
-dontwarn net.zetetic.database.sqlcipher.**

# ── Data / sealed classes used by the app ────────────────────────────────────
-keep class com.vida.** { *; }
-keep class com.vida.**$* { *; }
