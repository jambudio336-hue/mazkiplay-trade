# Mazkiplay Trade

**Nusantara Forex \u2014 Trading cerdas, analisa otomatis**

Aplikasi Android native untuk analisa dan eksekusi trading forex: harga real-time,
kalender ekonomi, analisa otomatis (teknikal + fundamental), manajemen risiko,
copy trade, dan notifikasi. Ditulis dengan **Kotlin + Jetpack Compose (Material 3)**.

> By.mazkiplayTrade

---

## \u2b07\ufe0f Unduh APK Release

| Versi | Berkas | Ukuran | Unduh langsung |
|-------|--------|--------|----------------|
| **v1.5.0** | `mazkiplay-trade-v1.5.0.apk` | ~17 MB | **(lihat halaman rilis v1.5.0 setelah tag dipublikasikan)** |
| v1.0.0 (sebelumnya) | `mazkiplay-trade-v1.0.0.apk` | 13,5 MB | **[\u2b07\ufe0f Unduh APK](https://github.com/jambudio336-hue/mazkiplay-trade/releases/download/v1.0.0/mazkiplay-trade-v1.0.0.apk)** |

- Halaman rilis sebelumnya: **[Releases v1.1.0](https://github.com/jambudio336-hue/mazkiplay-trade/releases/tag/v1.1.0)**
- Rilis sebelumnya: **[Releases v1.0.0](https://github.com/jambudio336-hue/mazkiplay-trade/releases/tag/v1.0.0)**
- Semua rilis: **[github.com/jambudio336-hue/mazkiplay-trade/releases](https://github.com/jambudio336-hue/mazkiplay-trade/releases)**
- APK ditandatangani dengan `CN=Mazkiplay Trade, OU=Mobile, O=Nusantara Forex, C=ID` (RSA 2048)
- SHA-256 APK v1.0.0 (versi sebelumnya): `d415ef271494d513f23f3d974c3b311ae99578929975ebbf9349db2b1b4535b8`

APK juga diunggah sebagai *workflow artifact* pada setiap build
(**Actions \u2192 Android Release \u2192 run terbaru \u2192 Artifacts**).

---

## \u2728 Fitur

### Data Real-Time TradingView (baru di v1.1.0)
- **Sumber utama data pasar kini TradingView**, bukan lagi feed sekunder:
  - **Advanced Real-Time Chart** resmi (WebView, JavaScript + DOM storage aktif) \u2014 chart
    **live tick-by-tick**, ganti simbol, ganti timeframe (M1/M5/M15/H1/H4/D1/W), indikator,
    dan tombol Analisa Otomatis langsung dari chart.
  - **Ticker tape** live (harga bergerak terus) dan **Market Overview** widget di dashboard.
  - **Technical Analysis gauge** resmi \u2014 rekomendasi beli / netral / jual.
  - **Economic Calendar widget** resmi + **News Timeline widget** resmi.
  - **Endpoint publik TradingView** (scanner) untuk angka numerik di dalam app \u2014 watchlist,
    kalkulator, dan TP/SL memakai harga yang di-poll **setiap 10 detik**.
  - **Indikator status koneksi**: `LIVE` / `TERTUNDA` / `OFFLINE` + waktu update terakhir,
    dengan snapshot terakhir dipertahankan saat jaringan gagal.
- **Layar Live Market** (tujuh panel): **Chart**, **Screener** (filter tren/RSI/volatilitas/sinyal),
  **Kekuatan** (currency strength meter + heatmap), **Kalender** (hitung mundur rilis),
  **Berita** (news-flow TradingView), **Crypto** (CoinGecko public top-100), dan **Sinyal** (gabungan teknikal + fundamental).
- **Panel sinyal gabungan**: skor teknikal TradingView (65%) + skor fundamental otomatis (35%)
  dari agenda berdampak tinggi pada mata uang pair \u2192 bias, Entry/SL/TP, R:R, confidence, alasan.
- **Alert harga** otomatis saat harga melewati ambang persentase (default 1%).
- **Market Pulse** dari quote live: breadth naik/turun/datar, rata-rata perubahan sesi, top gainer/top loser, dan jumlah ticker yang belum tersedia.

### Tampilan & Navigasi
- **Splash screen** Nusantara Forex dengan ilustrasi, **sapaan acak yang berganti setiap app dibuka**, dan tanda tangan `By.mazkiplayTrade`.
- **Dashboard** padat fitur: saldo, floating P/L, skor likuiditas, watchlist live, status sesi, agenda ekonomi, aksi cepat, sinyal terakhir, dan headline berita.
- Navigasi **bottom bar** (Dashboard, Pasar, Berita, Analisa, Order) + **drawer** untuk 9 halaman lainnya.
- **Multi-bahasa ID/EN**, **tema dark/light/system**, pengaturan **kecerahan layar** dan **format waktu & tanggal**.

### Data Pasar Real-Time
- Katalog live berisi **71 instrumen publik**: forex major/cross, IDX/BEI (IHSG, IDX30, LQ45 dan saham pilihan), saham US/global, crypto populer, gold/silver, komoditas, futures, index global, DXY, VIX, dan US Treasury yield. Panel Crypto menambahkan **hingga 100 coin teratas CoinGecko** secara dinamis.
- **Chart candlestick** dengan overlay **EMA 20/50/200, Fibonacci retracement, Fair Value Gap, zona Supply & Demand, Support/Resistance**.
- Auto-refresh berkala (TradingView sekitar 10 detik untuk feed live, Yahoo fallback 60 detik saat aktif, dan 15 menit di latar belakang lewat WorkManager).
- **Kalender ekonomi** dari feed publik (agenda fundamental, impact High/Medium/Low, actual vs forecast).
- **Berita real-time** dari beberapa feed RSS/TradingView dengan filter kategori, penanda **BARU**, dan notifikasi.
- Notifikasi event ekonomi berdampak tinggi dikirim ketika rilis memasuki jendela sekitar 20 menit, dengan waktu, impact, forecast, previous, actual, dan deduplikasi agar tidak spam.

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
- **Copy Trade**: katalog publik/edukasi dengan portofolio dan statistik tersimpan; diberi label **KATALOG / TIDAK_REALTIME**. Open position/SL/TP lintas platform tidak diklaim tersedia tanpa provider resmi.
- **Journal Trading**: catatan lokal untuk setup, alasan entry, evaluasi, mood, dan waktu; tidak menghapus riwayat posisi lama.
- **Video News Online**: feed YouTube RSS publik yang berganti otomatis saat video baru tersedia, diputar/stopped di dalam APK melalui embed resmi YouTube.
- **Alarm Entry**: alarm pada jam tertentu atau jeda cepat, opsi ulang harian, dijadwalkan lewat `AlarmManager` (exact alarm).
- **Sesi Trading**: Sydney, Tokyo, London, New York dengan status buka/tutup real-time, progress, dan **skor likuiditas**.
- **Watchlist** yang dapat dikelola, **notifikasi** berita baru / perubahan harga / alarm entry, dan **auto-refresh latar belakang**.

---

## \ud83e\uddf1 Struktur Folder

```
mazkiplay-trade/
\u251c\u2500\u2500 .github/workflows/android-release.yml   # CI: build APK + upload release
\u251c\u2500\u2500 app/
\u2502   \u251c\u2500\u2500 build.gradle.kts                    # Konfigurasi modul + signing release
\u2502   \u251c\u2500\u2500 proguard-rules.pro
\u2502   \u2514\u2500\u2500 src/main/
\u2502       \u251c\u2500\u2500 AndroidManifest.xml
\u2502       \u251c\u2500\u2500 assets/                         # Sumber vektor ikon & splash
\u2502       \u251c\u2500\u2500 java/com/mazkiplay/trade/
\u2502       \u2502   \u251c\u2500\u2500 MainActivity.kt             # Activity tunggal, brightness & permission
\u2502       \u2502   \u251c\u2500\u2500 MazkiplayApp.kt             # Application + service locator
\u2502       \u2502   \u251c\u2500\u2500 data/
\u2502       \u2502   \u2502   \u251c\u2500\u2500 api/                    # MarketApi, MarketDataSource, RSS, DTO, Network
\u2502       \u2502   \u2502   \u251c\u2500\u2500 local/                  # Room: entities, DAO, database
\u2502       \u2502   \u2502   \u251c\u2500\u2500 model/                  # Instrument, Candle, Quote, Analysis, Trade
\u2502       \u2502   \u2502   \u251c\u2500\u2500 tradingview/            # TvTickers, TvModels, TvDataSource, TvAnalyzer, TvRepository
\u2502       \u2502   \u2502   \u2514\u2500\u2500 repository/             # Market, News, Trade, CopyTrade, Settings
\u2502       \u2502   \u251c\u2500\u2500 domain/
\u2502       \u2502   \u2502   \u251c\u2500\u2500 analysis/               # Indicators, Technical, Fundamental, AutoAnalyzer
\u2502       \u2502   \u2502   \u251c\u2500\u2500 session/                # SessionManager (Sydney-New York)
\u2502       \u2502   \u2502   \u2514\u2500\u2500 trade/                  # PositionCalculator, RiskCalculator
\u2502       \u2502   \u251c\u2500\u2500 service/                    # Notifikasi, alarm, worker, boot receiver
\u2502       \u2502   \u251c\u2500\u2500 ui/
\u2502       \u2502   \u2502   \u251c\u2500\u2500 components/             # PriceChart, NewsCard, OrderBook, Profile, Live...
\u2502       \u2502   \u2502   \u251c\u2500\u2500 screens/                # 15 layar (Dashboard, Pasar, Live Market, ...)
\u2502       \u2502   \u2502   \u251c\u2500\u2500 theme/                  # Color, Type, Theme
\u2502       \u2502   \u2502   \u251c\u2500\u2500 viewmodel/              # 7 ViewModel
\u2502       \u2502   \u2502   \u251c\u2500\u2500 widgets/                # Widget resmi TradingView (WebView)
\u2502       \u2502   \u2502   \u251c\u2500\u2500 AppState.kt
\u2502       \u2502   \u2502   \u2514\u2500\u2500 MazkiplayNavHost.kt     # Graf navigasi + bottom nav + drawer
\u2502       \u2502   \u2514\u2500\u2500 util/                       # Formatters, Greetings, LocaleStrings, Constants
\u2502       \u2514\u2500\u2500 res/
\u2502           \u251c\u2500\u2500 drawable/                   # ic_launcher_background
\u2502           \u251c\u2500\u2500 drawable-nodpi/             # diisi saat build dari SVG vektor
\u2502           \u251c\u2500\u2500 mipmap-*/                   # ikon launcher semua ukuran
\u2502           \u251c\u2500\u2500 mipmap-anydpi-v26/          # adaptive icon
\u2502           \u251c\u2500\u2500 values/                     # strings, colors, themes
\u2502           \u2514\u2500\u2500 xml/                        # backup, data extraction, file provider
\u251c\u2500\u2500 tools/generate-icons.sh                 # Rasterisasi ikon & splash dari vektor
\u251c\u2500\u2500 gradle/wrapper/
\u251c\u2500\u2500 CHANGELOG.md
\u251c\u2500\u2500 LICENSE
\u2514\u2500\u2500 README.md
```

---

## \ud83d\udee0\ufe0f Cara Build

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
3. Menyiapkan keystore \u2014 dari secret `KEYSTORE_BASE64` bila tersedia, jika tidak dibuat otomatis untuk build demo.
4. `gradle :app:assembleDebug` (sanity check) lalu `gradle :app:assembleRelease`.
5. Mengunggah APK sebagai **workflow artifact**.
6. Pada tag, **melampirkan APK ke GitHub Release** sehingga bisa diunduh langsung.

Membuat rilis baru:
```bash
git tag v1.1.0
git push origin v1.1.0
```

Untuk build release bertanda tangan produksi, tambahkan repository secret **`KEYSTORE_BASE64`** berisi keystore Anda dalam base64 (`base64 -w0 release.jks`).

---

## \ud83d\udcf1 Cara Install APK

1. Buka halaman **[Releases](https://github.com/jambudio336-hue/mazkiplay-trade/releases)** dan pilih versi terbaru.
2. Unduh berkas `.apk` pada bagian **Assets**.
3. Di perangkat Android, buka **Pengaturan \u2192 Aplikasi \u2192 Akses khusus \u2192 Instal aplikasi tidak dikenal**, lalu izinkan untuk aplikasi pengelola berkas / peramban yang Anda pakai.
4. Ketuk berkas APK yang sudah diunduh \u2192 **Instal**.
5. Saat pertama dibuka, izinkan **notifikasi** dan (bila diminta) **alarm & pengingat** agar fitur alarm entry berjalan.

**Persyaratan minimum:** Android 7.0 (API 24). Target SDK 35 (Android 15).

---

## \ud83c\udfa8 Ikon Aplikasi

Ikon launcher bertema **Nusantara Forex** (emas + batik, latar gelap) berasal dari satu
sumber vektor, [`app/src/main/assets/nusantara_logo.svg`](app/src/main/assets/nusantara_logo.svg),
yang dirasterisasi oleh `tools/generate-icons.sh` menjadi semua kerapatan
(`mdpi` 48px s.d. `xxxhdpi` 192px), **adaptive icon** (`mipmap-anydpi-v26`),
dan ilustrasi splash [`nusantara_splash.svg`](app/src/main/assets/nusantara_splash.svg).

Pendekatan vektor-ke-bitmap ini menjaga konsistensi semua ukuran sekaligus membuat
repositori bebas berkas biner.

---

## \u26a0\ufe0f Catatan Penting

- Semua harga, kalender, dan berita diambil dari **sumber publik pihak ketiga** (TradingView,
  Yahoo Finance, Forex Factory, RSS) dan dapat ditunda atau tidak tersedia; aplikasi selalu
  mempertahankan data terakhir yang valid.
- **Order book** dan **market profile** dihitung dari sebaran volume candle, bukan dari
depth broker, karena feed retail forex tidak menyediakan order book sesungguhnya.
- Hasil **Analisa Otomatis** dihasilkan mesin dari data publik dan **bukan rekomendasi investasi**.
- Aplikasi tidak mengirim order ke broker; eksekusi di sini adalah pencatatan posisi lokal (jurnal trading).

---

## \ud83d\udcc4 Lisensi

[MIT](LICENSE) \u00a9 2026 Mazkiplay Trade (Nusantara Forex)
