package com.tradingapp.domain.provider

import com.tradingapp.domain.model.AssetMetadata

/**
 * Synchronous provider for asset display metadata (name + logo URL).
 *
 * Intentionally not a suspend function — metadata is either static or cached in
 * memory, so callers never need to wait on IO. This also keeps the interface
 * testable without coroutine machinery.
 */
interface AssetMetadataProvider {
    fun getMetadata(baseSymbol: String): AssetMetadata
}
