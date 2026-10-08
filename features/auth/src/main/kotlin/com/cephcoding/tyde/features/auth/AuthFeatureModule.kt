package com.cephcoding.tyde.features.auth

import com.cephcoding.tyde.features.auth.google.GoogleIdTokenRequester
import com.cephcoding.tyde.features.auth.login.LoginViewModel
import com.cephcoding.tyde.features.auth.register.RegisterViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.core.qualifier.named
import org.koin.dsl.module

val authFeatureModule = module {
    single { GoogleIdTokenRequester(serverClientId = get(named("defaultWebClientId"))) }
    viewModel { LoginViewModel(get(), get()) }
    viewModel { RegisterViewModel(get()) }
}
