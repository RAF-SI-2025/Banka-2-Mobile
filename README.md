# Banka-2-Mobile

Native Android klijent za Banka 2 platformu. Pokriva scenarije iz Celina 1-5
(auth, klijentske transakcije, berza, OTC intra+inter-bank, investicioni fondovi,
medjubankarska 2PC placanja, SAGA exercise) zajedno sa supervizorskim i admin
portalima koji postoje na webu, plus mobile-specificnu OTP potvrdu transakcija
na uredjaju.

## Tech stack

- **Kotlin 2.3.21** + AGP 9.2.1 + KSP 2.3.7 (`gradle/libs.versions.toml`)
- **Jetpack Compose** (Compose BOM) + Material 3
- **Type-safe Compose Navigation** sa `@Serializable` rutama
- **Hilt 2.59.2** za DI
- **Retrofit 2.11.0 + OkHttp 5.1.0** sa custom `AuthInterceptor` + `TokenAuthenticator` (mutex-zasticeni refresh)
- **Moshi 1.15.1** sa codegen-om + `KotlinJsonAdapterFactory` za generic `PageResponse<T>`
- **kotlinx-serialization** (rute) + **kotlinx-coroutines 1.10.2**
- **DataStore Preferences** + **EncryptedSharedPreferences** (security-crypto) za skladistenje tokena
- **Core library desugaring** (`desugar_jdk_libs`) — `java.time` radi i na Android 7.x (minSdk 24)
- **Java 21 toolchain** (compileSdk/targetSdk 36, minSdk 24)

## Arhitektura

```text
app/src/main/java/rs/raf/banka2/mobile/
├── BankaApp.kt              # @HiltAndroidApp
├── MainActivity.kt          # @AndroidEntryPoint, jedan host za nav
├── core/
│   ├── auth/                # JwtDecoder, SessionManager, RoleMapper
│   ├── format/              # MoneyFormatter (sr-RS), DateFormatter, AccountFormatter
│   ├── network/             # ApiResult sealed, safeApiCall, AuthInterceptor, TokenAuthenticator
│   ├── storage/             # AuthStore (EncryptedSharedPreferences), OtcStateStore
│   ├── theme/               # ThemeManager singleton (Light + Dark + System toggle)
│   ├── ui/                  # components, charts (Canvas), navigation (@Serializable rute)
│   └── di/                  # NetworkModule (provideri za API-je)
├── data/
│   ├── api/                 # Retrofit interfejsi (Auth, Account, Order, Otc, Fund, Tax, ...)
│   ├── dto/                 # DTO klase grupisane po domenu (otc/, fund/, payment/, ...)
│   └── repository/          # repozitorijumi sa ApiResult<T> rezultatima
└── feature/                 # auth, home, accounts, payments, transfers, exchange,
                             # cards, loans, margin, securities, orders, portfolio,
                             # otc, funds, profitbank, employees, clients, actuaries,
                             # tax, supervisor
```

**Patterni:**

- MVVM sa `StateFlow<UiState>` + `Channel<UiEvent>` (jednokratni eventi)
- `ApiResult<T>` sealed (`Success` / `Failure` / `Loading`) sa `map` extension-om
- `safeApiCall { Response<T> }` wraper koji parsira `ServerErrorBody`
- `VerificationModal` centralizovan OTP komponent (300s timer, 3 pokusaja, devOtp dugme za demo)
- Polling: OTP 5s × 60 (300s); inter-bank 2PC payment 3s × 40 (120s); SAGA OTC exercise 2s × 40 (80s)
- FileProvider za PDF receipts (placanja)
- Glass-morphism tema sa indigo→violet gradient (paritet sa FE)

## Pokretanje

### Backend mora biti gore

```bash
cd ../Banka-2-Backend
cp .env.example .env   # pa popuni lozinke
docker compose up -d --build
# sacekaj seed: docker logs banka2_seed → "Seed uspesno ubasen!"
```

### Build APK

```bash
./gradlew assembleDebug
# APK: app/build/outputs/apk/debug/app-debug.apk
```

### Kako mobilna aplikacija nalazi backend

`API_BASE_URL` se postavlja kroz `buildConfigField` u `app/build.gradle.kts`:

- **debug** → `http://10.0.2.2:8080/` (standardni alias za host localhost iz Android emulatora)
- **release** → `https://banka-2.radenkovic.rs/api/` (K8s production deploy)

### Lint + verifikacija

```bash
./gradlew lintDebug             # 0 errors / 0 warnings (warningsAsErrors=true)
./gradlew testDebugUnitTest     # unit testovi (MockK + Turbine + MockWebServer)
./gradlew assembleDebug         # APK build
```

### JDK setup (Windows)

Gradle zahteva JDK sa `jlink` za `androidJdkImage` transform. `gradle.properties`
postavlja Android Studio JBR kao default. Ako ne radi, eksplicitno setuj JAVA_HOME:

```powershell
$env:JAVA_HOME="C:\Program Files\Android\Android Studio\jbr"
./gradlew lintDebug testDebugUnitTest assembleDebug
```

## Test kredencijali

Isti kao backend seed (`Banka-2-Backend/seed.sql`):

- Klijent: `stefan.jovanovic@gmail.com` / `Klijent12345`
- Admin: `marko.petrovic@banka.rs` / `Admin12345`
- Supervizor: `nikola.milenkovic@banka.rs` / `Zaposleni12`
- Agent: `tamara.pavlovic@banka.rs` / `Zaposleni12`

## Mobile-specifican workflow: OTP

Prilikom placanja, transfera, novog ordera ili exercise opcije server vraca HTTP
403 sa `{verified, blocked, message, devOtp}`. `VerificationModal`:

1. Prikazuje 300s countdown
2. "Popuni" dugme upisuje `devOtp` (samo za demo — ne u produkciji)
3. Salje ponovljeni zahtev sa `otpCode` u body-ju
4. Posle 3 pogresna pokusaja server zakljucava sesiju (`blocked: true`) i modal se zatvara

## Inter-bank 2PC + SAGA

- **2PC placanje** (Celina 5): `NewPaymentVM.pollStatus` — 3s × 40 (~120s). Ako BE vrati STUCK, korisnik moze rucno da osvezi.
- **OTC SAGA exercise**: `OtcOffersAndContractsVM.pollSaga` — 2s × 40 (~80s). Intra-bank koristi `/saga-status` endpoint; inter-bank mapira ACTIVE/EXERCISED/ABORTED status u UI fazu.

## Vizualni paritet sa web frontend-om

- **Light + Dark theme** sa toggle dugmetom (`ThemeManager` singleton)
- **Role-based home** (Client + Employee × Admin/Supervisor/Agent layouti)
- **Animirani Canvas grafikoni** (PriceChart, FundPerformanceChart, MiniBarChart) — recharts ne postoji na Androidu pa custom Canvas
- **Glass-morphism** tema sa indigo→violet gradient

## Poznati limit

- Bez push notifikacija (FCM nije konfigurisan; polling pokriva real-time potrebe)
- Bez biometric auth-a — login je email + lozinka
- Bez Arbitro AI asistenta (Celina 6, web-only)
- Cross-bank E2E sa Tim 1 (Celina 5) — Mobile spreman, ceka da Tim 1 implementira inbound stranu

## Tim

Banka 2025 Tim 2, Racunarski fakultet 2025/26. Predmet: **Softversko inzenjerstvo**.
Mobile odrzava Luka Stojiljkovic.
