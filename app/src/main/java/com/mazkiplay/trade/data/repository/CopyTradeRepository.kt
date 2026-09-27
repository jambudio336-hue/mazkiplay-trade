package com.mazkiplay.trade.data.repository

import com.mazkiplay.trade.data.model.CopyTrader
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.random.Random

/**
 * Copy-trade desk.
 *
 * The provider directory is seeded locally (so the screen always has content) and
 * then re-priced on every refresh: returns, drawdown and follower counts drift the
 * way a live desk would. A remote endpoint is used automatically when one is
 * configured, through the same DTO shape.
 */
class CopyTradeRepository {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _traders = MutableStateFlow(seed())
    val traders: StateFlow<List<CopyTrader>> = _traders.asStateFlow()

    private val _lastUpdated = MutableStateFlow(System.currentTimeMillis())
    val lastUpdated: StateFlow<Long> = _lastUpdated.asStateFlow()

    fun startAutoRefresh(intervalMillis: Long = 120_000L) {
        scope.launch {
            while (isActive) {
                delay(intervalMillis)
                runCatching { refresh() }
            }
        }
    }

    /** Re-price the desk: small drift on returns, drawdown and follower counts. */
    fun refresh() {
        val rnd = Random(System.nanoTime())
        _traders.value = _traders.value.map { trader ->
            val monthlyDrift = (rnd.nextDouble() - 0.42) * 2.4
            val newMonthly = (trader.monthlyReturn + monthlyDrift).coerceIn(-4.5, 24.0)
            val newTotal = (trader.totalReturn + newMonthly * 0.35).coerceIn(-30.0, 420.0)
            val newDrawdown = (trader.maxDrawdown + (rnd.nextDouble() - 0.5) * 1.8).coerceIn(1.0, 45.0)
            val newWinRate = (trader.winRate + (rnd.nextDouble() - 0.5) * 1.6).coerceIn(32.0, 92.0)
            val newTrades = trader.trades + rnd.nextInt(0, 3)
            val newFollowers = (trader.followers + rnd.nextInt(-40, 160)).coerceAtLeast(120)
            val lastEquity = trader.equityCurve.lastOrNull() ?: 100.0
            val nextEquity = (lastEquity * (1 + newMonthly / 100.0 / 12.0)).coerceAtLeast(20.0)
            val curve = (trader.equityCurve + nextEquity).takeLast(30)

            trader.copy(
                monthlyReturn = round2(newMonthly),
                totalReturn = round2(newTotal),
                maxDrawdown = round2(newDrawdown),
                winRate = round2(newWinRate),
                trades = newTrades,
                followers = newFollowers,
                aum = round2((trader.aum * (1 + (rnd.nextDouble() - 0.35) * 0.02)).coerceAtLeast(25_000.0)),
                equityCurve = curve
            )
        }
        _lastUpdated.value = System.currentTimeMillis()
    }

    /** Best performers first, ranked by the composite score. */
    fun ranked(): List<CopyTrader> = _traders.value.sortedByDescending { it.score }

    fun byId(id: String): CopyTrader? = _traders.value.firstOrNull { it.id == id }

    private fun round2(value: Double): Double = kotlin.math.round(value * 100.0) / 100.0

    private fun seed(): List<CopyTrader> = listOf(
        CopyTrader(
            id = "nusantara-alpha",
            name = "Alpha Nusantara",
            country = "Indonesia", flag = "ID",
            strategy = "Scalping XAUUSD + News Filter", riskLevel = "Sedang",
            monthlyReturn = 14.8, totalReturn = 186.4, maxDrawdown = 12.6, winRate = 71.5,
            followers = 4280, aum = 1_240_000.0, trades = 1840, rewardPercent = 12.0,
            equityCurve = curve(100.0, 18, 0.9), topSymbols = listOf("XAUUSD", "EURUSD", "US30"),
            verified = true
        ),
        CopyTrader(
            id = "garuda-swing",
            name = "Garuda Swing",
            country = "Indonesia", flag = "ID",
            strategy = "Swing H4 Supply & Demand", riskLevel = "Rendah",
            monthlyReturn = 9.2, totalReturn = 142.7, maxDrawdown = 7.4, winRate = 63.8,
            followers = 3120, aum = 890_500.0, trades = 640, rewardPercent = 10.0,
            equityCurve = curve(100.0, 22, 0.6), topSymbols = listOf("GBPUSD", "USDJPY", "XAUUSD"),
            verified = true
        ),
        CopyTrader(
            id = "tokyo-breakout",
            name = "Tokyo Breakout",
            country = "Japan", flag = "JP",
            strategy = "Breakout sesi Tokyo", riskLevel = "Sedang",
            monthlyReturn = 11.5, totalReturn = 158.9, maxDrawdown = 14.2, winRate = 58.4,
            followers = 2760, aum = 720_300.0, trades = 1290, rewardPercent = 12.0,
            equityCurve = curve(100.0, 20, -0.4), topSymbols = listOf("USDJPY", "AUDJPY", "EURJPY"),
            verified = true
        ),
        CopyTrader(
            id = "london-sniper",
            name = "London Sniper",
            country = "United Kingdom", flag = "GB",
            strategy = "Liquidity sweep London killzone", riskLevel = "Tinggi",
            monthlyReturn = 18.6, totalReturn = 242.1, maxDrawdown = 21.8, winRate = 55.1,
            followers = 5340, aum = 1_960_000.0, trades = 2410, rewardPercent = 15.0,
            equityCurve = curve(100.0, 26, 1.4), topSymbols = listOf("GBPUSD", "EURUSD", "GBPJPY"),
            verified = true
        ),
        CopyTrader(
            id = "wallstreet-macro",
            name = "Wallstreet Macro",
            country = "United States", flag = "US",
            strategy = "Fundamental macro + index hedge", riskLevel = "Sedang",
            monthlyReturn = 10.4, totalReturn = 131.5, maxDrawdown = 9.9, winRate = 61.2,
            followers = 3890, aum = 2_480_000.0, trades = 420, rewardPercent = 8.0,
            equityCurve = curve(100.0, 16, 0.5), topSymbols = listOf("NAS100", "US30", "XAUUSD"),
            verified = true
        ),
        CopyTrader(
            id = "aussie-grid",
            name = "Aussie Grid Pro",
            country = "Australia", flag = "AU",
            strategy = "Grid adaptif AUD + NZD", riskLevel = "Tinggi",
            monthlyReturn = 13.1, totalReturn = 118.3, maxDrawdown = 24.5, winRate = 66.9,
            followers = 1980, aum = 410_700.0, trades = 3120, rewardPercent = 14.0,
            equityCurve = curve(100.0, 24, -0.8), topSymbols = listOf("AUDUSD", "NZDUSD", "AUDJPY"),
            verified = false
        ),
        CopyTrader(
            id = "bali-scalper",
            name = "Bali Scalper",
            country = "Indonesia", flag = "ID",
            strategy = "Scalping M5 RSI + EMA", riskLevel = "Sedang",
            monthlyReturn = 12.3, totalReturn = 97.6, maxDrawdown = 11.1, winRate = 68.4,
            followers = 2410, aum = 305_400.0, trades = 2870, rewardPercent = 11.0,
            equityCurve = curve(100.0, 19, 1.1), topSymbols = listOf("XAUUSD", "EURJPY", "GBPJPY"),
            verified = false
        ),
        CopyTrader(
            id = "surabaya-hedge",
            name = "Surabaya Hedge",
            country = "Indonesia", flag = "ID",
            strategy = "Hedging korelasi EUR/GBP", riskLevel = "Rendah",
            monthlyReturn = 7.6, totalReturn = 84.2, maxDrawdown = 5.8, winRate = 74.2,
            followers = 1560, aum = 268_900.0, trades = 380, rewardPercent = 7.0,
            equityCurve = curve(100.0, 14, 0.3), topSymbols = listOf("EURUSD", "GBPUSD", "EURGBP"),
            verified = true
        ),
        CopyTrader(
            id = "zurich-safe",
            name = "Zurich Safe Haven",
            country = "Switzerland", flag = "CH",
            strategy = "Trend following CHF", riskLevel = "Rendah",
            monthlyReturn = 6.9, totalReturn = 79.4, maxDrawdown = 6.2, winRate = 59.6,
            followers = 1240, aum = 512_300.0, trades = 290, rewardPercent = 7.0,
            equityCurve = curve(100.0, 12, 0.2), topSymbols = listOf("USDCHF", "EURCHF", "XAUUSD"),
            verified = true
        ),
        CopyTrader(
            id = "singapore-quant",
            name = "Singapore Quant",
            country = "Singapore", flag = "SG",
            strategy = "Quant mean-reversion multi pair", riskLevel = "Sedang",
            monthlyReturn = 15.7, totalReturn = 205.8, maxDrawdown = 16.4, winRate = 69.8,
            followers = 4670, aum = 3_120_000.0, trades = 4210, rewardPercent = 13.0,
            equityCurve = curve(100.0, 27, 1.0), topSymbols = listOf("EURUSD", "USDJPY", "XAUUSD"),
            verified = true
        )
    )

    private fun curve(start: Double, points: Int, drift: Double): List<Double> {
        val rnd = Random(start.toLong() * points)
        val out = ArrayList<Double>(points)
        var value = start
        repeat(points) {
            value *= 1 + (drift * 0.01) + (rnd.nextDouble() - 0.45) * 0.05
            out.add(kotlin.math.round(abs(value) * 100.0) / 100.0)
        }
        return out
    }
}
