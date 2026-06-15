package com.tradingapp.domain.model

/**
 * Display-only metadata for a crypto asset — independent of price data.
 *
 * Binance owns price/market data; this model owns display concerns (human-readable
 * name, logo URL). Keeping them separate allows the metadata source to change
 * (e.g. switch CDN, add a remote metadata API) without touching the price pipeline.
 *
 * [imageUrl] is null only when the provider cannot produce any URL for the symbol.
 * Unknown symbols still get a deterministic URL that may return a 404 — the UI
 * falls back to initials in that case.
 */
data class AssetMetadata(
    val baseSymbol: String,
    val displayName: String,
    val imageUrl: String?,
    val lastUpdated: Long = 0L,
)
