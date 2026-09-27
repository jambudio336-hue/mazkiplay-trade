# Changelog

Semua perubahan penting pada **Mazkiplay Trade** didokumentasikan di file ini.
Format mengikuti [Keep a Changelog](https://keepachangelog.com/id/1.1.0/)
dan proyek ini menggunakan [Semantic Versioning](https://semver.org/lang/id/).

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
