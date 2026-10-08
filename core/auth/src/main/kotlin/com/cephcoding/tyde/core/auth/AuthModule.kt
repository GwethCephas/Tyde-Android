package com.cephcoding.tyde.core.auth

import com.cephcoding.tyde.core.common.auth.AuthTokenProvider
import com.google.firebase.auth.FirebaseAuth
import org.koin.dsl.module

val authModule = module {
    single { FirebaseAuth.getInstance() }
    single<AuthRepository>(createdAtStart = true) { FirebaseAuthRepository(get()) }
    single<AuthTokenProvider> { FirebaseAuthTokenProvider(get()) }
}
