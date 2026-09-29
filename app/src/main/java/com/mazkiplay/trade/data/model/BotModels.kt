package com.mazkiplay.trade.data.model

enum class BotChannel(val label: String) { TELEGRAM("Telegram"), WHATSAPP("WhatsApp Business"), DISCORD("Discord") }
enum class BotTrigger(val label: String) {
    NEW_SETUP("New BUY/SELL setup"), SETUP_WAIT("Setup berubah WAIT"), BREAKOUT("Breakout"), REVERSAL("Reversal"), TARGET("TP1/TP2 tercapai"), STOP("SL tercapai"), SPREAD("Spread melebar"), VOLATILITY("Volatility spike"), NEWS("Breaking/high-impact news"), EVENT("Event countdown/live/released"), MACRO("Macro regime berubah")
}
data class UnifiedSignalPayload(
    val symbol: String,
    val timeframe: String,
    val decision: String,
    val marketStatus: String,
    val entry: String,
    val stopLoss: String,
    val takeProfit1: String,
    val takeProfit2: String,
    val riskPercent: String,
    val rr: String,
    val lot: String,
    val technical: String,
    val fundamental: String,
    val macro: String,
    val news: String,
    val confluence: String,
    val riskGuardian: String,
    val explanation: String,
    val status: String = "ACTIVE"
)

data class BotDeliveryResult(val channel: BotChannel, val success: Boolean, val code: Int? = null, val message: String)
