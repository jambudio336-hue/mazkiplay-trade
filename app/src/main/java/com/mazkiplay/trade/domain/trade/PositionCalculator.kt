package com.mazkiplay.trade.domain.trade

import com.mazkiplay.trade.data.model.Instrument
import com.mazkiplay.trade.data.model.SizedTrade
import com.mazkiplay.trade.data.model.TradeDirection
import kotlin.math.abs

/**
 * Position sizing and money management.
 *
 * The engine answers one question: given a balance, a risk budget and a stop
 * distance, how many lots may be opened without losing more than the budget?
 */
object PositionCalculator {

    const val STANDARD_LOT_UNITS = 100_000.0

    /**
     * Pip value per 1.00 standard lot, expressed in USD.
     *
     * For a XXX/USD pair one pip is simply pipSize * 100k. For a USD/XXX pair the
     * value must be converted through the live price (pipSize * 100k / price), which
     * is why the price is an explicit argument.
     */
    fun pipValuePerLot(instrument: Instrument, price: Double): Double {
        if (instrument.klass == com.mazkiplay.trade.data.model.InstrumentClass.CRYPTO) {
            return instrument.pipSize
        }
        if (instrument.klass == com.mazkiplay.trade.data.model.InstrumentClass.INDEX) {
            return instrument.pipSize
        }
        val units = instrument.contractSize
        val quoteIsUsd = instrument.quoteCurrency == "USD"
        val baseIsUsd = instrument.baseCurrency == "USD"
        return when {
            quoteIsUsd -> instrument.pipSize * units
            baseIsUsd -> if (price <= 0.0) 0.0 else instrument.pipSize * units / price
            else -> instrument.pipSize * units // best effort for crosses
        }
    }

    /** Stop distance in pips from a percentage of the entry price. */
    fun stopDistancePips(instrument: Instrument, entry: Double, slPercent: Double): Double {
        if (entry <= 0.0 || instrument.pipSize <= 0.0) return 0.0
        val priceDistance = entry * (slPercent / 100.0)
        return priceDistance / instrument.pipSize
    }

    /**
     * Full sizing pass.
     *
     * @param tpRatio take-profit multiple of the stop distance (1:1, 1:2, 1:3)
     * @param slPercent stop distance as a percentage of the entry price
     */
    fun size(
        instrument: Instrument,
        direction: TradeDirection,
        entry: Double,
        balance: Double,
        riskPercent: Double,
        tpRatio: Double,
        slPercent: Double,
        leverage: Int = 100
    ): SizedTrade {
        val warnings = ArrayList<String>()
        val pipValue = pipValuePerLot(instrument, entry)
        val slPips = stopDistancePips(instrument, entry, slPercent)
        val riskAmount = balance * (riskPercent / 100.0)

        if (entry <= 0.0) warnings += "Harga entry tidak valid"
        if (balance <= 0.0) warnings += "Modal belum diisi"
        if (slPips <= 0.0) warnings += "Jarak stop loss nol"
        if (pipValue <= 0.0) warnings += "Nilai pip tidak dapat dihitung"

        val rawLot = if (pipValue > 0.0 && slPips > 0.0) riskAmount / (slPips * pipValue) else 0.0
        // Brokers trade in steps of 0.01 lot; keep two decimals and never round up.
        val lot = (kotlin.math.floor(rawLot * 100.0) / 100.0).coerceAtLeast(0.0)
        val units = lot * instrument.contractSize

        val slDistance = abs(entry * (slPercent / 100.0))
        val tpDistance = slDistance * tpRatio
        val stop = if (direction == TradeDirection.BUY) entry - slDistance else entry + slDistance
        val target = if (direction == TradeDirection.BUY) entry + tpDistance else entry - tpDistance

        val potentialLoss = lot * slPips * pipValue
        val potentialProfit = lot * (slPips * tpRatio) * pipValue
        val margin = if (leverage <= 0) 0.0 else (units * entry) / leverage

        if (margin > balance) warnings += "Margin dibutuhkan melebihi modal — kurangi lot atau pakai leverage lebih besar"
        if (lot < 0.01) warnings += "Lot hasil hitung di bawah minimum 0.01 — naikkan risiko atau perlebar modal"
        if (riskPercent > 3.0) warnings += "Risiko di atas 3% per transaksi termasuk agresif"

        return SizedTrade(
            symbol = instrument.symbol,
            direction = direction,
            balance = balance,
            riskPercent = riskPercent,
            riskAmount = riskAmount,
            stopLossPrice = stop,
            takeProfitPrice = target,
            entryPrice = entry,
            slDistancePips = slPips,
            tpDistancePips = slPips * tpRatio,
            pipValuePerLot = pipValue,
            lot = lot,
            units = units,
            marginRequired = margin,
            potentialProfit = potentialProfit,
            potentialLoss = potentialLoss,
            riskReward = tpRatio,
            valid = warnings.isEmpty() || (warnings.size == 1 && warnings[0].startsWith("Risiko di atas")),
            warnings = warnings
        )
    }

    /** Plain pip / margin / profit calculator used by the Calculator screen. */
    data class CalcResult(
        val pipValue: Double,
        val pipValueForLot: Double,
        val margin: Double,
        val leverage: Int,
        val profitForPips: Double,
        val lossForPips: Double
    )

    fun calculate(
        instrument: Instrument,
        lot: Double,
        currentPrice: Double,
        pips: Double,
        leverage: Int
    ): CalcResult {
        val pipValue = pipValuePerLot(instrument, currentPrice)
        val pipValueForLot = pipValue * lot
        val units = lot * instrument.contractSize
        val margin = if (leverage <= 0) 0.0 else (units * currentPrice) / leverage
        return CalcResult(
            pipValue = pipValue,
            pipValueForLot = pipValueForLot,
            margin = margin,
            leverage = leverage,
            profitForPips = pipValueForLot * abs(pips),
            lossForPips = pipValueForLot * abs(pips)
        )
    }

    /** Lot needed to risk [riskAmount] over [slPips] pips. */
    fun lotForRisk(riskAmount: Double, slPips: Double, pipValuePerLot: Double): Double {
        if (slPips <= 0.0 || pipValuePerLot <= 0.0) return 0.0
        return riskAmount / (slPips * pipValuePerLot)
    }
}
