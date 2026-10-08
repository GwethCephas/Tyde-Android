package com.cephcoding.tyde.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.cephcoding.tyde.core.designsystem.component.TydeCard
import com.cephcoding.tyde.core.designsystem.component.TydePrimaryButton
import com.cephcoding.tyde.core.designsystem.component.TydeSecondaryButton
import com.cephcoding.tyde.core.designsystem.theme.TydeSpacing

@Composable
fun HomeScreen(
    email: String?,
    onNavigateToResume: () -> Unit,
    onLogout: () -> Unit
) {
    Scaffold { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(TydeSpacing.medium),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "Tyde",
                style = MaterialTheme.typography.headlineLarge
            )
            TydeCard(modifier = Modifier.padding(top = TydeSpacing.large)) {
                Text(
                    text = "Your AI-powered job application assistant.",
                    style = MaterialTheme.typography.bodyLarge
                )
                if (email != null) {
                    Text(
                        text = "Signed in as $email",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
            Spacer(modifier = Modifier.height(TydeSpacing.large))
            TydePrimaryButton(text = "Resume", onClick = onNavigateToResume)
            Spacer(modifier = Modifier.height(TydeSpacing.medium))
            TydeSecondaryButton(text = "Logout", onClick = onLogout)
        }
    }
}
