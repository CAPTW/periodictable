# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.kts.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html
#
# This file is referenced by app/build.gradle.kts (release buildType).
# Minification is currently disabled (isMinifyEnabled = false), so these
# rules are inert today, but the file must exist for assembleRelease /
# scripts/verify.* to succeed, and is the place to add keep-rules if/when
# R8/minification is enabled for a Play-ready build.

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Uncomment this to preserve the line number information for
# debugging stack traces.
#-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefilename SourceFile
