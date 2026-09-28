# Changelog

Semua perubahan penting pada **Mazkiplay Trade** didokumentasikan di file ini.
Format mengikuti [Keep a Changelog](https://keepachangelog.com/id/1.1.0/)
dan proyek ini menggunakan [Semantic Versioning](https://semver.org/lang/id/).

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
- Dialog donasi rinci dengan logo DANA, nomor DANA `085262965282`, petunjuk transfer,
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
