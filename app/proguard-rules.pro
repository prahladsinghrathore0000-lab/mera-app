# Safe ProGuard rules for Exam App

# 1. Preserve All Data Models & Entities (Prevents JSON/Database serialization mismatch)
-keep class com.example.model.** { *; }
-keepclassmembers class com.example.model.** { *; }

-keep class com.example.data.** { *; }
-keepclassmembers class com.example.data.** { *; }

-keep class com.example.service.** { *; }
-keepclassmembers class com.example.service.** { *; }

# 2. Room Database Rules
-keep class * extends androidx.room.RoomDatabase
-dontwarn androidx.room.paging.**
-keep class androidx.room.** { *; }
-keepclassmembers class * {
    @androidx.room.Entity *;
    @androidx.room.Dao *;
    @androidx.room.Database *;
    @androidx.room.TypeConverter *;
    @androidx.room.TypeConverters *;
    @androidx.room.PrimaryKey *;
    @androidx.room.ColumnInfo *;
}

# 3. Kotlin & Serialization Rules
-keepattributes *Annotation*,Signature,InnerClasses,EnclosingMethod
-keepattributes SourceFile,LineNumberTable
-dontwarn kotlinx.serialization.**
-keepclassmembers class * {
    @kotlinx.serialization.Serializable *;
    @kotlinx.serialization.SerialName *;
}
-keep class kotlinx.serialization.json.** { *; }

# 4. JSON & Network models
-keepclassmembers class * {
    public <init>(org.json.JSONObject);
    public <init>(org.json.JSONArray);
}

# 5. Jetpack Compose & Material Icons Rules
-dontwarn androidx.compose.**
-keep class androidx.compose.material.icons.** { *; }
-keep class androidx.compose.runtime.** { *; }

# 6. Coroutines
-dontwarn kotlinx.coroutines.**
