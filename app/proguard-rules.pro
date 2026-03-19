 -keep,allowobfuscation,allowshrinking interface retrofit2.Call
 -keep,allowobfuscation,allowshrinking class retrofit2.Response

 -keep,allowobfuscation,allowshrinking class kotlin.coroutines.Continuation

-keepattributes Signature, *Annotation*, EnclosingMethod, InnerClasses

-keepattributes RuntimeVisibleAnnotations, RuntimeVisibleParameterAnnotations

-keep class br.com.sequor.launcher.models.** { *; }
-keep class br.com.sequor.launcher.data.model.** { *; }

-keepclassmembers class br.com.sequor.launcher.models.** { <fields>; }
-keepclassmembers class br.com.sequor.launcher.data.models.** { <fields>; }

-keep interface br.com.sequor.launcher.data.remote.** { *; }