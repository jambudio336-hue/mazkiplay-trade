package com.mazkiplay.trade.data.model

/** The instrument universe shipped with Mazkiplay Trade. */
object Instruments {

    val all: List<Instrument> = listOf(
        Instrument("XAUUSD", "GC=F", "Emas / Gold", InstrumentClass.METAL, 0.1, 2, 100.0, "USD", "XAU"),
        Instrument("XAGUSD", "SI=F", "Perak / Silver", InstrumentClass.METAL, 0.01, 3, 5000.0, "USD", "XAG"),
        Instrument("WTIUSD", "CL=F", "Minyak WTI", InstrumentClass.METAL, 0.01, 2, 1000.0, "USD", "WTI"),
        Instrument("EURUSD", "EURUSD=X", "Euro / Dolar AS", InstrumentClass.MAJOR, 0.0001, 5, 100000.0, "USD", "EUR"),
        Instrument("GBPUSD", "GBPUSD=X", "Pound / Dolar AS", InstrumentClass.MAJOR, 0.0001, 5, 100000.0, "USD", "GBP"),
        Instrument("USDJPY", "USDJPY=X", "Dolar AS / Yen", InstrumentClass.MAJOR, 0.01, 3, 100000.0, "JPY", "USD"),
        Instrument("USDCHF", "USDCHF=X", "Dolar AS / Franc", InstrumentClass.MAJOR, 0.0001, 5, 100000.0, "CHF", "USD"),
        Instrument("AUDUSD", "AUDUSD=X", "Aussie / Dolar AS", InstrumentClass.MAJOR, 0.0001, 5, 100000.0, "USD", "AUD"),
        Instrument("USDCAD", "USDCAD=X", "Dolar AS / Loonie", InstrumentClass.MAJOR, 0.0001, 5, 100000.0, "CAD", "USD"),
        Instrument("NZDUSD", "NZDUSD=X", "Kiwi / Dolar AS", InstrumentClass.MAJOR, 0.0001, 5, 100000.0, "USD", "NZD"),
        Instrument("EURJPY", "EURJPY=X", "Euro / Yen", InstrumentClass.MINOR, 0.01, 3, 100000.0, "JPY", "EUR"),
        Instrument("GBPJPY", "GBPJPY=X", "Pound / Yen", InstrumentClass.MINOR, 0.01, 3, 100000.0, "JPY", "GBP"),
        Instrument("AUDJPY", "AUDJPY=X", "Aussie / Yen", InstrumentClass.MINOR, 0.01, 3, 100000.0, "JPY", "AUD"),
        Instrument("EURGBP", "EURGBP=X", "Euro / Pound", InstrumentClass.MINOR, 0.0001, 5, 100000.0, "GBP", "EUR"),
        Instrument("GBPAUD", "GBPAUD=X", "Pound / Aussie", InstrumentClass.MINOR, 0.0001, 5, 100000.0, "AUD", "GBP"),
        Instrument("EURAUD", "EURAUD=X", "Euro / Aussie", InstrumentClass.MINOR, 0.0001, 5, 100000.0, "AUD", "EUR"),
        Instrument("BTCUSD", "BTC-USD", "Bitcoin / USD", InstrumentClass.CRYPTO, 1.0, 2, 1.0, "USD", "BTC"),
        Instrument("ETHUSD", "ETH-USD", "Ethereum / USD", InstrumentClass.CRYPTO, 0.1, 2, 1.0, "USD", "ETH"),
        Instrument("US30", "^DJI", "Dow Jones 30", InstrumentClass.INDEX, 1.0, 2, 1.0, "USD", "US30"),
        Instrument("NAS100", "^NDX", "Nasdaq 100", InstrumentClass.INDEX, 1.0, 2, 1.0, "USD", "NAS100")
    )

    /** The eight most traded symbols, used for the dashboard ticker. */
    val featured: List<Instrument> = listOf("XAUUSD", "EURUSD", "GBPUSD", "USDJPY", "AUDUSD", "USDCAD")
        .map { bySymbol(it) }

    val forexOnly: List<Instrument> get() = all.filter { it.klass == InstrumentClass.MAJOR || it.klass == InstrumentClass.MINOR }

    fun bySymbol(symbol: String): Instrument =
        all.firstOrNull { it.symbol.equals(symbol, ignoreCase = true) } ?: all.first()

    fun currencies(): List<String> =
        listOf("USD", "EUR", "GBP", "JPY", "AUD", "CAD", "CHF", "NZD", "XAU", "XAG", "BTC", "ETH")
}
