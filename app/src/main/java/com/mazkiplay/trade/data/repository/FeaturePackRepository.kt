package com.mazkiplay.trade.data.repository

import com.mazkiplay.trade.data.model.AcademyLesson
import com.mazkiplay.trade.data.model.DataHealth
import com.mazkiplay.trade.data.model.EventState
import com.mazkiplay.trade.data.model.Impact
import com.mazkiplay.trade.data.model.LiveEconomicEvent
import com.mazkiplay.trade.data.model.MarketBrief
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.TimeUnit

class FeaturePackRepository {
    private val _emergencyLock = MutableStateFlow(false)
    val emergencyLock: StateFlow<Boolean> = _emergencyLock.asStateFlow()
    private val _riskProfile = MutableStateFlow(com.mazkiplay.trade.data.model.RiskProfile.BALANCED)
    val riskProfile: StateFlow<com.mazkiplay.trade.data.model.RiskProfile> = _riskProfile.asStateFlow()
    private val _health = MutableStateFlow(DataHealth())
    val health: StateFlow<DataHealth> = _health.asStateFlow()

    val lessons: List<AcademyLesson> = listOf(
        AcademyLesson("professional", "Professional Trader", "🎓", "Fondasi market, order, leverage, margin, spread, liquidity, dan trading plan.", listOf("Financial market: forex, saham, crypto, commodities, index", "Bid/ask, spread, liquidity, slippage", "Entry, stop loss, take profit, position sizing", "Candlestick dibaca bersama konteks; pattern tidak menjamin arah harga")),
        AcademyLesson("money", "Money Management", "💰", "Mengubah balance dan risk menjadi position size yang terukur.", listOf("Fixed fractional risk", "Risk amount = balance × risk %", "Stop distance, pip value, margin, leverage", "Daily loss limit, drawdown, exposure, correlation, risk of ruin")),
        AcademyLesson("candles", "Candlestick Mastery", "🕯️", "Open, high, low, close, body, wick, dan pola dengan konteks.", listOf("Bullish, bearish, doji, hammer, shooting star", "Engulfing, morning star, evening star, inside bar", "Konfirmasi trend, level, volume, dan timeframe")),
        AcademyLesson("technical", "Technical Analysis", "📊", "Trend, momentum, structure, indicator, confluence, dan multi-timeframe.", listOf("SMA, EMA, RSI, MACD, Bollinger, ATR, ADX", "Support, resistance, breakout, retest, reversal", "BOS, CHoCH, VWAP, pivot, Fibonacci, supply-demand", "1D → 4H → 1H → 15M → 5M context")),
        AcademyLesson("fundamental", "Fundamental Analysis", "🌍", "Membaca data ekonomi dan bisnis menjadi konteks, bukan sinyal buta.", listOf("Revenue, earnings, EPS, P/E, debt, cash flow", "Interest rate, inflation, employment, GDP, PMI", "Central bank, bond yield, currency strength", "Fundamental + technical + market context → trading plan")),
        AcademyLesson("macro", "Macro Economics", "🌐", "Memahami hubungan inflasi, kebijakan, yield, currency, gold, dan equities.", listOf("Inflation → central bank → rate expectations → yields", "Fiscal/monetary policy, yield curve, liquidity", "Risk-on, risk-off, recession, economic cycle")),
        AcademyLesson("news", "News & Economic Calendar", "📰", "Previous, forecast, actual, surprise, impact, dan news risk management.", listOf("NFP, CPI, PPI, FOMC, ECB, BOE, BOJ, GDP, PMI", "Actual vs forecast → economic surprise", "News blackout, spread/slippage, and high-impact event mode")),
        AcademyLesson("psychology", "Trading Psychology", "🧠", "Menjaga proses, disiplin, dan risiko ketika hasil tidak bisa dikontrol.", listOf("FOMO, revenge trading, overtrading", "Fear, greed, confirmation bias, loss aversion", "Trading plan, journaling, process vs outcome")),
        AcademyLesson("backtest", "Strategy & Backtesting", "🧪", "Menguji rules secara historis tanpa menjadikan win rate sebagai satu-satunya target.", listOf("Historical, forward, out-of-sample, walk-forward testing", "Overfitting, survivorship bias, spread, commission, slippage", "Win rate, profit factor, expectancy, drawdown, Sharpe, Average R")),
        AcademyLesson("journal", "Trading Journal", "📓", "Merekam setup, keputusan, hasil, kesalahan, dan lesson.", listOf("Date, instrument, timeframe, direction, entry, SL, TP", "Position size, risk, strategy, market condition", "Reason, result, mistake, lesson, screenshot")),
        AcademyLesson("checklist", "Professional Checklist", "✅", "Checklist sebelum entry, saat management, exit, dan post-trade review.", listOf("Trend dan higher timeframe", "Technical, fundamental, news, entry, SL, TP", "R:R, position size, daily risk, drawdown, spread", "Trading plan → entry → management → exit → review"))
    )

    val events: List<LiveEconomicEvent> get() {
        val now = System.currentTimeMillis()
        return listOf(
            LiveEconomicEvent("fomc", "FOMC Press Conference", "Federal Reserve", "USD", now + TimeUnit.MINUTES.toMillis(42), Impact.HIGH, "https://www.federalreserve.gov/monetarypolicy/fomccalendars.htm"),
            LiveEconomicEvent("ecb", "ECB Monetary Policy Press Conference", "European Central Bank", "EUR", now + TimeUnit.HOURS.toMillis(3), Impact.HIGH, "https://www.ecb.europa.eu/press/tvservices/webcast/html/index.en.html"),
            LiveEconomicEvent("boe", "MPC Press Conference", "Bank of England", "GBP", now - TimeUnit.MINUTES.toMillis(8), Impact.MEDIUM, "https://www.bankofengland.co.uk/news", EventState.ENDED)
        )
    }

    fun setEmergencyLock(enabled: Boolean) { _emergencyLock.value = enabled }
    fun setRiskProfile(profile: com.mazkiplay.trade.data.model.RiskProfile) { _riskProfile.value = profile }
    fun refreshHealth(market: String = "LIVE") { _health.value = _health.value.copy(market = market, lastUpdateMillis = System.currentTimeMillis()) }

    fun brief(highImpact: Int, live: Int, signals: Int): MarketBrief = MarketBrief("Strong", "Neutral", "High volatility", "Trending", highImpact, live, signals, if (_health.value.signalPaused) "SIGNAL PAUSED · market data stale" else null)
}
