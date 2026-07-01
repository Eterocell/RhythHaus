# Native TagLib bridge classes are called from C++ JNI by their JVM names/signatures.
# R8 can otherwise keep the native method entry points but still optimize the
# Kotlin result/value classes into unusable shapes in release builds, causing
# metadata reads to fail only when minified.
-keepclasseswithmembernames,includedescriptorclasses class * {
    native <methods>;
}

-keep class com.eterocell.rhythhaus.taglib.NativeTagLibBridge { *; }
-keep class com.eterocell.rhythhaus.taglib.NativeTagLibReadResult { *; }
-keep class com.eterocell.rhythhaus.taglib.NativeWriteBridge { *; }
-keep class com.eterocell.rhythhaus.taglib.WriteMeta { *; }
