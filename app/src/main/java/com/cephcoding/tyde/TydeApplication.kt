package com.cephcoding.tyde

import android.app.Application
import com.cephcoding.tyde.core.auth.authModule
import com.cephcoding.tyde.core.network.networkModule
import com.cephcoding.tyde.di.appModule
import com.cephcoding.tyde.features.auth.authFeatureModule
import com.cephcoding.tyde.features.resume.resumeFeatureModule
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin

class TydeApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidLogger()
            androidContext(this@TydeApplication)
            modules(networkModule, authModule, authFeatureModule, resumeFeatureModule, appModule)
        }
    }
}
