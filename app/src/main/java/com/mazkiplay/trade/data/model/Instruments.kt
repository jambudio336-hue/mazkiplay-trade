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
        Instrument("SOLUSD", "SOL-USD", "Solana / USD", InstrumentClass.CRYPTO, 0.01, 4, 1.0, "USD", "SOL"),
        Instrument("XRPUSD", "XRP-USD", "XRP / USD", InstrumentClass.CRYPTO, 0.0001, 5, 1.0, "USD", "XRP"),
        Instrument("DOGEUSD", "DOGE-USD", "Dogecoin / USD", InstrumentClass.CRYPTO, 0.0001, 5, 1.0, "USD", "DOGE"),
        Instrument("ADAUSD", "ADA-USD", "Cardano / USD", InstrumentClass.CRYPTO, 0.0001, 5, 1.0, "USD", "ADA"),
        Instrument("USDTUSD", "USDT-USD", "Tether / USD", InstrumentClass.CRYPTO, 0.0001, 5, 1.0, "USD", "USDT"),
        Instrument("USDIDR", "USDIDR=X", "Dolar AS / Rupiah", InstrumentClass.MAJOR, 1.0, 0, 100000.0, "IDR", "USD"),
        Instrument("EURCHF", "EURCHF=X", "Euro / Franc", InstrumentClass.MINOR, 0.0001, 5, 100000.0, "CHF", "EUR"),
        Instrument("AUDNZD", "AUDNZD=X", "Aussie / Kiwi", InstrumentClass.MINOR, 0.0001, 5, 100000.0, "NZD", "AUD"),
        Instrument("USDSGD", "USDSGD=X", "Dolar AS / Dolar Singapura", InstrumentClass.MINOR, 0.0001, 5, 100000.0, "SGD", "USD"),
        Instrument("USDHKD", "USDHKD=X", "Dolar AS / Dolar Hong Kong", InstrumentClass.MINOR, 0.0001, 5, 100000.0, "HKD", "USD"),
        Instrument("USDTRY", "USDTRY=X", "Dolar AS / Lira", InstrumentClass.MINOR, 0.0001, 5, 100000.0, "TRY", "USD"),
        Instrument("USDMXN", "USDMXN=X", "Dolar AS / Peso", InstrumentClass.MINOR, 0.0001, 5, 100000.0, "MXN", "USD"),
        Instrument("USDZAR", "USDZAR=X", "Dolar AS / Rand", InstrumentClass.MINOR, 0.0001, 5, 100000.0, "ZAR", "USD"),
        Instrument("GBPCHF", "GBPCHF=X", "Pound / Franc", InstrumentClass.MINOR, 0.0001, 5, 100000.0, "CHF", "GBP"),
        Instrument("NZDJPY", "NZDJPY=X", "Kiwi / Yen", InstrumentClass.MINOR, 0.01, 3, 100000.0, "JPY", "NZD"),
        Instrument("COPPER", "HG=F", "Copper Futures", InstrumentClass.COMMODITY, 0.0001, 4, 25000.0, "USD", "HG"),
        Instrument("BRENT", "BZ=F", "Brent Crude Futures", InstrumentClass.COMMODITY, 0.01, 2, 1000.0, "USD", "BZ"),
        Instrument("NATGAS", "NG=F", "Natural Gas Futures", InstrumentClass.COMMODITY, 0.001, 3, 10000.0, "USD", "NG"),
        Instrument("WHEAT", "ZW=F", "Wheat Futures", InstrumentClass.COMMODITY, 0.25, 2, 5000.0, "USD", "ZW"),
        Instrument("CORN", "ZC=F", "Corn Futures", InstrumentClass.COMMODITY, 0.25, 2, 5000.0, "USD", "ZC"),
        Instrument("COFFEE", "KC=F", "Coffee Futures", InstrumentClass.COMMODITY, 0.05, 2, 37500.0, "USD", "KC"),
        Instrument("SP500F", "ES=F", "S&P 500 Futures", InstrumentClass.FUTURES, 0.25, 2, 50.0, "USD", "ES"),
        Instrument("NAS100F", "NQ=F", "Nasdaq Futures", InstrumentClass.FUTURES, 0.25, 2, 20.0, "USD", "NQ"),
        Instrument("DOWF", "YM=F", "Dow Futures", InstrumentClass.FUTURES, 1.0, 2, 5.0, "USD", "YM"),
        Instrument("NIKKEIF", "NKD=F", "Nikkei Futures", InstrumentClass.FUTURES, 5.0, 0, 5.0, "USD", "NKD"),
        Instrument("US30", "^DJI", "Dow Jones 30", InstrumentClass.INDEX, 1.0, 2, 1.0, "USD", "US30"),
        Instrument("NAS100", "^NDX", "Nasdaq 100", InstrumentClass.INDEX, 1.0, 2, 1.0, "USD", "NAS100"),
        Instrument("SP500", "^GSPC", "S&P 500", InstrumentClass.INDEX, 0.1, 2, 1.0, "USD", "SP500"),
        Instrument("DAX", "^GDAXI", "DAX Germany", InstrumentClass.INDEX, 0.1, 2, 1.0, "EUR", "DAX"),
        Instrument("FTSE", "^FTSE", "FTSE 100", InstrumentClass.INDEX, 0.1, 2, 1.0, "GBP", "FTSE"),
        Instrument("NIKKEI", "^N225", "Nikkei 225", InstrumentClass.INDEX, 1.0, 0, 1.0, "JPY", "NIKKEI"),
        Instrument("HSI", "^HSI", "Hang Seng", InstrumentClass.INDEX, 1.0, 0, 1.0, "HKD", "HSI"),
        Instrument("IHSG", "^JKSE", "IHSG / Jakarta Composite", InstrumentClass.INDEX, 0.1, 2, 1.0, "IDR", "IHSG"),
        Instrument("IDX30", "^JKID", "IDX30", InstrumentClass.INDEX, 0.1, 2, 1.0, "IDR", "IDX30"),
        Instrument("LQ45", "^JKLQ45", "LQ45", InstrumentClass.INDEX, 0.1, 2, 1.0, "IDR", "LQ45"),
        Instrument("BBCA", "BBCA.JK", "Bank Central Asia", InstrumentClass.EQUITY, 1.0, 0, 1.0, "IDR", "BBCA"),
        Instrument("BBRI", "BBRI.JK", "Bank Rakyat Indonesia", InstrumentClass.EQUITY, 1.0, 0, 1.0, "IDR", "BBRI"),
        Instrument("BMRI", "BMRI.JK", "Bank Mandiri", InstrumentClass.EQUITY, 1.0, 0, 1.0, "IDR", "BMRI"),
        Instrument("TLKM", "TLKM.JK", "Telkom Indonesia", InstrumentClass.EQUITY, 1.0, 0, 1.0, "IDR", "TLKM"),
        Instrument("ASII", "ASII.JK", "Astra International", InstrumentClass.EQUITY, 1.0, 0, 1.0, "IDR", "ASII"),
        Instrument("AAPL", "AAPL", "Apple", InstrumentClass.EQUITY, 0.01, 2, 1.0, "USD", "AAPL"),
        Instrument("MSFT", "MSFT", "Microsoft", InstrumentClass.EQUITY, 0.01, 2, 1.0, "USD", "MSFT"),
        Instrument("NVDA", "NVDA", "NVIDIA", InstrumentClass.EQUITY, 0.01, 2, 1.0, "USD", "NVDA"),
        Instrument("TSLA", "TSLA", "Tesla", InstrumentClass.EQUITY, 0.01, 2, 1.0, "USD", "TSLA"),
        Instrument("AMZN", "AMZN", "Amazon", InstrumentClass.EQUITY, 0.01, 2, 1.0, "USD", "AMZN"),
        Instrument("TOYOTA", "7203.T", "Toyota Motor", InstrumentClass.EQUITY, 1.0, 0, 1.0, "JPY", "TOYOTA"),
        Instrument("TENCENT", "0700.HK", "Tencent", InstrumentClass.EQUITY, 0.01, 2, 1.0, "HKD", "TENCENT"),
        Instrument("SAP", "SAP.DE", "SAP SE", InstrumentClass.EQUITY, 0.01, 2, 1.0, "EUR", "SAP"),
        Instrument("BHP", "BHP.AX", "BHP Group", InstrumentClass.EQUITY, 0.01, 2, 1.0, "AUD", "BHP"),
        Instrument("DXY", "DX-Y.NYB", "US Dollar Index", InstrumentClass.MACRO, 0.001, 3, 1.0, "USD", "DXY"),
        Instrument("VIX", "^VIX", "CBOE Volatility Index", InstrumentClass.MACRO, 0.01, 2, 1.0, "USD", "VIX"),
        Instrument("US10Y", "^TNX", "US 10Y Treasury Yield", InstrumentClass.BOND, 0.001, 3, 1.0, "USD", "US10Y"),
        Instrument("US30Y", "^TYX", "US 30Y Treasury Yield", InstrumentClass.BOND, 0.001, 3, 1.0, "USD", "US30Y")
    )

    /** The eight most traded symbols, used for the dashboard ticker. */
    val featured: List<Instrument> = listOf("XAUUSD", "EURUSD", "GBPUSD", "USDJPY", "AUDUSD", "USDCAD")
        .map { bySymbol(it) }

    val forexOnly: List<Instrument> get() = all.filter {
        it.klass == InstrumentClass.MAJOR || it.klass == InstrumentClass.MINOR
    }

    fun bySymbol(symbol: String): Instrument =
        all.firstOrNull { it.symbol.equals(symbol, ignoreCase = true) } ?: all.first()

    fun currencies(): List<String> =
        listOf("USD", "EUR", "GBP", "JPY", "AUD", "CAD", "CHF", "NZD", "XAU", "XAG", "BTC", "ETH")
}
