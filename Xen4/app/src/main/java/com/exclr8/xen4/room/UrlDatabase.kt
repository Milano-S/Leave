package com.exclr8.xen4.room

import androidx.room.Database
import androidx.room.RoomDatabase
import com.exclr8.xen4.model.UrlsData

@Database(entities = [UrlsData::class], version = 1 , exportSchema = false)
abstract class UrlDatabase : RoomDatabase(){
    abstract fun UrlDao(): UrlDao
}