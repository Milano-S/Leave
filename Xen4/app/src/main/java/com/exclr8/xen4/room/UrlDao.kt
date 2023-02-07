package com.exclr8.xen4.room

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.exclr8.xen4.model.UrlsData

@Dao
interface UrlDao {

    @Query("SELECT * FROM UrlsData")
    suspend fun getAllBaseUrls(): List<UrlsData>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertUrl(baseUrl: UrlsData)

    @Query("DELETE FROM UrlsData")
    suspend fun deleteAllUrls()
}
