package com.cephcoding.tyde.features.resume

import com.cephcoding.tyde.features.resume.data.local.ResumeFileReader
import com.cephcoding.tyde.features.resume.data.remote.ResumeApi
import com.cephcoding.tyde.features.resume.data.remote.ResumeRepositoryImpl
import com.cephcoding.tyde.features.resume.domain.ResumeRepository
import com.cephcoding.tyde.features.resume.presentation.ResumeViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module
import retrofit2.Retrofit

val resumeFeatureModule = module {
    single { get<Retrofit>().create(ResumeApi::class.java) }
    single { ResumeFileReader() }
    single<ResumeRepository> { ResumeRepositoryImpl(get()) }
    viewModel { ResumeViewModel(get(), get(), get()) }
}
