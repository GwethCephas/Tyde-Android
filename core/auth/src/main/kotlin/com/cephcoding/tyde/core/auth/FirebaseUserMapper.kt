package com.cephcoding.tyde.core.auth

import com.google.firebase.auth.FirebaseUser

fun FirebaseUser.toTydeUser(): TydeUser = TydeUser(
    uid = uid,
    email = email,
    displayName = displayName,
    photoUrl = photoUrl?.toString(),
    isEmailVerified = isEmailVerified
)
