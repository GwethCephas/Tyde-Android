package com.cephcoding.tyde.core.navigation

import kotlinx.serialization.Serializable

sealed interface TydeDestination {

    @Serializable
    data object Login : TydeDestination

    @Serializable
    data object Register : TydeDestination

    @Serializable
    data object Onboarding : TydeDestination

    @Serializable
    data object Home : TydeDestination

    @Serializable
    data object Resume : TydeDestination

    @Serializable
    data object Profile : TydeDestination

    @Serializable
    data object Jobs : TydeDestination

    @Serializable
    data class JobDetails(val jobId: String) : TydeDestination

    @Serializable
    data class Application(val jobId: String) : TydeDestination

    @Serializable
    data class ApplicationReview(val applicationId: String) : TydeDestination

    @Serializable
    data object Settings : TydeDestination
}
