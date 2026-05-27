package com.tradingapp.data.mapper

import com.tradingapp.database.entity.AssetEntity
import com.tradingapp.domain.model.Asset
import com.tradingapp.domain.model.PriceTick
import com.tradingapp.network.model.BinanceTicker24hrDto
import com.tradingapp.network.model.PriceTickDto

// --- DTO → Entity ---

private val NAME_MAP =
    mapOf(
        // Layer 1 — Proof of Work
        "BTC" to "Bitcoin",
        "LTC" to "Litecoin",
        "BCH" to "Bitcoin Cash",
        "XMR" to "Monero",
        "ZEC" to "Zcash",
        "DASH" to "Dash",
        "ETC" to "Ethereum Classic",
        // Layer 1 — Proof of Stake / other
        "ETH" to "Ethereum",
        "BNB" to "BNB",
        "SOL" to "Solana",
        "ADA" to "Cardano",
        "TRX" to "TRON",
        "AVAX" to "Avalanche",
        "DOT" to "Polkadot",
        "MATIC" to "Polygon",
        "ATOM" to "Cosmos",
        "NEAR" to "NEAR Protocol",
        "APT" to "Aptos",
        "SUI" to "Sui",
        "FTM" to "Fantom",
        "ALGO" to "Algorand",
        "HBAR" to "Hedera",
        "VET" to "VeChain",
        "ICP" to "Internet Computer",
        "XTZ" to "Tezos",
        "NEO" to "NEO",
        "EGLD" to "MultiversX",
        "ONE" to "Harmony",
        "THETA" to "Theta Network",
        "IOTA" to "IOTA",
        "FLOW" to "Flow",
        "KSM" to "Kusama",
        "KAVA" to "Kava",
        "ROSE" to "Oasis Network",
        "ZIL" to "Zilliqa",
        "QTUM" to "Qtum",
        "WAVES" to "Waves",
        // Cross-chain / interoperability
        "XRP" to "XRP",
        "XLM" to "Stellar",
        "EOS" to "EOS",
        "BAND" to "Band Protocol",
        "ZETA" to "ZetaChain",
        // Layer 2 / Rollups
        "OP" to "Optimism",
        "ARB" to "Arbitrum",
        "STRK" to "Starknet",
        "IMX" to "Immutable",
        "MANTA" to "Manta Network",
        "METIS" to "Metis",
        "BOBA" to "Boba Network",
        // DeFi
        "UNI" to "Uniswap",
        "LINK" to "Chainlink",
        "AAVE" to "Aave",
        "MKR" to "Maker",
        "CRV" to "Curve DAO",
        "COMP" to "Compound",
        "SNX" to "Synthetix",
        "1INCH" to "1inch",
        "SUSHI" to "SushiSwap",
        "BAL" to "Balancer",
        "YFI" to "Yearn Finance",
        "CAKE" to "PancakeSwap",
        "LDO" to "Lido DAO",
        "RPL" to "Rocket Pool",
        "GMX" to "GMX",
        "GRT" to "The Graph",
        "INJ" to "Injective",
        "TIA" to "Celestia",
        "SEI" to "Sei",
        // AI / infrastructure
        "FET" to "Fetch.ai",
        "RNDR" to "Render",
        "WLD" to "Worldcoin",
        "OCEAN" to "Ocean Protocol",
        "AGIX" to "SingularityNET",
        // NFT / gaming / metaverse
        "SAND" to "The Sandbox",
        "MANA" to "Decentraland",
        "AXS" to "Axie Infinity",
        "ENJ" to "Enjin Coin",
        "BLUR" to "Blur",
        "GALA" to "Gala",
        "CHZ" to "Chiliz",
        // Meme coins
        "DOGE" to "Dogecoin",
        "SHIB" to "Shiba Inu",
        "PEPE" to "Pepe",
        "FLOKI" to "Floki",
        "BONK" to "Bonk",
        "WIF" to "dogwifhat",
        // Exchange / utility tokens
        "OKB" to "OKB",
        "CRO" to "Cronos",
        "KCS" to "KuCoin Token",
        "HT" to "Huobi Token",
        // Inscriptions / ordinals
        "ORDI" to "ORDI",
        "SATS" to "SATS",
        // Stablecoins (shown for completeness — near-zero % change expected)
        "USDC" to "USD Coin",
        "FDUSD" to "First Digital USD",
        "TUSD" to "TrueUSD",
        "BUSD" to "Binance USD",
        "USDP" to "Pax Dollar",
    )

fun BinanceTicker24hrDto.toEntity(): AssetEntity {
    val bare = symbol.removeSuffix("USDT")
    return AssetEntity(
        symbol = bare,
        name = NAME_MAP[bare] ?: bare,
        price = lastPrice.toDoubleOrNull() ?: 0.0,
        change24h = priceChange.toDoubleOrNull() ?: 0.0,
        changePct24h = priceChangePercent.toDoubleOrNull() ?: 0.0,
        high24h = highPrice.toDoubleOrNull() ?: 0.0,
        low24h = lowPrice.toDoubleOrNull() ?: 0.0,
        // Binance public API has no market cap; use quoteVolume as a proxy
        marketCap = quoteVolume.toDoubleOrNull() ?: 0.0,
        volume24h = volume.toDoubleOrNull() ?: 0.0,
        logoUrl = "https://assets.coincap.io/assets/icons/${bare.lowercase()}@2x.png",
    )
}

// --- Entity → Domain ---

fun AssetEntity.toDomain(): Asset = Asset(
    symbol = symbol,
    name = name,
    currentPrice = price,
    priceChange24h = change24h,
    priceChangePct24h = changePct24h,
    high24h = high24h,
    low24h = low24h,
    marketCap = marketCap,
    volume24h = volume24h,
    logoUrl = logoUrl,
    isFavorite = isFavorite,
    lastUpdated = lastUpdated,
)

// --- Network tick → Domain tick ---

fun PriceTickDto.toDomain(): PriceTick = PriceTick(
    symbol = symbol,
    price = price,
    timestamp = timestamp,
)
