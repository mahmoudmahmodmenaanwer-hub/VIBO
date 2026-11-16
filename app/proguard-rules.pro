# Keep Firestore POJO / data classes for serialization & deserialization
-keepclassmembers class com.example.data.model.** {
    *;
}
-keep class com.example.data.model.** { *; }

# Keep Kotlinx Serialization navigation routes
-keepattributes *Annotation*, InnerClasses
-keepclassmembers class kotlinx.serialization.json.** { *; }
-keep,includedescriptorclasses class com.example.ui.navigation.**$$serializer { *; }
-keepclassmembers class com.example.ui.navigation.** {
    *** Companion;
}

# Preserve line numbers for Crash / Play Console stack traces
-keepattributes SourceFile,LineNumberTable
