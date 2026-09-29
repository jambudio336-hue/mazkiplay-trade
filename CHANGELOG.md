# Changelog

Semua perubahan penting pada **Mazkiplay Trade** didokumentasikan di file ini.
Format mengikuti [Keep a Changelog](https://keepachangelog.com/id/1.1.0/)
dan proyek ini menggunakan [Semantic Versioning](https://semver.org/lang/id/).

## [2.1.0] - 2026-09-29

### Fitur terintegrasi dari spesifikasi pengguna
- **Live Chart expansion**: timeframe M1, M3, M5, M15, M30, H1, H2, H4, H6, H8, H12, D1, W1, dan 1M; interval yang tidak tersedia langsung diagregasi lokal.
- **Market Radar** read-only untuk ticker publik Indodax, status health LIVE/STALE/OFFLINE, ranking volume, range position, bid/ask, serta failover ke snapshot terakhir.
- Adapter public REST Indodax untuk server time, pairs, ticker all/single, depth, trades, dan TradingView-style history.
- **Data pipeline** provider → normalizer → technical/fundamental/news context → signal/risk UI tetap deterministic dan dapat diaudit.
- **AI Trading Lab** diperluas dengan pipeline OHLCV/Trades, pattern matching, Order Manager, dan mode PAPER/MANUAL yang aman.
- **Risk-first workflow** dipertahankan: signal tidak mengeksekusi order, risk/reward dan position sizing menjadi trade-plan untuk konfirmasi pengguna.
- **Donasi QRIS** memakai gambar QRIS resmi yang dikirim pemilik aplikasi; nomor telepon lama dihapus dari source, UI, dan dokumentasi.
- **Launcher identity** baru: ikon beruang bullish dan banteng bearish premium; varian bearish disimpan sebagai asset brand.

### Batas konektor
- Tokocrypto/iTick/BiQuote, broker, MT4/MT5, Lighter DEX, private account, dan AI/API berbayar tidak diaktifkan sebagai order execution tanpa credential, lisensi, dan backend aman.

## [2.2.0] - 2026-09-29

### Market Command Center
- Menambahkan Personal Market Brief: currency strength, gold volatility, crypto regime, active signals, high-impact event count, dan signal pause saat data stale.
- Menambahkan Data Health Center, provider path, failover status, conflict/risk messaging, Opportunity Scanner taxonomy, dan Emergency Risk Lock.
- Menambahkan Personal Trading Profile: Conservative, Balanced, Aggressive, Custom dengan hard limits untuk risk, minimum R:R, dan jumlah posisi.
- Menambahkan What-If Simulator untuk balance, risk, SL distance, R:R, risk amount, lot estimate, dan potential reward tanpa mengirim order.

### Mazkiplay Live
- Menambahkan Live Event Center untuk Federal Reserve/FOMC, ECB, dan BOE dengan state `UPCOMING → LIVE → ENDED`.
- Official source links dibuka langsung; aplikasi tidak meng-embed atau mengambil stream yang tidak mengizinkan embedding.
- Menambahkan NEWS MODE, market reaction context, transcript/watchwords placeholder, dan pengetatan risk/signal messaging saat high-impact event.

### Mazkiplay Academy
- Menambahkan sepuluh modul original: Professional Trader, Money Management, Candlestick, Technical, Fundamental, Macro, News, Psychology, Backtesting, Journal, dan Checklist.
- Reader offline di dalam aplikasi dengan versioning, disclaimer edukasi, serta branding M4zk1pLayNusantara.

### Mazkiplay AI
- Menambahkan AI Provider abstraction: local Qwen/Gemma/llama.cpp reference, OpenRouter, OpenAI, Gemini, Anthropic, Groq, Mistral, DeepSeek, dan custom OpenAI-compatible.
- Menambahkan AI Router local-first, Model Manager, provider status, konfigurasi credential, delete credential, dan Android Keystore encrypted vault.
- AI hanya menjelaskan context yang dihitung engine; tidak mengakses private key, tidak menjadi sumber angka kritis, dan tidak mengeksekusi order.

## [2.3.0] - 2026-09-29

### Unified Signal Bot
- Menambahkan **Signal Bot Center** dengan satu laporan keputusan terpadu dari market data, technical, fundamental, macro, news, live event, signal, risk, money management, dan AI explanation.
- Channel outbound resmi yang tersedia: **Telegram Bot API**, **Discord Incoming Webhook**, dan **WhatsApp Business Cloud API**.
- Menambahkan format pesan bilingual-friendly dengan decision, entry, SL, TP1/TP2, risk, R:R, lot, confluence, Risk Guardian, dan explanation.
- Menambahkan event-driven trigger taxonomy: new setup, WAIT, breakout, reversal, TP1/TP2, SL, spread, volatility, breaking/high-impact news, countdown, LIVE NOW, release, post-news reanalysis, central-bank decision, dan macro regime.
- Menambahkan anti-spam: deduplication key per symbol/decision/trigger dan cooldown dua menit; pesan tidak dikirim setiap tick.
- Menambahkan konfigurasi dan preview/test message dari drawer **Signal Bot · Telegram/WA/Discord**.
- Token/API key/webhook disimpan terenkripsi memakai Android Keystore; tidak ada credential developer di source code atau GitHub.

### Safety and provider boundaries
- WhatsApp yang didukung adalah **WhatsApp Business Cloud API**, bukan otomasi akun WhatsApp personal.
- Bot hanya outbound notification dan tidak menerima private key atau mengeksekusi order broker/exchange.
- Pesan sinyal produksi harus berasal dari data aktual dan conflict/risk checks; preview menggunakan status WAIT agar tidak tampak sebagai rekomendasi palsu.

## [2.4.0] - 2026-09-29

### Responsive UX and launch soundtrack
- Menambahkan startup soundtrack milik project ke APK sebagai `startup_theme.mp3`.
- Soundtrack autoplay sekali saat process aplikasi pertama kali dibuka dan berhenti otomatis saat mencapai end-of-file; tidak ada tombol mute, stop, skip, atau delete di dalam UI sesuai brief.
- MediaPlayer dilepas otomatis di completion/error dan meminta audio focus transient agar tidak membebani lifecycle aplikasi.
- Menjaga fitur lama, navigation graph, data repositories, live feeds, bot integrations, Academy, AI Center, risk controls, dan donation flow tetap berada dalam struktur yang sama.
- Menata startup audio di Application lifecycle agar tidak dijalankan berulang saat recomposition Compose.

Catatan platform: audio dapat dihentikan sementara atau diinterupsi oleh Android, panggilan telepon, Bluetooth, atau aplikasi audio lain karena aturan audio focus sistem. Aplikasi tidak dapat dan tidak seharusnya mengunci kontrol sistem tersebut.

## [2.5.0] - 2026-09-29

### Online-only and Bahasa Indonesia
- Menambahkan **Online Required Gate**: navigation, chart, signal, news, calendar, bot, AI, Academy, dan fitur lain tidak dikomposisikan ketika Android belum memiliki koneksi internet tervalidasi.
- Gate menggunakan `NET_CAPABILITY_INTERNET` + `NET_CAPABILITY_VALIDATED`, memantau perubahan jaringan otomatis, dan membuka aplikasi saat koneksi tervalidasi kembali.
- Startup soundtrack baru dimulai setelah gate online lolos, sehingga APK tidak mengaktifkan experience penuh dalam mode offline.
- Mengunci bahasa release ke Bahasa Indonesia sebagai bahasa antarmuka utama dan memigrasikan preferensi bahasa lama ke Indonesia.
- Mengganti launcher artwork menjadi ikon premium bull/bear split-face dengan tema terminal, emas, bullish green, dan bearish red; seluruh density tetap digenerate dari satu SVG saat CI build.

Catatan: online-only berarti aplikasi menolak mode offline pada level UI. Ketersediaan realtime tiap provider tetap bergantung pada endpoint publik, rate limit, dan kesehatan jaringan/provider; status LIVE/STALE/OFFLINE tetap menjadi sumber kebenaran di aplikasi.

## [2.6.0] - 2026-09-30

### Responsive chart and premium UX
- Menambahkan tombol **Buka Landscape** pada chart TradingView Live Market dan chart Analisa.
- Chart memakai orientasi landscape dengan tinggi adaptif dan dapat kembali ke portrait tanpa menutup layar atau kehilangan konteks instrumen/timeframe.
- Mempertahankan indikator otomatis EMA, RSI, MACD, ATR, Fibonacci, supply-demand, FVG, market structure, technical gauge, serta gabungan fundamental dari feed live.
- Menormalkan detail notifikasi kalender ke Bahasa Indonesia: Dampak, Perkiraan, Sebelumnya, dan Aktual.
- Memperbarui launcher vector menjadi ikon bull/bear premium dinamis dan mempertahankan generator semua density pada CI.
- Settings, drawer, bottom navigation, refresh, analyzer, risk, bot, AI, dan Academy tetap dipertahankan; audit callback tidak menemukan tombol kosong.
- Layout chart dan kontrol memakai state responsive Android orientation/configChanges agar tidak terpotong saat rotasi.

Catatan data: chart TradingView widget menampilkan feed yang tersedia dari provider; repository technical quote melakukan refresh berkala dan crypto utama memakai WebSocket publik. Tidak ada provider publik yang dapat menjamin zero-delay untuk seluruh saham, forex, crypto, fundamental, dan berita secara bersamaan.

## [2.7.0] - 2026-09-30

### Twelve Data market-data integration
- Menambahkan Twelve Data sebagai provider REST market-data opsional untuk quote dan candle saham, forex, crypto, serta instrumen yang didukung provider.
- Jika credential Twelve Data tersedia, jalur ini dicoba lebih dulu; Yahoo tetap menjadi fallback defensif ketika provider gagal, rate limit, atau simbol tidak tersedia.
- Kunci API tidak di-hardcode, tidak ditulis ke log, tidak masuk Git, dan tidak dikemas ke APK; pengguna memasukkannya dari AI Center lalu disimpan terenkripsi melalui Android Keystore.
- Menambahkan tombol konfigurasi Twelve Data pada daftar provider AI Center dengan label Bahasa Indonesia.
- Menambahkan parsing null/error, mapping interval, mapping simbol, dan status fallback agar feed tidak membuat aplikasi crash.

Catatan keamanan: API key yang pernah ditempelkan di chat dianggap terekspos. Rotasi/revoke key tersebut di dashboard Twelve Data, lalu masukkan key baru melalui **AI Center → Twelve Data → Konfigurasi**. Release ini sengaja tidak membawa key pengguna di dalam binary.

## [2.7.1] - 2026-09-30

### Credential vault hotfix
- Memperbaiki potensi force close saat menekan **Simpan ke Brankas**.
- Operasi Android Keystore dan penyimpanan credential sekarang berjalan di background thread dan dibungkus error handling aman.
- Jika perangkat menolak Keystore, key kosong, atau storage gagal, aplikasi menampilkan pesan error dan tetap terbuka.
- Setelah menyimpan Twelve Data, aplikasi otomatis menjalankan tes koneksi endpoint harga AAPL dan menampilkan status **terhubung dan merespons** atau alasan kegagalannya.
- Menambahkan tombol **Tes Koneksi**, indikator proses, dan status provider yang diperbarui tanpa menutup AI Center.
- Semua angka signal/confidence adalah skor algoritmik internal, bukan probabilitas kemenangan.

## [2.0.0] - 2026-09-29

### Ditambahkan
- **AI Trading Lab** mengikuti alur video: source → API/WS → OHLCV & Trades → Data Manipulation → Pattern Matching → Order Manager → Lighter DEX.
- Pattern matching deterministik berbasis trend, RSI, dan struktur teknikal dari snapshot OHLCV yang sudah tersedia.
- Status eksekusi **PAPER / MANUAL** dan guard keamanan agar private key/order DEX tidak pernah ditanam di APK.
- Lighter DEX ditampilkan sebagai connector yang belum terhubung; klaim 0-fee/0-gas tidak dianggap fakta tanpa verifikasi SDK/provider resmi.

### Diubah
- Layar Analisa mendapatkan pipeline visual premium tanpa menghapus chart, analisa fundamental, Sniper Entry, dan fitur lama.

## [1.9.0] - 2026-09-29

### Ditambahkan
- Streaming market-data publik **Binance WebSocket** untuk crypto utama: BTC, ETH, BNB, SOL, XRP, DOGE, ADA, AVAX, LINK, DOT, TRX, SHIB, dan PEPE.
- Reconnect otomatis dengan exponential backoff ketika koneksi WebSocket terputus.
- Metadata source dan freshness pada model crypto: `BINANCE_WS` atau `COINGECKO`.
- **Realtime Command Center** pada dashboard untuk membedakan streaming, polling, dan data event-driven.

### Diubah
- Status crypto baru `STREAMING`, bukan lagi menyamakan streaming dengan refresh periodik.
- Kartu UI menggunakan outline brand dan elevation lebih premium untuk tampilan yang lebih profesional.
- Release tetap transparan: saham IDX, data fundamental, macro, news, dan aset berlisensi tidak diklaim tick-by-tick tanpa provider resmi.

## [1.8.0] - 2026-09-29

### Ditambahkan
- **Top Signals** pada desk Live Market: ranking lintas instrumen berdasarkan confidence dan R:R.
- Label aksi, timestamp snapshot, dan status freshness pada panel sinyal.
- Tombol **Analisa ulang sekarang** untuk meminta kalkulasi terbaru.

### Diubah
- Sinyal otomatis dikunci menjadi **MENUNGGU DATA** ketika quote live belum tersedia; tidak ada BUY/SELL palsu dari snapshot kosong.
- Semua fitur v1.7.0 tetap dipertahankan: chart TradingView, teknikal + fundamental, alert, kalender, crypto, risiko, journal, dan navigasi lama.

## [1.7.0] - 2026-09-28

### Ditambahkan
- Background aplikasi adaptif portrait/landscape dengan ilustrasi bullish bull versus bearish bear, dibuat tanpa teks agar tetap aman di belakang UI.
- **Sniper Entry** pada layar Analisa: bias BUY/SELL dari analisis live, entry, SL, TP, confidence, lot, nominal risiko, dan R:R.
- Risk sizing Sniper Entry memakai balance, risk percent, leverage, SL percent, dan TP ratio pengguna melalui `PositionCalculator`.
- Tombol menyimpan pengingat alarm sniper satu menit agar pengguna memeriksa ulang feed sebelum entry.

### Diubah
- Versi aplikasi naik menjadi `1.7.0` dan versionCode menjadi 8.
- Sumber Bloomberg Terminal tidak diklaim sebagai feed publik; integrasi Bloomberg memerlukan lisensi/API resmi dan credential backend. Feed publik yang tersedia tetap diberi label sumber/status.

## [1.6.0] - 2026-09-28

### Ditambahkan
- Katalog Crypto Spot dinamis CoinGecko hingga 250 aset market-cap teratas.
- Katalog Meme Coin dinamis hingga 250 aset dari kategori `meme-token` CoinGecko.
- Refresh listing otomatis agar coin yang baru masuk ranking publik dapat muncul pada refresh berikutnya.
- Filter **Spot**, **Meme Coin**, dan **Semua**.
- Sparkline chart harga 7 hari pada setiap coin jika dikirim oleh sumber publik.

### Diubah
- Versi aplikasi naik menjadi `1.6.0` dan versionCode menjadi 7.
- Batas public/free dan rate-limit ditampilkan transparan; fitur ini bukan feed order book exchange.

## [1.5.0] - 2026-09-28

### Ditambahkan
- Layar **Video News Online** pada drawer aplikasi.
- Feed YouTube RSS publik dari beberapa kanal news, refresh otomatis, deduplikasi video, dan rotasi otomatis ke video terbaru.
- Pemutar embed resmi YouTube di dalam APK dengan tombol **Putar**, **Stop**, dan tautan YouTube.
- Fallback tanpa API key; tidak ada secret YouTube yang ditanam di APK.

### Diubah
- Versi aplikasi naik menjadi `1.5.0` dan versionCode menjadi 6.

## [1.4.0] - 2026-09-28

### Ditambahkan
- Model `MarketPulse` untuk merangkum breadth pasar dari quote live yang benar-benar berhasil diterima.
- Kartu **Market Pulse** responsif di Live Market: jumlah naik/turun/datar, persentase breadth, rata-rata perubahan sesi, top gainer, top loser, dan coverage feed.
- Top mover dapat diketuk untuk langsung berpindah ke simbol terkait di desk live.
- Katalog crypto public CoinGecko top-100 berdasarkan market cap dengan refresh otomatis dan status LIVE/TERTUNDA/OFFLINE.
- Journal trading lokal berbasis Room untuk simbol, setup, catatan analisis, mood, waktu, dan penghapusan catatan.
- Reminder kalender publik berdampak tinggi diperiksa tiap 15 menit di background dengan waktu, impact, forecast, previous, dan actual.
- Analisa otomatis tetap menggabungkan teknikal dan fundamental dari snapshot feed yang tersedia; indikator yang datanya tidak tersedia tidak diisi sintetis.

### Diubah
- Snapshot yang gagal resolve tidak masuk ke perhitungan breadth; jumlahnya ditampilkan sebagai coverage agar kualitas data transparan.
- Versi aplikasi naik menjadi `1.4.0` dan versionCode menjadi 5.
- Statistik copy-trade tidak lagi bergerak secara acak; katalog diberi label publik/non-real-time sampai provider resmi tersedia.

## [1.3.0] - 2026-09-28

### Ditambahkan
- Katalog aset publik diperluas untuk IDX, saham global, forex tambahan, crypto, komoditas, futures, index, DXY, VIX, dan Treasury yield.
- Timeframe mingguan (`W1`) dan bulanan (`1M`) tersedia pada chart/history.
- Pemetaan TradingView untuk aset populer dengan fallback Yahoo chart.
- Notifikasi kalender ekonomi berdampak tinggi dalam jendela 15 menit sebelum rilis, dengan deduplikasi lokal.

### Diubah
- Versi aplikasi naik menjadi `1.3.0` dan versionCode menjadi 4.

## [1.2.0] - 2026-09-28

### Ditambahkan
- Foto portrait yang diberikan pemilik proyek digunakan sebagai tampilan splash awal aplikasi.
- Kartu **Donasi untuk Pengembangan APK** di menu Settings.
- Dialog donasi rinci dengan QRIS, petunjuk scan, dan verifikasi nama penerima,
  tombol salin nomor, nama developer `by.M4zk1pl4y Nusantara`, dan kontak email developer.
- Asset splash photo dan logo DANA disertakan dalam source agar build lokal/CI konsisten.

### Diubah
- Versi aplikasi naik menjadi `1.2.0` dan versionCode menjadi 3.

## [1.1.0] - 2026-09-28

### Ditambahkan
- **Integrasi penuh data real-time TradingView** sebagai sumber utama:
  - **Advanced Real-Time Chart** resmi di dalam WebView \u2014 chart live tick-by-tick per
    instrumen, bisa ganti simbol, ganti timeframe (M1, M5, M15, H1, H4, D1, W), indikator,
    drawing tools, dan tombol Analisa Otomatis langsung dari chart.
  - **Ticker tape** live di dashboard (harga bergerak terus) dan **Market Overview** widget.
  - **Technical Analysis gauge** resmi TradingView \u2014 rekomendasi beli/netral/jual.
  - **Economic Calendar widget** resmi + **News Timeline widget** resmi.
  - **Endpoint publik TradingView** (scanner/quote) untuk angka numerik di dalam app:
    watchlist, kalkulator, dan TP/SL memakai harga scanner yang di-poll **setiap 10 detik**,
    lengkap dengan indikator status koneksi **LIVE / TERTUNDA / OFFLINE + waktu update terakhir**.
  - Penanganan error/offline eksplisit: snapshot terakhir dipertahankan, banner peringatan
    ditampilkan, dan panel pengganti muncul jika widget tidak dapat dimuat.

- **Layar Live Market baru** dengan enam panel dalam satu layar:
  - **Chart** \u2014 chart live + statistik OHLC live + gauge teknikal + aksi Analisa Otomatis.
  - **Screener** \u2014 filter nyata: tren naik/turun, RSI overbought/oversold, volatilitas tinggi,
    sinyal beli/jual; diurutkan menurut pergerakan terbesar.
  - **Kekuatan** \u2014 **Currency Strength Meter + heatmap mata uang**, dihitung dari pergerakan
    persen live seluruh pair (bukan tabel statis).
  - **Kalender** \u2014 widget kalender TradingView + daftar **hitung mundur** ke setiap rilis
    berdampak tinggi.
  - **Berita** \u2014 headline dari news-flow TradingView, ditandai simbol terkait, plus timeline widget.
  - **Sinyal** \u2014 **panel sinyal gabungan**: skor teknikal TradingView (bobot 65%) dipadukan
    dengan skor fundamental otomatis (bobot 35%) yang dihitung dari agenda berdampak tinggi
    pada mata uang di dalam pair, menghasilkan bias, level Entry/SL/TP, R:R, confidence, dan alasan.

- **Notifikasi alert harga** berbasis ambang persentase (default 1%), hanya terpicu saat
  harga **melewati** ambang \u2014 bukan berulang setiap poll.
- **Preferensi baru**: timeframe default dan ambang alert harga.
- **Deteksi ticker gagal resolve**: jumlah ticker yang tidak kembali dari scanner dipantau
  agar pemetaan simbol yang bermasalah cepat terlihat.

### Diubah
- Repositori TradingView terintegrasi ke service locator aplikasi; feed Yahoo yang lama tetap
  dipertahankan sebagai jalur cadangan (fallback) ketika TradingView tidak dapat dijangkau.
- Versi naik dari `1.0.0` (versionCode 1) ke **`1.1.0`** (versionCode 2).

### Catatan teknis
- Semua ticker TradingView dipetakan pada satu tabel terverifikasi
  (`TvTickers.kt`): `OANDA:XAUUSD`, `TVC:SILVER`, `NYMEX:CL1!`, `BITSTAMP:BTCUSD`,
  `OANDA:US30USD`, `NASDAQ:NDX`, `TVC:DXY`, dan seluruh major/cross.
- WebView dikonfigurasi dengan JavaScript + DOM storage aktif dan base URL https
  agar widget live berjalan stabil di Android.

## [1.0.0] - 2026-09-27

### Ditambahkan
- **Splash screen Nusantara Forex**: ilustrasi bertema Nusantara, sapaan acak yang
  berganti setiap aplikasi dibuka, dan tanda tangan `By.mazkiplayTrade`.
- **Dashboard** dengan kartu-kartu ringkasan pasar, sesi trading aktif,
  berita terbaru, dan navigasi cepat ke seluruh fitur.
- **Data real-time**: harga forex (XAUUSD, EURUSD, GBPUSD, USDJPY, dll) dan
  kalender ekonomi (Forex Factory JSON) dengan auto-refresh berkala.
- **Chart real-time** berbasis Compose Canvas + overlay indikator
  (EMA 20/50/200, Bollinger, Fibonacci retracement, FVG, S/R, supply & demand).
- **Analisa Otomatis**: gabungan analisa fundamental + teknikal per instrumen,
  menampilkan bias, level entry/SL/TP, alasan, dan confidence.
- **Open Posisi Buy/Sell** dengan kalkulasi lot, TP/SL otomatis berdasarkan
  rasio TP (1:1, 1:2, 1:3) dan SL dalam persen modal.
- **Kalkulator Forex**: lot, pip value, margin, dan estimasi profit/loss.
- **Kalkulator Risiko**: position sizing berbasis risiko per transaksi.
- **Halaman News real-time** dengan penggabungan beberapa RSS feed dan
  penanda berita baru.
- **Copy Trade**: daftar trader profesional publik lengkap dengan portofolio,
  return, drawdown, dan opsi ikut copy trade.
- **Alarm Entry**: penjadwalan alarm pada jam tertentu (mis. saat rilis berita).
- **Sesi Trading**: Sydney, Tokyo, London, New York dengan status buka/tutup real-time.
- **Analisa Pasar**: order book, market profile, Fibonacci, EMA, RSI, FVG,
  support & resistance, supply & demand, dan market structure.
- **Watchlist** dan **Riwayat Posisi** persisten (Room).
- **Pengaturan**: kecerahan, tema (terang/gelap/sistem), format waktu & tanggal,
  bahasa (Indonesia/English), dan preferensi trading.
- **Notifikasi**: berita baru, perubahan harga, dan alarm entry.
- **Icon adaptif** "Nusantara Forex" untuk seluruh densitas layar.

### Catatan
- Aplikasi ini murni untuk tujuan edukasi dan analisa pasar. Data pasar berasal
  dari sumber publik (Yahoo Finance chart feed, Forex Factory JSON feed, dan RSS
  berita ekonomi) dan dapat tertunda.
