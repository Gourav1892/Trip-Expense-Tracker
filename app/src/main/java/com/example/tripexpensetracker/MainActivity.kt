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
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.TextButton
import android.content.Intent
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
import androidx.hilt.navigation.compose.hiltViewModel

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
    object Notifications : Screen("notifications")
    object ChangePassword : Screen("change_password")
    object Friends : Screen("friends")
    object AddFriend : Screen("add_friend")
    object SuggestedContacts : Screen("suggested_contacts")


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
            val isDarkTheme = remember { mutableStateOf(false) } // Ideally persisted
            TripExpenseTrackerTheme(darkTheme = isDarkTheme.value) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()
                    val joinViewModel: com.example.tripexpensetracker.ui.trips.JoinTripViewModel = hiltViewModel()
                    
                    // Handle Deep Link
                    val currentIntent = intent
                    LaunchedEffect(currentIntent) {
                        if (Intent.ACTION_VIEW == currentIntent.action && currentIntent.data != null) {
                            val data = currentIntent.data
                            if (data?.scheme == "tripexpensetracker" && data.host == "invite") {
                                val tripId = data.lastPathSegment
                                if (tripId != null) {
                                    joinViewModel.loadTripDetails(tripId)
                                }
                            }
                        }
                    }
                    
                    val joinState by joinViewModel.uiState.collectAsState()
                    
                    if (joinState is com.example.tripexpensetracker.ui.trips.JoinTripViewModel.JoinUiState.Loaded) {
                        val trip = (joinState as com.example.tripexpensetracker.ui.trips.JoinTripViewModel.JoinUiState.Loaded).trip
                        AlertDialog(
                            onDismissRequest = { joinViewModel.reset() },
                            title = { Text("Join Trip?") },
                            text = { Text("You have been invited to join '${trip.name}'.") },
                            confirmButton = {
                                Button(onClick = {
                                    joinViewModel.joinTrip(trip.id) {
                                        joinViewModel.reset()
                                        // Navigate to trip details
                                        navController.navigate(Screen.TripDetails.createRoute(trip.id)) {
                                            // Clear back stack to avoid weird loops? logic implies we are usually at root or trip list
                                        }
                                    }
                                }) {
                                    Text("Join")
                                }
                            },
                            dismissButton = {
                                TextButton(onClick = { joinViewModel.reset() }) {
                                    Text("Cancel")
                                }
                            }
                        )
                    }

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
                onTripClick = { tripId -> navController.navigate(Screen.TripDetails.createRoute(tripId)) },
                onAddTripClick = { navController.navigate(Screen.AddEditTrip.createRoute(null)) },
                onProfileClick = { navController.navigate(Screen.Profile.route) },
                onNotificationsClick = { navController.navigate(Screen.Notifications.route) }
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
                onNavigateBack = { navController.popBackStack() },
                onNavigateToTripDetails = { tripId: String -> 
                    navController.popBackStack()
                    navController.navigate(Screen.TripDetails.createRoute(tripId))
                }
            )
        }

        composable(Screen.TripDetails.route) { backStackEntry ->
             val tripId = backStackEntry.arguments?.getString("tripId") ?: return@composable
             TripDetailsScreen(
                 tripId = tripId,
                 onBackClick = { navController.popBackStack() },
                 onAddExpenseClick = {
                     navController.navigate(Screen.AddEditExpense.createRoute(tripId))
                 },
                 onSettleClick = {
                     navController.navigate(Screen.Settlement.createRoute(tripId))
                 },
                 onEditTripClick = {
                     navController.navigate(Screen.AddEditTrip.createRoute(tripId))
                 },
                 onNavigateToCityDetails = { destinationId ->
                     navController.navigate("cityDetails/$tripId/$destinationId")
                 }
             )
        }

        composable(
            route = Screen.AddEditExpense.route + "?destinationId={destinationId}",
            arguments = listOf(
                navArgument("tripId") { type = NavType.StringType },
                navArgument("destinationId") { 
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                }
            )
        ) { backStackEntry ->
             val tripId = backStackEntry.arguments?.getString("tripId") ?: return@composable
             val destinationId = backStackEntry.arguments?.getString("destinationId")
             AddEditExpenseScreen(
                 tripId = tripId,
                 destinationId = destinationId,
                 onNavigateBack = { navController.popBackStack() }
             )
        }

        composable(Screen.Settlement.route) { backStackEntry ->
            val tripId = backStackEntry.arguments?.getString("tripId") ?: return@composable
            SettlementScreen(
                tripId = tripId,
                onNavigateBack = { navController.popBackStack() }
            )
        }
        
        // City Details Route
        composable(
            route = "cityDetails/{tripId}/{destinationId}",
            arguments = listOf(
                navArgument("tripId") { type = NavType.StringType },
                navArgument("destinationId") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val tripId = backStackEntry.arguments?.getString("tripId") ?: return@composable
            val destinationId = backStackEntry.arguments?.getString("destinationId") ?: return@composable
            com.example.tripexpensetracker.ui.cities.CityDetailsScreen(
                tripId = tripId,
                destinationId = destinationId,
                onNavigateBack = { navController.popBackStack() },
                onAddExpense = { destId ->
                    navController.navigate("add_edit_expense/$tripId?destinationId=$destId")
                },
                onAddActivity = { destId ->
                    // For now, we can reuse the existing add itinerary flow
                    // Will enhance later to pre-select destination
                    navController.navigate(Screen.TripDetails.createRoute(tripId))
                },
                onEndCityVisit = {
                    // The viewModel in TripDetailsScreen will handle this
                    // We just need to navigate back
                }
            )
        }
        
        composable(Screen.Profile.route) {
            ProfileScreen(
                onSignOut = {
                     navController.navigate(Screen.Login.route) {
                         popUpTo(0) { inclusive = true }
                     }
                },
                onChangePassword = {
                    navController.navigate(Screen.ChangePassword.route)
                },
                onMyFriendsClick = {
                    navController.navigate(Screen.Friends.route)
                },
                onFindFriendsClick = {
                    navController.navigate(Screen.SuggestedContacts.route)
                },
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Screen.Notifications.route) {
            com.example.tripexpensetracker.ui.notifications.NotificationsScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Screen.ChangePassword.route) {
            ChangePasswordScreen(
                onNavigateBack = { navController.popBackStack() },
                onPasswordChanged = { navController.popBackStack() }
            )
        }

        composable(Screen.SuggestedContacts.route) {
            com.example.tripexpensetracker.ui.contacts.SuggestedContactsScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Screen.Friends.route) {
            com.example.tripexpensetracker.ui.friends.FriendsScreen(
                onNavigateBack = { navController.popBackStack() },
                onAddFriendClick = { navController.navigate(Screen.AddFriend.route) }
            )
        }

        composable(Screen.AddFriend.route) {
            com.example.tripexpensetracker.ui.friends.AddFriendScreen(
                onNavigateBack = { navController.popBackStack() }
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
