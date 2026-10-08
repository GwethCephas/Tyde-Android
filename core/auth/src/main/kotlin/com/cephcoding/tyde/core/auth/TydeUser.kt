package com.cephcoding.tyde.core.auth

data class TydeUser(
    val uid: String,
    val email: String?,
    val displayName: String?,
    val photoUrl: String?,
    val isEmailVerified: Boolean
)
