package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.data.AppDatabase
import com.example.data.JournalRepository
import com.example.ui.JournalViewModel
import com.example.ui.JournalViewModelFactory
import com.example.ui.screens.*
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                val database = AppDatabase.getDatabase(applicationContext)
                val repository = JournalRepository(database.appDao())
                val journalViewModel: JournalViewModel = viewModel(
                    factory = JournalViewModelFactory(application, repository)
                )

                val navController = rememberNavController()

                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    NavHost(
                        navController = navController,
                        startDestination = "dashboard",
                        modifier = Modifier.padding(innerPadding)
                    ) {
                        composable("dashboard") {
                            DashboardScreen(
                                viewModel = journalViewModel,
                                onNavigateToLogTrade = {
                                    navController.navigate("log_trade")
                                },
                                onNavigateToCalculator = {
                                    navController.navigate("calculator")
                                },
                                onNavigateToPsychology = {
                                    navController.navigate("psychology")
                                },
                                onNavigateToPlan = {
                                    navController.navigate("plan")
                                },
                                onNavigateToHistory = {
                                    navController.navigate("history")
                                },
                                onNavigateToTradeDetail = { tradeId ->
                                    navController.navigate("detail/$tradeId")
                                },
                                onNavigateToBroker = {
                                    navController.navigate("broker_connection")
                                }
                            )
                        }

                        composable("broker_connection") {
                            BrokerScreen(
                                viewModel = journalViewModel,
                                onBack = { navController.popBackStack() }
                            )
                        }

                        composable("calculator") {
                            CalculatorScreen(
                                viewModel = journalViewModel,
                                onNavigateToLogTradeWithParams = { instrument, direction, entry, sl, lot ->
                                    val pairParam = instrument.uppercase()
                                    navController.navigate(
                                        "log_trade?pair=$pairParam&direction=$direction&entry=${entry.toFloat()}&sl=${sl.toFloat()}&lot=${lot.toFloat()}"
                                    )
                                },
                                onBack = { navController.popBackStack() }
                            )
                        }

                        composable(
                            route = "log_trade?pair={pair}&direction={direction}&entry={entry}&sl={sl}&lot={lot}",
                            arguments = listOf(
                                navArgument("pair") { nullable = true; defaultValue = null },
                                navArgument("direction") { nullable = true; defaultValue = null },
                                navArgument("entry") { type = NavType.FloatType; defaultValue = -1f },
                                navArgument("sl") { type = NavType.FloatType; defaultValue = -1f },
                                navArgument("lot") { type = NavType.FloatType; defaultValue = -1f }
                            )
                        ) { backStackEntry ->
                            val pair = backStackEntry.arguments?.getString("pair")
                            val direction = backStackEntry.arguments?.getString("direction")
                            val entry = backStackEntry.arguments?.getFloat("entry")?.takeIf { it >= 0f }?.toDouble()
                            val sl = backStackEntry.arguments?.getFloat("sl")?.takeIf { it >= 0f }?.toDouble()
                            val lot = backStackEntry.arguments?.getFloat("lot")?.takeIf { it >= 0f }?.toDouble()

                            LogTradeScreen(
                                viewModel = journalViewModel,
                                prefilledPair = pair,
                                prefilledDirection = direction,
                                prefilledEntry = entry,
                                prefilledSl = sl,
                                prefilledLot = lot,
                                onSuccess = {
                                    navController.navigate("dashboard") {
                                        popUpTo("dashboard") { inclusive = false }
                                    }
                                },
                                onBack = { navController.popBackStack() }
                            )
                        }

                        composable("psychology") {
                            PsychologyScreen(
                                viewModel = journalViewModel,
                                onSuccess = {
                                    navController.navigate("dashboard") {
                                        popUpTo("dashboard") { inclusive = false }
                                    }
                                },
                                onBack = { navController.popBackStack() }
                            )
                        }

                        composable("plan") {
                            TradingPlanScreen(
                                viewModel = journalViewModel,
                                onSuccess = { navController.popBackStack() },
                                onBack = { navController.popBackStack() }
                            )
                        }

                        composable("history") {
                            HistoryScreen(
                                viewModel = journalViewModel,
                                onNavigateToTradeDetail = { tradeId ->
                                    navController.navigate("detail/$tradeId")
                                },
                                onBack = { navController.popBackStack() }
                            )
                        }

                        composable(
                            route = "detail/{tradeId}",
                            arguments = listOf(
                                navArgument("tradeId") { type = NavType.IntType }
                            )
                        ) { backStackEntry ->
                            val tradeId = backStackEntry.arguments?.getInt("tradeId") ?: 0
                            CorrectionDetailScreen(
                                viewModel = journalViewModel,
                                tradeId = tradeId,
                                onSuccess = { navController.popBackStack() },
                                onBack = { navController.popBackStack() }
                            )
                        }
                    }
                }
            }
        }
    }
}
