package com.tradingapp.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.tradingapp.database.entity.AssetEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AssetDao {
    /** Observe all assets ordered by market cap descending. */
    @Query("SELECT * FROM assets ORDER BY market_cap DESC")
    fun observeAll(): Flow<List<AssetEntity>>

    /** Observe only favorited assets. */
    @Query("SELECT * FROM assets WHERE is_favorite = 1 ORDER BY market_cap DESC")
    fun observeFavorites(): Flow<List<AssetEntity>>

    @Query("SELECT * FROM assets WHERE symbol = :symbol LIMIT 1")
    fun observeBySymbol(symbol: String): Flow<AssetEntity?>

    /**
     * Insert rows that do not yet exist. IGNORE = skip if the symbol is already present.
     * Used as step 1 of the safe sync upsert so that [is_favorite] is never overwritten.
     */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAllIgnore(assets: List<AssetEntity>)

    /**
     * Update only the market-data columns for an existing row.
     * [is_favorite] and [last_updated] (for the WS price column) are intentionally excluded.
     * Used as step 2 of the safe sync upsert.
     */
    @Query(
        """
        UPDATE assets SET
            name           = :name,
            price          = :price,
            change_24h     = :change24h,
            change_pct_24h = :changePct24h,
            high_24h       = :high24h,
            low_24h        = :low24h,
            market_cap     = :marketCap,
            volume_24h     = :volume24h,
            logo_url       = :logoUrl
        WHERE symbol = :symbol
        """,
    )
    suspend fun updateMarketData(
        symbol: String,
        name: String,
        price: Double,
        change24h: Double,
        changePct24h: Double,
        high24h: Double,
        low24h: Double,
        marketCap: Double,
        volume24h: Double,
        logoUrl: String?,
    )

    /** Partial update — only price columns to avoid overwriting favorite flag. */
    @Query(
        """
        UPDATE assets
        SET price        = :price,
            last_updated = :timestamp
        WHERE symbol     = :symbol
        """,
    )
    suspend fun updatePrice(symbol: String, price: Double, timestamp: Long)

    @Query("UPDATE assets SET is_favorite = :isFavorite WHERE symbol = :symbol")
    suspend fun setFavorite(symbol: String, isFavorite: Boolean)

    @Query("SELECT COUNT(*) FROM assets")
    suspend fun count(): Int

    /**
     * Returns the bare symbols (e.g. "BTC") of the top [limit] assets ordered by market cap.
     * Used on startup to rebuild the WebSocket subscription URL from cached DB data.
     */
    @Query("SELECT symbol FROM assets ORDER BY market_cap DESC LIMIT :limit")
    suspend fun getTopSymbols(limit: Int): List<String>
}
