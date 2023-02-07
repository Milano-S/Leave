package com.exclr8.xen4.room

import android.content.Context
import androidx.room.Room

object UrlsDatabaseBuilder {

    private var INSTANCE: UrlDatabase? = null

    fun getInstance(context: Context): UrlDatabase{
        if (INSTANCE == null) {
            synchronized(UrlDatabase::class) {
                INSTANCE = buildUrlDb(context)
            }
        }
        return INSTANCE!!
    }

    private fun buildUrlDb(context: Context) = Room.databaseBuilder(
        context.applicationContext,
        UrlDatabase::class.java,
        "base-urls"
    ).build()

}