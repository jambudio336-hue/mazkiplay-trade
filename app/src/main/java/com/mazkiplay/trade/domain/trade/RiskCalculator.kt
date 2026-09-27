package com.mazkiplay.trade.domain.trade

import com.mazkiplay.trade.data.model.Instrument

/**
 * Risk-first helpers for the Risk Calculator screen: it flips the usual question
 * around and asks how much may be lost before the stop is hit.
 */
object RiskCalculator {

    data class RiskPlan(
        val riskAmount: Double,
        val rewardAmount: Double,
        val lot: Double,
        val stopPips: Double,
        val targetPips: Double,
        val riskOfBalance: Double,
        val breakEvenWinRate: Double,
        val maxConsecutiveLosses: Int,
        val advice: List<String>
    )

    /**
     * @param riskPercent share of the balance risked on a single trade
     * @param tpRatio reward multiple
     * @param winRate assumed strategy hit rate, used for the break-even check
     */
    fun plan(
        instrument: Instrument,
        balance: Double,
        riskPercent: Double,
        entry: Double,
        slPercent: Double,
        tpRatio: Double,
        winRate: Double = 55.0,
        leverage: Int = 100
    ): RiskPlan {
        val sized = PositionCalculator.size(
            instrument = instrument,
            direction = com.mazkiplay.trade.data.model.TradeDirection.BUY,
            entry = entry,
            balance = balance,
            riskPercent = riskPercent,
            tpRatio = tpRatio,
            slPercent = slPercent,
            leverage = leverage
        )

        // Break-even win rate for a given R:R = risk / (risk + reward).
        val breakEven = (1.0 / (1.0 + tpRatio)) * 100.0
        val maxLosses = if (riskPercent <= 0.0) 0 else (100.0 / riskPercent).toInt()

        val advice = ArrayList<String>()
        advice += if (riskPercent <= 1.0) "Risiko $riskPercent% per trade masih dalam batas konservatif."
        else if (riskPercent <= 2.0) "Risiko $riskPercent% per trade wajar untuk trader berpengalaman."
        else "Risiko $riskPercent% per trade terlalu besar; pertimbangkan 1-2%."
        advice += "Dengan R:R 1:${tpRatio.toInt()} Anda butuh win rate minimal ${String.format(java.util.Locale.US, "%.1f", breakEven)}% agar impas."
        advice += if (winRate >= breakEven) "Target win rate Anda ${String.format(java.util.Locale.US, "%.1f", winRate)}% berada di atas titik impas — ekspektasi positif."
        else "Target win rate Anda ${String.format(java.util.Locale.US, "%.1f", winRate)}% masih di bawah titik impas — perbaiki rasio TP atau akurasi."
        advice += "Modal tahan $maxLosses kali kerugian beruntun sebelum habis (asumsi risiko tetap)."
        sized.warnings.forEach { advice += it }

        return RiskPlan(
            riskAmount = sized.riskAmount,
            rewardAmount = sized.potentialProfit,
            lot = sized.lot,
            stopPips = sized.slDistancePips,
            targetPips = sized.tpDistancePips,
            riskOfBalance = riskPercent,
            breakEvenWinRate = breakEven,
            maxConsecutiveLosses = maxLosses,
            advice = advice
        )
    }
}
