# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

-dontobfuscate
# -dontpreverify
-allowaccessmodification

-keepattributes SourceFile, LineNumberTable, Exception, *Annotation*, InnerClasses, EnclosingMethod, Signature

-keepclassmembers, allowoptimization enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

-keep class de.binarynoise.pingTui.** {
	public static void main(java.lang.String[]);
}
-keep class de.binarynoise.pingTui.PingConfiguration$LoadedConfiguration { *; }
#-keep class ** extends de.binarynoise.pingTui.PingConfiguration { *; }
#-keep class de.binarynoise.pingTui.PingConfigurationKt { *; }

-keepclassmembers, allowoptimization class * {
    *** INSTANCE;
}

-keep class de.binarynoise.pingTui.PingConfiguration$PingConfigurationSupport {
    *** createFileAndExit();
}

-keep,allowoptimization class org.fusesource.jansi.internal.CLibrary { *; }

-keep class io.github.config4k.TypeReference { *; }
-keep class ** extends io.github.config4k.TypeReference { *; }

-keep, allowoptimization class ** extends kotlin.jvm.internal.ReflectionFactory
-keep, allowoptimization class kotlin.jvm.internal.Reflection {
    private static final kotlin.jvm.internal.ReflectionFactory factory;
}

######################
# kotlin-reflect.pro #
######################

# Keep Metadata annotations so they can be parsed at runtime.
-keep class kotlin.Metadata { *; }

# Keep implementations of service loaded interfaces
# R8 will automatically handle these these in 1.6+
-keep interface kotlin.reflect.jvm.internal.impl.builtins.BuiltInsLoader
-keep class * implements kotlin.reflect.jvm.internal.impl.builtins.BuiltInsLoader { public protected *; }
-keep interface kotlin.reflect.jvm.internal.impl.resolve.ExternalOverridabilityCondition
-keep class * implements kotlin.reflect.jvm.internal.impl.resolve.ExternalOverridabilityCondition { public protected *; }

# Keep generic signatures and annotations at runtime.
# R8 requires InnerClasses and EnclosingMethod if you keepattributes Signature.
-keepattributes InnerClasses,Signature,RuntimeVisible*Annotations,EnclosingMethod

# Don't note on API calls from different JVM versions as they're gated properly at runtime.
-dontnote kotlin.internal.PlatformImplementationsKt

# Don't note on internal APIs, as there is some class relocating that shrinkers may unnecessarily find suspicious.
-dontwarn kotlin.reflect.jvm.internal.**

##################
# coroutines.pro #
##################

# ServiceLoader support
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}

# Most of volatile fields are updated with AFU and should not be mangled
-keepclassmembers class kotlinx.coroutines.** {
    volatile <fields>;
}

# Same story for the standard library's SafeContinuation that also uses AtomicReferenceFieldUpdater
-keepclassmembers class kotlin.coroutines.SafeContinuation {
    volatile <fields>;
}

# These classes are only required by kotlinx.coroutines.debug.AgentPremain, which is only loaded when
# kotlinx-coroutines-core is used as a Java agent, so these are not needed in contexts where ProGuard is used.
-dontwarn java.lang.instrument.ClassFileTransformer
-dontwarn sun.misc.SignalHandler
-dontwarn java.lang.instrument.Instrumentation
-dontwarn sun.misc.Signal

# Only used in `kotlinx.coroutines.internal.ExceptionsConstructor`.
# The case when it is not available is hidden in a `try`-`catch`, as well as a check for Android.
-dontwarn java.lang.ClassValue

# An annotation used for build tooling, won't be directly accessed.
-dontwarn org.codehaus.mojo.animal_sniffer.IgnoreJRERequirement

