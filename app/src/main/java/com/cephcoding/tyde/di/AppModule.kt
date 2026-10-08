package com.cephcoding.tyde.di

import com.cephcoding.tyde.AppViewModel
import com.cephcoding.tyde.BuildConfig
import com.cephcoding.tyde.core.common.coroutines.DefaultDispatcherProvider
import com.cephcoding.tyde.core.common.coroutines.DispatcherProvider
import org.koin.core.module.dsl.viewModel
import org.koin.core.qualifier.named
import org.koin.dsl.module

val appModule = module {
    single<DispatcherProvider> { DefaultDispatcherProvider() }
    single(named("defaultWebClientId")) { BuildConfig.GOOGLE_WEB_CLIENT_ID }
    viewModel { AppViewModel(get()) }
}
