package com.example.tripexpensetracker

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavType
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Text
import androidx.compose.animation.*
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.tripexpensetracker.data.repository.AuthRepository
import com.example.tripexpensetracker.ui.login.LoginScreen
import com.example.tripexpensetracker.ui.expenses.AddEditExpenseScreen
import com.example.tripexpensetracker.ui.settlement.SettlementScreen
import com.example.tripexpensetracker.ui.theme.TripExpenseTrackerTheme
import com.example.tripexpensetracker.ui.trips.AddEditTripScreen
import com.example.tripexpensetracker.ui.trips.TripDetailsScreen
import com.example.tripexpensetracker.ui.trips.TripListScreen
import com.example.tripexpensetracker.ui.profile.ProfileScreen
import com.example.tripexpensetracker.ui.profile.ChangePasswordScreen

import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import com.example.tripexpensetracker.util.NotificationHelper
import android.Manifest
import android.os.Build

import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels

sealed class Screen(val route: String) {
    object Login : Screen("login")
    object TripList : Screen("trip_list")
    object AddEditTrip : Screen("add_edit_trip?tripId={tripId}") {
        fun createRoute(tripId: String? = null) = if (tripId == null) "add_edit_trip" else "add_edit_trip?tripId=$tripId"
    }
    object TripDetails : Screen("trip_details/{tripId}") {
        fun createRoute(tripId: String) = "trip_details/$tripId"
    }
    object AddEditExpense : Screen("add_edit_expense/{tripId}") {
        fun createRoute(tripId: String) = "add_edit_expense/$tripId"
    }
    object Settlement : Screen("settlement/{tripId}") {
        fun createRoute(tripId: String) = "settlement/$tripId"
    }
    object Profile : Screen("profile")

    object ChangePassword : Screen("change_password")
    object Onboarding : Screen("onboarding")
}

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    
    @Inject
    lateinit var authRepository: AuthRepository

    private val mainViewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        NotificationHelper.createNotificationChannel(this)
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
             registerForActivityResult(ActivityResultContracts.RequestPermission()) {}.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
        
        val isOnboardingCompleted = mainViewModel.isOnboardingCompleted
        val startDestination = if (!isOnboardingCompleted) {
             Screen.Onboarding.route
        } else if (authRepository.isLoggedIn()) {
             Screen.TripList.route
        } else {
             Screen.Login.route
        }
        
        setContent {
            TripExpenseTrackerTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val isConnected by mainViewModel.isConnected.collectAsState(initial = true)
                    
                    Column(modifier = Modifier.fillMaxSize()) {
                        OfflineBanner(
                            visible = !isConnected,
                            modifier = Modifier.fillMaxWidth()
                        )
                        
                        Box(modifier = Modifier.weight(1f)) {
                            AppNavigation(startDestination = startDestination)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AppNavigation(startDestination: String) {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = startDestination) {
        composable(Screen.Login.route) {
            LoginScreen(
                onLoginSuccess = {
                    navController.navigate(Screen.TripList.route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.Onboarding.route) {
            val mainViewModel: MainViewModel = androidx.hilt.navigation.compose.hiltViewModel()
            com.example.tripexpensetracker.ui.onboarding.OnboardingScreen(
                onOnboardingFinished = {
                    mainViewModel.completeOnboarding()
                    navController.navigate(Screen.Login.route) {
                        popUpTo(Screen.Onboarding.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.TripList.route) {
            TripListScreen(
                onTripClick = { tripId ->
                    navController.navigate(Screen.TripDetails.createRoute(tripId))
                },
                onAddTripClick = {
                    navController.navigate(Screen.AddEditTrip.createRoute(null))
                },
                onProfileClick = {
                    navController.navigate(Screen.Profile.route)
                }
            )
        }

        composable(
            route = Screen.AddEditTrip.route,
            arguments = listOf(
                navArgument("tripId") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                }
            )
        ) {
            AddEditTripScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(
            route = "trip_details/{tripId}",
            arguments = listOf(navArgument("tripId") { type = NavType.StringType })
        ) { backStackEntry ->
            val tripId = backStackEntry.arguments?.getString("tripId") ?: return@composable
            TripDetailsScreen(
                tripId = tripId,
                onBackClick = { navController.popBackStack() },
                onAddExpenseClick = { id -> navController.navigate("add_edit_expense/$id") },
                onSettleClick = { id -> navController.navigate("settlement/$id") },
                onEditTripClick = { id -> navController.navigate(Screen.AddEditTrip.createRoute(id)) }
            )
        }

        composable(
            route = "add_edit_expense/{tripId}",
            arguments = listOf(navArgument("tripId") { type = NavType.StringType })
        ) { backStackEntry ->
             val tripId = backStackEntry.arguments?.getString("tripId") ?: return@composable
             AddEditExpenseScreen(
                 tripId = tripId,
                 onNavigateBack = { navController.popBackStack() }
             )
        }

        composable(
            route = "settlement/{tripId}",
            arguments = listOf(navArgument("tripId") { type = NavType.StringType })
        ) { backStackEntry ->
            val tripId = backStackEntry.arguments?.getString("tripId") ?: return@composable
            SettlementScreen(
                tripId = tripId,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Screen.Profile.route) {
            ProfileScreen(
                onSignOut = {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(0) { inclusive = true } // Clear entire stack
                    }
                },
                onChangePassword = {
                    navController.navigate(Screen.ChangePassword.route)
                },
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Screen.ChangePassword.route) {
            ChangePasswordScreen(
                onNavigateBack = { navController.popBackStack() },
                onPasswordChanged = { navController.popBackStack() }
            )
        }
    }
}

@Composable
fun OfflineBanner(
    visible: Boolean,
    modifier: Modifier = Modifier
) {
    androidx.compose.animation.AnimatedVisibility(
        visible = visible,
        enter = androidx.compose.animation.expandVertically(expandFrom = androidx.compose.ui.Alignment.Top) + androidx.compose.animation.fadeIn(),
        exit = androidx.compose.animation.shrinkVertically(shrinkTowards = androidx.compose.ui.Alignment.Top) + androidx.compose.animation.fadeOut(),
        modifier = modifier
    ) {
        Surface(
            color = MaterialTheme.colorScheme.errorContainer,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = "You are offline. Changes will sync when online.",
                color = MaterialTheme.colorScheme.onErrorContainer,
                style = MaterialTheme.typography.labelSmall,
                modifier = Modifier.padding(8.dp),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }
    }
}
