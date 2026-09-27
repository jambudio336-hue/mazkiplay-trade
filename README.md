# Mazkiplay Trade

**Nusantara Forex — Trading cerdas, analisa otomatis**

Aplikasi Android native untuk analisa dan eksekusi trading forex: harga real-time,
kalender ekonomi, analisa otomatis (teknikal + fundamental), manajemen risiko,
copy trade, dan notifikasi. Ditulis dengan **Kotlin + Jetpack Compose (Material 3)**.

> By.mazkiplayTrade

---

## ⬇️ Unduh APK Release

| Versi | Berkas | Tautan |
|-------|--------|--------|
| v1.0.0 | `mazkiplay-trade-v1.0.0.apk` | **[Unduh dari GitHub Releases](https://github.com/jambudio336-hue/mazkiplay-trade/releases/tag/v1.0.0)** |

Semua rilis tersedia di halaman **[Releases](https://github.com/jambudio336-hue/mazkiplay-trade/releases)**.
APK juga diunggah sebagai *workflow artifact* pada setiap build.

---

## ✨ Fitur

### Tampilan & Navigasi
- **Splash screen** Nusantara Forex dengan ilustrasi, **sapaan acak yang berganti setiap app dibuka**, dan tanda tangan `By.mazkiplayTrade`.
- **Dashboard** padat fitur: saldo, floating P/L, skor likuiditas, watchlist live, status sesi, agenda ekonomi, aksi cepat, sinyal terakhir, dan headline berita.
- Navigasi **bottom bar** (Dashboard, Pasar, Berita, Analisa, Order) + **drawer** untuk 9 halaman lainnya.
- **Multi-bahasa ID/EN**, **tema dark/light/system**, pengaturan **kecerahan layar** dan **format waktu & tanggal**.

### Data Pasar Real-Time
- Harga live untuk **20 instrumen**: XAUUSD, XAGUSD, WTI, EURUSD, GBPUSD, USDJPY, USDCHF, AUDUSD, USDCAD, NZDUSD, EURJPY, GBPJPY, AUDJPY, EURGBP, GBPAUD, EURAUD, BTCUSD, ETHUSD, US30, NAS100.
- **Chart candlestick** dengan overlay **EMA 20/50/200, Fibonacci retracement, Fair Value Gap, zona Supply & Demand, Support/Resistance**.
- Auto-refresh berkala (60 detik saat aplikasi aktif, 30 menit di latar belakang lewat WorkManager).
- **Kalender ekonomi** dari feed publik (agenda fundamental, impact High/Medium/Low, actual vs forecast).
- **Berita real-time** dari beberapa feed RSS dengan filter kategori dan penanda **BARU**.

### Analisa Otomatis
- **Analisa Otomatis** gabungan: mesin teknikal (6 sinyal berbobot) + mesin fundamental (kekuatan mata uang, tekanan kalender, sentimen headline).
- Output panel analisa: **bias**, **level Entry/SL/TP**, **rasio R:R**, **tingkat keyakinan (%)**, dan daftar **alasan**.
- **Analisa Teknikal**: EMA stack, RSI(14), ATR, MACD, Bollinger, market structure (HH/HL), swing high/low, pivot klasik.
- **Analisa Fundamental**: kekuatan 12 mata uang, sentimen berita, agenda high-impact, probabilitas.
- **Analisa Pasar**: **order book** (estimasi likuiditas), **market profile** (POC + value area + bentuk profil), **Fibonacci**, **FVG**, **supply & demand**, **support & resistance**.

### Eksekusi & Manajemen Risiko
- **Buka posisi Buy/Sell** dengan TP & SL otomatis: pilih rasio TP (**1:1, 1:1.5, 1:2, 1:3**) dan jarak SL (%), sistem menghitung **lot, harga TP/SL, nilai pip, margin, dan risk/reward**.
- **Kalkulator Forex**: nilai pip, margin, dan estimasi profit/loss per instrumen dengan pengaturan leverage.
- **Kalkulator Risiko**: titik impas (break-even win rate), ketahanan modal, saran money management.
- **Riwayat posisi**: posisi terbuka dengan floating P/L, riwayat tertutup dengan hasil realisasi dan win rate.
- **Copy Trade**: 10 trader publik dengan portofolio, return bulanan/total, drawdown, win rate, followers, AUM, kurva ekuitas, dan opsi **ikut copy trade**.
- **Alarm Entry**: alarm pada jam tertentu atau jeda cepat, opsi ulang harian, dijadwalkan lewat `AlarmManager` (exact alarm).
- **Sesi Trading**: Sydney, Tokyo, London, New York dengan status buka/tutup real-time, progress, dan **skor likuiditas**.
- **Watchlist** yang dapat dikelola, **notifikasi** berita baru / perubahan harga / alarm entry, dan **auto-refresh latar belakang**.

---

## 🧱 Struktur Folder

```
mazkiplay-trade/
├── .github/workflows/android-release.yml   # CI: build APK + upload release
├── app/
│   ├── build.gradle.kts                    # Konfigurasi modul + signing release
│   ├── proguard-rules.pro
│   └── src/main/
│       ├── AndroidManifest.xml
│       ├── assets/                         # Sumber vektor ikon & splash
│       ├── java/com/mazkiplay/trade/
│       │   ├── MainActivity.kt             # Activity tunggal, brightness & permission
│       │   ├── MazkiplayApp.kt             # Application + service locator
│       │   ├── data/
│       │   │   ├── api/                    # MarketApi, MarketDataSource, RSS, DTO, Network
│       │   │   ├── local/                  # Room: entities, DAO, database
│       │   │   ├── model/                  # Instrument, Candle, Quote, Analysis, Trade
│       │   │   └── repository/             # Market, News, Trade, CopyTrade, Settings
│       │   ├── domain/
│       │   │   ├── analysis/               # Indicators, Technical, Fundamental, AutoAnalyzer
│       │   │   ├── session/                # SessionManager (Sydney-New York)
│       │   │   └── trade/                  # PositionCalculator, RiskCalculator
│       │   ├── service/                    # Notifikasi, alarm, worker, boot receiver
│       │   ├── ui/
│       │   │   ├── components/             # PriceChart, NewsCard, OrderBook, Profile...
│       │   │   ├── screens/                # 14 layar (Dashboard, Pasar, Analisa, ...)
│       │   │   ├── theme/                  # Color, Type, Theme
│       │   │   ├── viewmodel/              # 6 ViewModel
│       │   │   ├── AppState.kt
│       │   │   └── MazkiplayNavHost.kt     # Graf navigasi + bottom nav + drawer
│       │   └── util/                       # Formatters, Greetings, LocaleStrings, Constants
│       └── res/
│           ├── drawable/                   # ic_launcher_background
│           ├── drawable-nodpi/             # diisi saat build dari SVG vektor
│           ├── mipmap-*/                   # ikon launcher semua ukuran
│           ├── mipmap-anydpi-v26/          # adaptive icon
│           ├── values/                     # strings, colors, themes
│           └── xml/                        # backup, data extraction, file provider
├── tools/generate-icons.sh                 # Rasterisasi ikon & splash dari vektor
├── gradle/wrapper/
├── CHANGELOG.md
├── LICENSE
└── README.md
```

---

## 🛠️ Cara Build

### Prasyarat
- **JDK 17**
- **Android SDK** dengan `platforms;android-35` dan `build-tools;35.0.0`
- **Gradle 8.9** (workflow CI mengunduhnya otomatis; untuk lokal, jalankan `gradle` setelah memasang Gradle 8.9 atau membuat wrapper sendiri dengan `gradle wrapper`)

### Build lokal (debug)
```bash
gradle :app:assembleDebug
# hasil: app/build/outputs/apk/debug/app-debug.apk
```

### Build lokal (release, signed)
```bash
mkdir -p keystore
keytool -genkeypair -v \
  -keystore keystore/release.jks \
  -alias mazkiplay -keyalg RSA -keysize 2048 -validity 10950 \
  -storepass mazkiplay123 -keypass mazkiplay123 \
  -dname "CN=Mazkiplay Trade, OU=Mobile, O=Nusantara Forex, L=Jakarta, ST=DKI Jakarta, C=ID"

gradle :app:assembleRelease
# hasil: app/build/outputs/apk/release/app-release.apk
```

Keystore dan kata sandi di atas hanyalah nilai demo/open-source yang juga dipakai CI.
Untuk produksi, gunakan keystore Anda sendiri dan lewatkan kata sandi sebagai properti:
```bash
gradle :app:assembleRelease \
  -PRELEASE_STORE_PASSWORD=... \
  -PRELEASE_KEY_ALIAS=... \
  -PRELEASE_KEY_PASSWORD=...
```

### Build otomatis (GitHub Actions)
Workflow [`.github/workflows/android-release.yml`](.github/workflows/android-release.yml) berjalan pada setiap **push ke `main`** dan setiap **tag `v*`**:

1. Menyiapkan JDK 17, Android SDK, Gradle 8.9, dan Gradle cache.
2. Membangkitkan ikon launcher + splash dari sumber vektor.
3. Menyiapkan keystore — dari secret `KEYSTORE_BASE64` bila tersedia, jika tidak dibuat otomatis untuk build demo.
4. `gradle :app:assembleDebug` (sanity check) lalu `gradle :app:assembleRelease`.
5. Mengunggah APK sebagai **workflow artifact**.
6. Pada tag, **melampirkan APK ke GitHub Release** sehingga bisa diunduh langsung.

Membuat rilis baru:
```bash
git tag v1.0.0
git push origin v1.0.0
```

Untuk build release bertanda tangan produksi, tambahkan repository secret **`KEYSTORE_BASE64`** berisi keystore Anda dalam base64 (`base64 -w0 release.jks`).

---

## 📱 Cara Install APK

1. Buka halaman **[Releases](https://github.com/jambudio336-hue/mazkiplay-trade/releases)** dan pilih versi terbaru.
2. Unduh berkas `.apk` pada bagian **Assets**.
3. Di perangkat Android, buka **Pengaturan → Aplikasi → Akses khusus → Instal aplikasi tidak dikenal**, lalu izinkan untuk aplikasi pengelola berkas / peramban yang Anda pakai.
4. Ketuk berkas APK yang sudah diunduh → **Instal**.
5. Saat pertama dibuka, izinkan **notifikasi** dan (bila diminta) **alarm & pengingat** agar fitur alarm entry berjalan.

**Persyaratan minimum:** Android 7.0 (API 24). Target SDK 35 (Android 15).

---

## 🎨 Ikon Aplikasi

Ikon launcher bertema **Nusantara Forex** (emas + batik, latar gelap) berasal dari satu
sumber vektor, [`app/src/main/assets/nusantara_logo.svg`](app/src/main/assets/nusantara_logo.svg),
yang dirasterisasi oleh `tools/generate-icons.sh` menjadi semua kerapatan
(`mdpi` 48px s.d. `xxxhdpi` 192px), **adaptive icon** (`mipmap-anydpi-v26`),
dan ilustrasi splash [`nusantara_splash.svg`](app/src/main/assets/nusantara_splash.svg).

Pendekatan vektor-ke-bitmap ini menjaga konsistensi semua ukuran sekaligus membuat
repositori bebas berkas biner.

---

## ⚠️ Catatan Penting

- Semua harga, kalender, dan berita diambil dari **sumber publik pihak ketiga** dan dapat
ditunda atau tidak tersedia; aplikasi selalu mempertahankan data terakhir yang valid.
- **Order book** dan **market profile** dihitung dari sebaran volume candle, bukan dari
depth broker, karena feed retail forex tidak menyediakan order book sesungguhnya.
- Hasil **Analisa Otomatis** dihasilkan mesin dari data publik dan **bukan rekomendasi investasi**.
- Aplikasi tidak mengirim order ke broker; eksekusi di sini adalah pencatatan posisi lokal (jurnal trading).

---

## 📄 Lisensi

[MIT](LICENSE) © 2026 Mazkiplay Trade (Nusantara Forex)
