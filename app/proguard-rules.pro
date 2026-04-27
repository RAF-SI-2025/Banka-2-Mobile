# R8 / ProGuard pravila za Banka 2 Mobile.
# Pravila samo za biblioteke koje se ne ponasaju ispravno sa default R8-om.

# Hilt — generisani Dagger graf zadrzati u celosti
-keep class dagger.hilt.** { *; }
-keep class * extends dagger.hilt.android.lifecycle.HiltViewModel { *; }

# Moshi sa kotlin codegen-om — adapteri se generisu u istom paketu kao DTO
-keep class ** implements com.squareup.moshi.JsonAdapter { *; }
-keep,allowobfuscation @interface com.squareup.moshi.JsonClass
-keep,allowobfuscation @com.squareup.moshi.JsonClass class **
-keepclassmembers class **JsonAdapter { *; }

# Retrofit — sacuvaj generisku informaciju o servis interfejsima
-keepattributes Signature, InnerClasses, EnclosingMethod
-keep,allowobfuscation,allowshrinking interface retrofit2.Call
-keep,allowobfuscation,allowshrinking class retrofit2.Response

# Kotlinx serialization — generisani serializeri
-keepattributes RuntimeVisibleAnnotations
-keepclassmembers class **$$serializer { *; }

# Compose Navigation type-safe rute (Routes.* @Serializable klase)
-keep,allowshrinking class rs.raf.banka2.mobile.core.ui.navigation.Routes** { *; }

# Sacuvaj liniju u stack trace-u za laksu debugging produkcije
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# Timber — uklanja log pozive u release build-u
-assumenosideeffects class timber.log.Timber {
    public static *** v(...);
    public static *** d(...);
}
