package com.cephcoding.tyde

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.cephcoding.tyde.core.auth.AuthState
import com.cephcoding.tyde.core.designsystem.component.TydeLoadingIndicator
import com.cephcoding.tyde.core.navigation.TydeDestination
import com.cephcoding.tyde.core.navigation.TydeNavHost
import com.cephcoding.tyde.features.auth.login.LoginRoute
import com.cephcoding.tyde.features.auth.register.RegisterRoute
import com.cephcoding.tyde.features.resume.presentation.ResumeRoute
import com.cephcoding.tyde.home.HomeScreen
import org.koin.androidx.compose.koinViewModel

@Composable
fun TydeApp(appViewModel: AppViewModel = koinViewModel()) {
    val authState by appViewModel.authState.collectAsStateWithLifecycle()

    when (val state = authState) {
        AuthState.Loading -> TydeLoadingIndicator(modifier = Modifier.fillMaxSize())
        else -> {
            val navController = rememberNavController()
            val startDestination = remember {
                if (state is AuthState.Authenticated) TydeDestination.Home else TydeDestination.Login
            }
            var previousAuthState by remember { mutableStateOf(authState) }

            TydeNavHost(
                navController = navController,
                startDestination = startDestination
            ) {
                composable<TydeDestination.Login> {
                    LoginRoute(onNavigateToRegister = { navController.navigate(TydeDestination.Register) })
                }
                composable<TydeDestination.Register> {
                    RegisterRoute(onNavigateToLogin = { navController.popBackStack() })
                }
                composable<TydeDestination.Home> {
                    val authenticatedEmail = (authState as? AuthState.Authenticated)?.user?.email
                    HomeScreen(
                        email = authenticatedEmail,
                        onNavigateToResume = { navController.navigate(TydeDestination.Resume) },
                        onLogout = appViewModel::logout
                    )
                }
                composable<TydeDestination.Resume> {
                    ResumeRoute(onBack = { navController.popBackStack() })
                }
            }

            LaunchedEffect(authState) {
                val previous = previousAuthState
                previousAuthState = authState
                when {
                    previous is AuthState.Authenticated && authState is AuthState.Unauthenticated ->
                        navController.navigate(TydeDestination.Login) { popUpTo(0) { inclusive = true } }
                    previous is AuthState.Unauthenticated && authState is AuthState.Authenticated ->
                        navController.navigate(TydeDestination.Home) { popUpTo(0) { inclusive = true } }
                }
            }
        }
    }
}
