# Regra para o Kotlinx Serialization
# Mantém todas as classes que têm a anotação @Serializable
-keep class **$$serializer { *; }
-keepclassmembers class * {
    @kotlinx.serialization.Serializable <fields>;
}
-keep class * implements kotlinx.serialization.KSerializer {
    <init>(...);
}

# Regra para o Ktor
# Se estiver a usar o plugin de serialização do Ktor, a regra acima já ajuda.
# Ktor também usa Coroutines, mas as regras padrão geralmente cobrem isso.
-dontwarn io.ktor.client.engine.cio.**
-keep class io.ktor.client.engine.cio.** { *; }

# Regra para Coroutines (geralmente já incluída, mas é seguro adicionar)
-keepclassmembers class * extends kotlin.coroutines.jvm.internal.ContinuationImpl {
    <fields>;
    <init>(...);
}
-keep class kotlin.coroutines.jvm.internal.DebugMetadataKt

# Regra para o SQLDelight
# Mantém as classes geradas pelo SQLDelight
-keep class app.cash.sqldelight.** { *; }
-dontwarn app.cash.sqldelight.**

# Regra para o Koin
# Mantém as definições de módulos e as anotações do Koin
-keep class org.koin.** { *; }
-keep class * extends org.koin.core.module.Module
-keep class * implements org.koin.core.module.Module
-keep class * extends org.koin.core.scope.Scope
-keep,allowobfuscation @org.koin.core.annotation.* class *
-keep @org.koin.core.annotation.* class *
-keepclassmembers class * {
    @org.koin.core.annotation.* *;
}
-dontwarn org.koin.core.error.KoinAppAlreadyStartedException

# Regra para Jetpack Compose
# As regras para Compose são geralmente incluídas automaticamente pela dependência,
# mas se tiver problemas, pode adicionar regras específicas.
# Geralmente, a "proguard-android-optimize.txt" já tem o que é preciso.

# Regra para o Ksoup (se estiver a ser removido incorretamente)
# Geralmente não é necessário, mas se tiver problemas:
-keep class com.fleeksoft.ksoup.** { *; }
-dontwarn com.fleeksoft.ksoup.**

# Regras para o Firebase Crashlytics
# Mantém os nomes dos ficheiros e números de linha para relatórios de erro mais úteis.
-keepattributes SourceFile,LineNumberTable
-keep class com.google.firebase.crashlytics.** { *; }
-dontwarn com.google.firebase.crashlytics.**