package com.cephcoding.tyde.features.auth.login

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cephcoding.tyde.core.designsystem.component.TydePrimaryButton
import com.cephcoding.tyde.core.designsystem.component.TydeSecondaryButton
import com.cephcoding.tyde.core.designsystem.theme.TydeSpacing
import com.cephcoding.tyde.features.auth.authErrorMessage
import org.koin.androidx.compose.koinViewModel

@Composable
fun LoginRoute(
    onNavigateToRegister: () -> Unit,
    viewModel: LoginViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    LoginScreen(
        state = uiState,
        onEmailChange = viewModel::onEmailChange,
        onPasswordChange = viewModel::onPasswordChange,
        onLoginClick = viewModel::onLoginClick,
        onGoogleSignInClick = { viewModel.onGoogleSignInClick(context) },
        onNavigateToRegister = onNavigateToRegister
    )
}

@Composable
private fun LoginScreen(
    state: LoginUiState,
    onEmailChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onLoginClick: () -> Unit,
    onGoogleSignInClick: () -> Unit,
    onNavigateToRegister: () -> Unit
) {
    Scaffold { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(TydeSpacing.large)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.Center
        ) {
            Text(text = "Welcome back", style = MaterialTheme.typography.headlineLarge)
            Spacer(modifier = Modifier.height(TydeSpacing.large))

            OutlinedTextField(
                value = state.email,
                onValueChange = onEmailChange,
                label = { Text("Email") },
                singleLine = true,
                enabled = !state.isLoading,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(TydeSpacing.medium))
            OutlinedTextField(
                value = state.password,
                onValueChange = onPasswordChange,
                label = { Text("Password") },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                enabled = !state.isLoading,
                modifier = Modifier.fillMaxWidth()
            )

            state.error?.let { error ->
                Spacer(modifier = Modifier.height(TydeSpacing.small))
                Text(
                    text = authErrorMessage(error),
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            Spacer(modifier = Modifier.height(TydeSpacing.large))
            TydePrimaryButton(
                text = if (state.isLoading) "Logging in…" else "Login",
                onClick = onLoginClick,
                enabled = !state.isLoading && state.email.isNotBlank() && state.password.isNotBlank()
            )
            Spacer(modifier = Modifier.height(TydeSpacing.medium))
            TydeSecondaryButton(
                text = "Continue with Google",
                onClick = onGoogleSignInClick,
                enabled = !state.isLoading
            )
            Spacer(modifier = Modifier.height(TydeSpacing.medium))
            TextButton(onClick = onNavigateToRegister, enabled = !state.isLoading) {
                Text("Don't have an account? Register")
            }
        }
    }
}
