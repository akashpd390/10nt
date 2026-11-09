package com.tenants.app

import android.app.Application
import com.tenants.app.di.appModule
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin

class TenantsApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidContext(this@TenantsApplication)
            modules(appModule)
        }
    }
}
