package com.vipmail.di

import android.content.Context
import androidx.room.Room
import com.vipmail.data.db.AppDatabase
import com.vipmail.data.db.dao.EmailAccountDao
import com.vipmail.data.db.dao.EmailMessageDao
import com.vipmail.data.db.dao.VipSenderDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DataModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase =
        Room.databaseBuilder(context, AppDatabase::class.java, "vipmail.db").build()

    @Provides
    fun provideEmailAccountDao(database: AppDatabase): EmailAccountDao = database.emailAccountDao()

    @Provides
    fun provideEmailMessageDao(database: AppDatabase): EmailMessageDao = database.emailMessageDao()

    @Provides
    fun provideVipSenderDao(database: AppDatabase): VipSenderDao = database.vipSenderDao()
}

