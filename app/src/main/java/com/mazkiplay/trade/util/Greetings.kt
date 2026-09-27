package com.mazkiplay.trade.util

import kotlin.random.Random

/** Greetings shown on the splash screen — one is drawn at random on every launch. */
object Greetings {

    private val indonesian = listOf(
        "Selamat datang, Trader Nusantara!",
        "Semoga profit hari ini! 🚀",
        "Pasar menanti, siap analisa?",
        "Disiplin adalah kunci profit.",
        "Selamat trading, jaga money management!",
        "Nusantara bangkit, trader berkarya.",
        "Analisa dulu, eksekusi kemudian.",
        "Semoga sinyalnya akurat hari ini.",
        "Tenang, sabar, dan konsisten.",
        "Jangan lupa pasang stop loss!",
        "Salam profit dari Nusantara Forex.",
        "Tren adalah teman terbaikmu.",
        "Kontrol emosi, kontrol risiko.",
        "Hari baru, peluang baru.",
        "Risk 1%, tidur nyenyak."
    )

    private val english = listOf(
        "Welcome back, Nusantara trader!",
        "May the pips be with you today.",
        "Market is open — ready to analyse?",
        "Discipline beats prediction.",
        "Trade safe, size right!",
        "Plan the trade, trade the plan.",
        "Patience pays better than leverage.",
        "Fresh session, fresh opportunity.",
        "Risk one percent, sleep well.",
        "Let the trend pay your bills.",
        "Signals loaded. Stay sharp.",
        "Control the risk, keep the gains.",
        "Golden hour on charts ahead.",
        "Structure first, entries second.",
        "Trade what you see, not what you feel."
    )

    /**
     * Random greeting. Reseeded on purpose so that two launches never show the
     * same sentence back-to-back is a nice-to-have, not a requirement — the seed
     * is the call instant.
     */
    fun random(language: String): String {
        val pool = if (language == "in") indonesian else english
        return pool[Random(System.nanoTime() + System.currentTimeMillis()).nextInt(pool.size)]
    }
}
