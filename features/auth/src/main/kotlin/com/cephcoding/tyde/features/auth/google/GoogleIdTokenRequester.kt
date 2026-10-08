package com.cephcoding.tyde.features.auth.google

import android.content.Context
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential

class GoogleIdTokenRequester(
    private val serverClientId: String
) {
    suspend fun requestIdToken(context: Context): String {
        val credentialManager = CredentialManager.create(context)
        val option = GetGoogleIdOption.Builder()
            .setServerClientId(serverClientId)
            .setFilterByAuthorizedAccounts(false)
            .build()
        val request = GetCredentialRequest.Builder()
            .addCredentialOption(option)
            .build()
        val response = credentialManager.getCredential(context, request)
        val credential = GoogleIdTokenCredential.createFrom(response.credential.data)
        return credential.idToken
    }
}
