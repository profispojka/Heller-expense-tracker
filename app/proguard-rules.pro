# ProGuard / R8 pravidla pro release (minify + shrinkResources zapnuté).
# Hilt, Compose, Coroutines a Room dodávají vlastní consumer-rules přes svoje knihovny;
# tady doplňujeme jen to, co je specifické pro Heller.

# --- Room: entity/DAO/convertery + DB enumy (ukládané jako .name → valueOf při čtení) ---
# Drží celý data.db balíček, ať R8 nepřejmenuje názvy enum konstant uložené v databázi.
-keep class cz.heller.data.db.** { *; }

# Pojistka pro enumy obecně (values()/valueOf používané Room convertery).
-keepclassmembers enum cz.heller.** {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# --- WorkManager: workery se instancují reflexí přes (Hilt) factory ---
-keep class * extends androidx.work.ListenableWorker { *; }

# --- Ponech zdrojové řádky ve stacktrace (kvůli ladění případných pádů v release) ---
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile
