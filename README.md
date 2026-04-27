# Banka-2-Mobile

Native Android klijent za Banka 2 platformu. Pokriva sve scenarije iz Celine
1-5 (auth, klijentske transakcije, berza, OTC, investicioni fondovi,
medjubankarska placanja) zajedno sa supervizorskim i admin portalima koji
postoje na webu, plus mobile-specificnu OTP potvrdu transakcija na uredjaju.

## Tech stack

- **Kotlin 2.0.21** + AGP 8.7.3 + Gradle 8.10.2
- **Jetpack Compose** (BOM 2024.12.01) + Material 3
- **Type-safe Compose Navigation 2.8.5** sa `@Serializable` rutama
- **Hilt 2.51.1** za DI (KSP1, KSP2 ima bug sa generic Moshi codegen-om)
- **Retrofit 2.11.0 + OkHttp 4.12.0** sa custom `AuthInterceptor` +
  `TokenAuthenticator` (mutex-zasticeni refresh)
- **Moshi 1.15.1** sa codegen-om + `KotlinJsonAdapterFactory` za generic
  `PageResponse<T>`
- **kotlinx-serialization 1.7.3** (rute) + **kotlinx-coroutines 1.9.0**
- **DataStore Preferences** + **EncryptedSharedPreferences**
  (security-crypto 1.1.0-alpha06) za skladistenje tokena
- **Core library desugaring** (`desugar_jdk_libs 2.1.4`) — `java.time` API
  radi i na Android 7.x (minSdk 24)

## Arhitektura

```text
app/src/main/java/rs/raf/banka2/mobile/
├── BankaApp.kt              # @HiltAndroidApp
├── MainActivity.kt          # @AndroidEntryPoint, jedan host za nav
├── core/
│   ├── auth/                # JwtDecoder, SessionManager, RoleMapper
│   ├── format/              # MoneyFormatter (sr-RS), DateFormatter, AccountFormatter
│   ├── network/             # ApiResult sealed, safeApiCall, AuthInterceptor, TokenAuthenticator
│   ├── storage/             # AuthStore (EncryptedSharedPreferences)
│   ├── ui/
│   │   ├── components/      # GlassCard, BankaScaffold, VerificationModal, MiniBarChart, ErrorBanner...
│   │   ├── navigation/      # Routes (svi @Serializable), AppNavHost
│   │   └── theme/           # Color, Type, Theme (dark only u v1)
│   └── di/                  # NetworkModule (provide-eri za 23 API-ja)
├── data/
│   ├── api/                 # 23 Retrofit interfejsa (AccountApi, OrderApi, ...)
│   ├── dto/                 # Sve DTO klase grupisane po domenu
│   └── repository/          # 22 repozitorijuma sa ApiResult<T> rezultatima
└── feature/
    ├── auth/                # login, forgot, reset, activate
    ├── splash, home, otp, profile/* (rola-based dashboard)
    ├── accounts/            # list, details, business, requestsmy
    ├── payments/            # create (sa interbank routing detect), history, details (PDF), recipients
    ├── transfers/           # create, history
    ├── exchange/, exchanges/, cards/, loans/, margin/
    ├── securities/, orders/, portfolio/    # Celina 3
    ├── otc/, funds/, profitbank/           # Celina 4
    ├── employees/, clients/, actuaries/, tax/
    └── supervisor/          # dashboard, accounts, accountcreate, accountcards, ...
```

**Patterni:**

- MVVM sa `StateFlow<UiState>` + `Channel<UiEvent>` (jednokratni eventi)
- `ApiResult<T>` sealed (`Success` / `Failure` / `Loading`) sa `map` extension-om
- `safeApiCall { Response<T> }` wraper koji parsira `ServerErrorBody`
- `VerificationModal` je centralizovan OTP komponent (300s timer, 3
  pokusaja, email fallback) — koristi se za placanja, transfere, kartice,
  ordere i opcione operacije
- Polling: OTP svakih 5s; Inter-bank 3s × 40; SAGA exercise 2s × 40

## Pokretanje

### Backend mora biti gore

```bash
cd ../Banka-2-Backend
docker compose down -v && docker compose up -d --build
# sacekaj seed: docker logs banka2_seed → "Seed uspesno ubasen!"
```

### Build APK

```bash
./gradlew assembleDebug
# APK: app/build/outputs/apk/debug/app-debug.apk
```

### Kako mobilna aplikacija nalazi backend

`buildConfigField` u `app/build.gradle.kts` postavlja
`API_BASE_URL = "http://10.0.2.2:8080/"`, sto je standardni alias za
host-ov localhost iz Android emulatora. Za pravan uredjaj se override-uje
preko `local.properties` ili gradle build flavor-a.

### Lint + verifikacija

```bash
./gradlew lintDebug
./gradlew testDebugUnitTest
./gradlew assembleDebug
```

## Test kredencijali

Isti kao backend seed (`Banka-2-Backend/seed.sql`). Za mobile demo:

- Klijent: `stefan.jovanovic@gmail.com` / `Klijent12345`
- Admin: `marko.petrovic@banka.rs` / `Admin12345`
- Supervizor: `nikola.milenkovic@banka.rs` / `Zaposleni12`
- Agent: `tamara.pavlovic@banka.rs` / `Zaposleni12`

## Mobile-specifican workflow: OTP

Prilikom placanja, transfera, novog ordera ili exercise opcije server vraca
HTTP 403 sa `{verified, blocked, message, devOtp}`. Mobile aplikacija
prikazuje `VerificationModal` koji:

1. Prikazuje 300s countdown
2. Daje "Popuni" dugme koje upisuje `devOtp` (samo za demo profesoru — nije
   pristupacan u proizvodnji)
3. Salje ponovljeni zahtev sa `otpCode` u body-ju
4. Posle 3 pogresna pokusaja, server zakljucava sesiju (`blocked: true`) i
   modal se zatvara

## Pokrivenost specifikacije

| Celina | Status |
| --- | --- |
| Celina 1 (auth + employees + permisije) | ✓ kompletna |
| Celina 2 (racuni, placanja, transferi, kartice, krediti, exchange) | ✓ kompletna |
| Celina 3 (berza, ordere, portfolio, opcije, marzni racuni) | ✓ kompletna |
| Celina 4 OTC intra-bank | ✓ kompletna |
| Celina 4 OTC inter-bank (SAGA exercise) | ✓ kompletna |
| Celina 4 Investicioni fondovi (discovery, details, create, invest, withdraw, my-funds) | ✓ kompletna |
| Celina 4 Profit Banke portal | ✓ kompletna |
| Celina 4 Inter-bank placanja | ✓ kompletna |

## Poznati limit

- Bez push notifikacija (FCM nije konfigurisan)
- Bez biometric auth-a — login je iskljucivo email + lozinka
- `EncryptedSharedPreferences` je `1.1.0-alpha06`; do stable verzije ostaje
  alpha — zadrzano jer 1.0.0 ne podrzava M-EncryptionScheme.AES256_GCM
- Tema je trenutno samo dark (web ima light + dark, mobile light dolazi
  posle KT3)
