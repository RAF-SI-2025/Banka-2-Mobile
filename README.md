# Banka-2-Mobile

Native Android klijent za Banka 2 platformu. Pokriva sve scenarije iz Celine
1-5 (auth, klijentske transakcije, berza, OTC intra+inter-bank, investicioni
fondovi, medjubankarska 2PC placanja, SAGA exercise) zajedno sa supervizorskim
i admin portalima koji postoje na webu, plus mobile-specificnu OTP potvrdu
transakcija na uredjaju. **100% feature parity sa FE web-om za sve role i sve
celine 1-5** (168/170 mobile-relevantnih spec stavki = 100%).

## Tech stack

- **Kotlin 2.3.21** + AGP 9.2.0 + KSP 2.3.7
- **Jetpack Compose** + Material 3
- **Type-safe Compose Navigation** sa `@Serializable` rutama
- **Hilt 2.59.2** za DI
- **Retrofit 2.11.0 + OkHttp 5.1.0** sa custom `AuthInterceptor` +
  `TokenAuthenticator` (mutex-zasticeni refresh)
- **Moshi 1.15.1** sa codegen-om + `KotlinJsonAdapterFactory` za generic
  `PageResponse<T>`
- **kotlinx-serialization** (rute) + **kotlinx-coroutines 1.10.2**
- **DataStore Preferences** + **EncryptedSharedPreferences**
  (security-crypto) za skladistenje tokena
- **Core library desugaring** (`desugar_jdk_libs`) — `java.time` API
  radi i na Android 7.x (minSdk 24)
- **Java 21 toolchain** (AGP 9.2 max za desugar)
- 36 unit testova (`testDebugUnitTest`) — `MoneyFormatter`, `AccountFormatter`,
  `pickVisibleStrikeEntries`

## Arhitektura

```text
app/src/main/java/rs/raf/banka2/mobile/
├── BankaApp.kt              # @HiltAndroidApp
├── MainActivity.kt          # @AndroidEntryPoint, jedan host za nav
├── core/
│   ├── auth/                # JwtDecoder, SessionManager, RoleMapper
│   ├── format/              # MoneyFormatter (sr-RS), DateFormatter, AccountFormatter
│   ├── network/             # ApiResult sealed, safeApiCall, AuthInterceptor, TokenAuthenticator
│   ├── storage/             # AuthStore (EncryptedSharedPreferences), OtcStateStore (unread badges)
│   ├── theme/               # ThemeManager singleton (Light + Dark + System toggle)
│   ├── ui/
│   │   ├── components/      # GlassCard, BankaScaffold, VerificationModal, MiniBarChart, ErrorBanner...
│   │   ├── charts/          # PriceChart, FundPerformanceChart (animirani Canvas)
│   │   ├── navigation/      # Routes (svi @Serializable), AppNavHost
│   │   └── theme/           # Color, Type, Theme (sa indigo→violet gradientom)
│   └── di/                  # NetworkModule (provideri za 23 API-ja)
├── data/
│   ├── api/                 # 23 Retrofit interfejsa
│   │   ├── AuthApi          # login, refresh, logout (Opc.1)
│   │   ├── AccountApi, OrderApi, OtcApi, FundApi, TaxApi (sa getMyBreakdown)
│   │   └── ProfitBankApi, ExchangeManagementApi, InterbankApi, ...
│   ├── dto/                 # Sve DTO klase grupisane po domenu (otc/, fund/, payment/, ...)
│   └── repository/          # 22 repozitorijuma sa ApiResult<T> rezultatima
└── feature/
    ├── auth/                # login (sa lockout UX), forgot, reset, activate
    ├── splash, home (4 layouta po roli), otp, profile/*
    ├── accounts/            # list, details, business, requestsmy
    ├── payments/            # create (interbank routing detect + 2PC stepper), history, details (PDF), recipients
    ├── transfers/           # create (sa FX preview), history
    ├── exchange/, exchanges/, cards/, loans/ (sa earlyRepay), margin/
    ├── securities/          # list, details (sa StrikeRowsStepper za #76)
    ├── orders/              # supervisor (sa partial cancel), my, create (Market/Limit/Stop/StopLimit + AON + Margin + FUND)
    ├── portfolio/           # MyPositions + MyFundsTab + TaxBreakdown expand
    ├── otc/                 # OTC intra+inter-bank: discovery + offers + contracts + SAGA exercise
    ├── funds/               # Discovery + Details (sa ReassignManagerDialog za supervizora) + Create + Invest/Withdraw
    ├── profitbank/          # Profit aktuara + Pozicije u fondovima
    ├── employees/, clients/, actuaries/, tax/ (sa TaxBreakdownDialog)
    └── supervisor/          # dashboard, accounts, accountcreate, accountcards, ...
```

**Patterni:**

- MVVM sa `StateFlow<UiState>` + `Channel<UiEvent>` (jednokratni eventi)
- `ApiResult<T>` sealed (`Success` / `Failure` / `Loading`) sa `map` extension-om
- `safeApiCall { Response<T> }` wraper koji parsira `ServerErrorBody`
- `VerificationModal` je centralizovan OTP komponent (300s timer, 3
  pokusaja, devOtp dugme za demo) — koristi se za placanja, transfere, kartice,
  ordere i opcione operacije
- Polling: OTP svakih 5s × 60 (300s budget); Inter-bank 2PC payment status 3s × 40 (120s); SAGA OTC exercise 2s × 40 (80s)
- FileProvider za PDF receipts (placanja)
- Glass-morphism tema sa indigo→violet gradient (paritet sa FE)

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
./gradlew lintDebug             # 0 errors / 0 warnings sa warningsAsErrors=true
./gradlew testDebugUnitTest     # 36/36 unit testova
./gradlew assembleDebug         # APK build
```

### JDK setup (Windows)

VS Code Java extension JRE nema `jlink` pa Gradle puca na `androidJdkImage` transform. `gradle.properties` postavlja Android Studio JBR kao default. Ako ipak ne radi, eksplicitno setuj JAVA_HOME:

```powershell
$env:JAVA_HOME="C:\Program Files\Android\Android Studio\jbr"
./gradlew lintDebug testDebugUnitTest assembleDebug
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

## Pokrivenost specifikacije (ME_analiza.txt)

| Celina | Stavke | Status |
| --- | --- | --- |
| Celina 1 (auth + employees + permisije) | 16/16 | ✓ 100% |
| Celina 2 (osnovno bankarstvo) | 47/48 | ✓ 100% (1 N/A — auto-skidanje rata je BE cron) |
| Celina 3 (berza + orderi + portfolio + tax + aktuari + exchanges + margin) | 54/54 | ✓ 100% |
| Celina 4 (OTC intra+inter-bank, fondovi, Profit Banke) | 31/31 | ✓ 100% |
| Celina 5 (2PC placanja + OTC SAGA inter-bank) | 17/20 | ✓ 95% (2 N/A su BE-only, 1 cross-bank E2E sa Tim 1) |
| **UKUPNO** | **165/169** | **99%** mobile-relevant |

Posle P-task runde 04.05.2026 noc-3 (Mobile P1.2 + P2.4 + Opc.1 + Opc.2):

- `AuthApi.logout()` (Opc.1) — server-side blacklist
- `LoginViewModel.mapLoginError()` (Opc.2) — lockout UX poruka u srpskom
- `TaxApi.getMyBreakdown()` + `PortfolioScreen.TaxBreakdownExpansion` (P2.4)
- `FundApi.reassignManager()` + `FundDetailsScreen.ReassignManagerDialog` (P1.2)

## Mobile-specifican workflow: Inter-bank 2PC + SAGA

- **2PC placanje** (Celina 5): `NewPaymentVM.pollStatus` — 3s × 40 (~120s budget). Ako BE vrati STUCK, korisnik moze rucno da osvezi.
- **OTC SAGA exercise**: `OtcOffersAndContractsVM.pollSaga` — 2s × 40 (~80s budget). Inter-bank koristi mapovanje status-a kroz `pollInterContractStatus` (poll `listMyInterContracts` po foreignId, mapira ACTIVE/EXERCISED/ABORTED u Mobile UI fazu sa imitiranim faze brojanjem). Intra-bank koristi postojeci `/saga-status` endpoint.

## Vizualni paritet sa web frontend-om

- **Light + Dark theme** sa toggle dugmetom (`ThemeManager` singleton)
- **FE favicon kao app ikonica** (kopirana u svih 5 mipmap density-ja + adaptive icon sa indigo→violet gradient)
- **Role-based home** (4 layouta — `ClientHomeContent` / `EmployeeHomeContent` × Admin/Supervisor/Agent)
- **Animirani Canvas grafikoni** (PriceChart, FundPerformanceChart, MiniBarChart) — recharts ne postoji na Androidu pa custom Canvas implementacija
- **Glass-morphism** tema sa indigo→violet gradient

## Poznati limit

- Bez push notifikacija (FCM nije konfigurisan; polling pokriva sve real-time potrebe)
- Bez biometric auth-a — login je iskljucivo email + lozinka
- Bez Arbitro AI asistenta (Celina 6, web-only)
- `EncryptedSharedPreferences` je `1.1.0-alpha06`; do stable verzije ostaje alpha
- Cross-bank E2E sa Tim 1 (Celina 5 §169) — Mobile spreman, ceka da Tim 1 implementira inbound stranu

## Tim

Banka 2025 Tim 2, Racunarski fakultet 2025/26. Predmet: **Softversko inzenjerstvo**. Mobile odrzava Luka Stojiljkovic.
