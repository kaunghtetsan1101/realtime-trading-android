package com.tradingapp.data.provider

import com.tradingapp.domain.model.AssetMetadata
import com.tradingapp.domain.provider.AssetMetadataProvider
import javax.inject.Inject
import javax.inject.Singleton

/**
 * In-memory metadata provider backed by a hard-coded name map and the CoinCap CDN.
 *
 * Image URL pattern: https://assets.coincap.io/assets/icons/<symbol_lowercase>@2x.png
 * Unknown symbols still receive a URL — the image may 404, and the UI falls back to
 * the initials avatar in that case.
 */
@Singleton
class StaticAssetMetadataProvider @Inject constructor() : AssetMetadataProvider {

    override fun getMetadata(baseSymbol: String): AssetMetadata = AssetMetadata(
        baseSymbol = baseSymbol,
        displayName = NAME_MAP[baseSymbol] ?: baseSymbol,
        imageUrl = "https://assets.coincap.io/assets/icons/${baseSymbol.lowercase()}@2x.png",
    )
}

internal val NAME_MAP: Map<String, String> = mapOf(
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
    // Stablecoins
    "USDC" to "USD Coin",
    "FDUSD" to "First Digital USD",
    "TUSD" to "TrueUSD",
    "BUSD" to "Binance USD",
    "USDP" to "Pax Dollar",
)
