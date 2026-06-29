@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class, androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.PaperPosition
import com.example.ui.JournalViewModel
import com.example.ui.theme.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun BrokerScreen(
    viewModel: JournalViewModel,
    onBack: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val isPaperMode by viewModel.isPaperMode.collectAsState()
    val oandaConnected by viewModel.oandaConnected.collectAsState()
    val balance by viewModel.brokerBalance.collectAsState()
    val currency by viewModel.brokerCurrency.collectAsState()
    val openPositionsCount by viewModel.brokerOpenPositionsCount.collectAsState()
    val paperPositions by viewModel.paperPositions.collectAsState()
    val errorMsg by viewModel.brokerError.collectAsState()
    val isLoading by viewModel.brokerLoading.collectAsState()
    val livePrices by viewModel.livePrices.collectAsState()
    val activeLocks by viewModel.activeLocks.collectAsState()

    // OANDA input fields
    var oandaTokenInput by remember { mutableStateOf("") }
    var oandaAccountIdInput by remember { mutableStateOf("") }
    var selectedEnv by remember { mutableStateOf("sandbox") } // sandbox, practice, live
    var showConnectionForm by remember { mutableStateOf(!oandaConnected) }

    // Quick Order fields
    var selectedAsset by remember { mutableStateOf("EUR/USD") }
    var lotSizeInput by remember { mutableStateOf("0.10") }
    var snackbarHostState = remember { SnackbarHostState() }

    // Notification states
    var executionSuccessMsg by remember { mutableStateOf<String?>(null) }
    var executionErrorMsg by remember { mutableStateOf<String?>(null) }

    val currentPrice = livePrices[selectedAsset] ?: 1.0

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "LIVE EXECUTION CENTER",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = OffWhite,
                            letterSpacing = 1.sp
                        )
                        Text(
                            text = "BROKER & PAPER INTEGRATION Engine",
                            fontSize = 10.sp,
                            color = CosmicTeal,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("broker_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = OffWhite
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = SpaceBlack)
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = SpaceBlack
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // --- 1. Connection Mode Selector ---
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = SlateDark),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "EXECUTION ROUTE",
                            color = MutedSteel,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.2.sp,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Button(
                                onClick = { viewModel.switchBrokerMode(true) },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isPaperMode) CosmicPurple else SlateCard
                                ),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .testTag("broker_mode_paper")
                            ) {
                                Icon(Icons.Default.Science, contentDescription = null, tint = OffWhite)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Paper Account", color = OffWhite, fontWeight = FontWeight.Bold)
                            }

                            Button(
                                onClick = { 
                                    if (oandaConnected) {
                                        viewModel.switchBrokerMode(false)
                                    } else {
                                        showConnectionForm = true
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (!isPaperMode) CosmicTeal else SlateCard
                                ),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .testTag("broker_mode_oanda")
                            ) {
                                Icon(Icons.Default.CloudQueue, contentDescription = null, tint = OffWhite)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("OANDA API", color = OffWhite, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            // --- 2. Live Account Details Card ---
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = SlateDark),
                    border = BorderStroke(
                        1.dp, 
                        if (isPaperMode) CosmicPurple.copy(alpha = 0.5f) else CosmicTeal.copy(alpha = 0.5f)
                    ),
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .clip(CircleShape)
                                        .background(if (isPaperMode || oandaConnected) TradeProfit else TradeLoss)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = if (isPaperMode) "DEMO / PAPER PRACTICE" else "OANDA LIVE ACCOUNT",
                                    color = MutedSteel,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp,
                                    letterSpacing = 1.sp
                                )
                            }

                            if (!isPaperMode && oandaConnected) {
                                IconButton(
                                    onClick = { viewModel.disconnectOanda() },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(Icons.Default.ExitToApp, contentDescription = "Disconnect", tint = TradeLoss)
                                }
                            } else if (isPaperMode) {
                                IconButton(
                                    onClick = { viewModel.resetPaperTrading() },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(Icons.Default.Refresh, contentDescription = "Reset Demo Account", tint = MutedSteel)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = String.format(Locale.US, "$%,.2f", balance),
                            color = OffWhite,
                            fontSize = 36.sp,
                            fontWeight = FontWeight.Black,
                            fontFamily = FontFamily.Monospace,
                            letterSpacing = (-0.5).sp
                        )

                        Text(
                            text = "AVAILABLE BALANCE ($currency)",
                            color = MutedSteel,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.2.sp,
                            modifier = Modifier.padding(top = 4.dp)
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        HorizontalDivider(color = SlateCard, thickness = 1.dp)

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("Open Positions", color = MutedSteel, fontSize = 11.sp)
                                Text(
                                    text = openPositionsCount.toString(),
                                    color = if (openPositionsCount > 0) CosmicTeal else OffWhite,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Box(modifier = Modifier.width(1.dp).height(30.dp).background(SlateCard))
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("Margin Status", color = MutedSteel, fontSize = 11.sp)
                                Text(
                                    text = if (balance > 0) "HEALTHY" else "MARGIN CALL",
                                    color = if (balance > 0) TradeProfit else TradeLoss,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }

            // --- 3. Expandable OANDA Credentials Form ---
            if (showConnectionForm && !isPaperMode) {
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = SlateDark),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "OANDA ACCOUNT LOGIN",
                                    color = OffWhite,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                                IconButton(onClick = { showConnectionForm = false }) {
                                    Icon(Icons.Default.Close, contentDescription = "Close", tint = MutedSteel)
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            OutlinedTextField(
                                value = oandaTokenInput,
                                onValueChange = { oandaTokenInput = it },
                                label = { Text("OANDA API Personal Token") },
                                leadingIcon = { Icon(Icons.Default.Key, contentDescription = null, tint = CosmicTeal) },
                                colors = OutlinedTextFieldDefaults.colors(
                                    unfocusedBorderColor = SlateCard,
                                    focusedBorderColor = CosmicTeal,
                                    focusedLabelColor = CosmicTeal,
                                    focusedTextColor = OffWhite,
                                    unfocusedTextColor = OffWhite
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("oanda_token_field")
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            OutlinedTextField(
                                value = oandaAccountIdInput,
                                onValueChange = { oandaAccountIdInput = it },
                                label = { Text("OANDA Account ID") },
                                leadingIcon = { Icon(Icons.Default.AccountBox, contentDescription = null, tint = CosmicTeal) },
                                colors = OutlinedTextFieldDefaults.colors(
                                    unfocusedBorderColor = SlateCard,
                                    focusedBorderColor = CosmicTeal,
                                    focusedLabelColor = CosmicTeal,
                                    focusedTextColor = OffWhite,
                                    unfocusedTextColor = OffWhite
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("oanda_account_id_field")
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            Text("Environment Mode", color = MutedSteel, fontSize = 12.sp)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                listOf("sandbox" to "Sandbox", "practice" to "Demo/Practice", "live" to "Live").forEach { (id, label) ->
                                    val isSelected = selectedEnv == id
                                    FilterChip(
                                        selected = isSelected,
                                        onClick = { selectedEnv = id },
                                        label = { Text(label) },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = CosmicTeal.copy(alpha = 0.2f),
                                            selectedLabelColor = CosmicTeal
                                        ),
                                        modifier = Modifier.testTag("env_chip_$id")
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            Button(
                                onClick = {
                                    if (oandaTokenInput.isBlank() || oandaAccountIdInput.isBlank()) {
                                        scope.launch { snackbarHostState.showSnackbar("Token and Account ID cannot be blank.") }
                                        return@Button
                                    }
                                    viewModel.connectOanda(
                                        token = oandaTokenInput,
                                        accountId = oandaAccountIdInput,
                                        environment = selectedEnv
                                    ) { res ->
                                        res.onSuccess {
                                            showConnectionForm = false
                                            scope.launch { snackbarHostState.showSnackbar("Successfully connected to OANDA!") }
                                        }.onFailure { err ->
                                            scope.launch { snackbarHostState.showSnackbar("OANDA Login Failed: ${err.localizedMessage}") }
                                        }
                                    }
                                },
                                enabled = !isLoading,
                                colors = ButtonDefaults.buttonColors(containerColor = CosmicTeal),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                                    .testTag("connect_oanda_button")
                            ) {
                                if (isLoading) {
                                    CircularProgressIndicator(color = OffWhite, modifier = Modifier.size(24.dp))
                                } else {
                                    Text("Authorize & Sync Accounts", fontWeight = FontWeight.Bold, color = SpaceBlack)
                                }
                            }
                        }
                    }
                }
            }

            // --- 4. Live Quote & Interactive Order Panel ---
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = SlateDark),
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "LIVE ORDER BOOK & EXECUTION TICKET",
                            color = MutedSteel,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.2.sp,
                            modifier = Modifier.padding(bottom = 12.dp)
                        )

                        // Risk lock warnings block execution
                        if (activeLocks.isNotEmpty()) {
                            Surface(
                                color = TradeLoss.copy(alpha = 0.15f),
                                border = BorderStroke(1.dp, TradeLoss),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 12.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.Lock, contentDescription = null, tint = TradeLoss)
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Text(
                                            "EXECUTION LOCKED",
                                            color = TradeLoss,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp
                                        )
                                        Text(
                                            "A risk lock event is active. Direct broker executions are disabled to protect capital.",
                                            color = OffWhite,
                                            fontSize = 10.sp
                                        )
                                    }
                                }
                            }
                        }

                        // Live Ticker Selector Grid
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf("EUR/USD", "GBP/USD", "USD/JPY", "XAU/USD").forEach { symbol ->
                                val price = livePrices[symbol] ?: 1.0
                                val isSelected = selectedAsset == symbol
                                
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(if (isSelected) SlateCard else SpaceBlack)
                                        .clickable { selectedAsset = symbol }
                                        .border(
                                            width = 1.dp,
                                            color = if (isSelected) CosmicTeal else Color.Transparent,
                                            shape = RoundedCornerShape(12.dp)
                                        )
                                        .padding(8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text(
                                            text = symbol,
                                            color = if (isSelected) CosmicTeal else MutedSteel,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = if (symbol.contains("JPY")) String.format(Locale.US, "%.3f", price)
                                                   else if (symbol.contains("XAU")) String.format(Locale.US, "%.2f", price)
                                                   else String.format(Locale.US, "%.5f", price),
                                            color = OffWhite,
                                            fontSize = 11.sp,
                                            fontFamily = FontFamily.Monospace,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Input fields
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = lotSizeInput,
                                onValueChange = { lotSizeInput = it },
                                label = { Text("Lot Size (Lots)") },
                                placeholder = { Text("0.10") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                colors = OutlinedTextFieldDefaults.colors(
                                    unfocusedBorderColor = SlateCard,
                                    focusedBorderColor = CosmicTeal,
                                    focusedLabelColor = CosmicTeal,
                                    focusedTextColor = OffWhite,
                                    unfocusedTextColor = OffWhite
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("order_lot_size")
                            )

                            Column(modifier = Modifier.weight(1.0f)) {
                                Text("Market Price", color = MutedSteel, fontSize = 11.sp)
                                Text(
                                    text = if (selectedAsset.contains("JPY")) String.format(Locale.US, "%.3f", currentPrice)
                                           else if (selectedAsset.contains("XAU")) String.format(Locale.US, "%.2f", currentPrice)
                                           else String.format(Locale.US, "%.5f", currentPrice),
                                    color = OffWhite,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        // Large Action Executions
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Button(
                                onClick = {
                                    val size = lotSizeInput.toDoubleOrNull() ?: 0.1
                                    viewModel.placeBrokerTrade(selectedAsset, "LONG", size, currentPrice) { res ->
                                        res.onSuccess { msg ->
                                            executionSuccessMsg = msg
                                            executionErrorMsg = null
                                        }.onFailure { err ->
                                            executionErrorMsg = err.localizedMessage
                                            executionSuccessMsg = null
                                        }
                                    }
                                },
                                enabled = activeLocks.isEmpty() && !isLoading,
                                colors = ButtonDefaults.buttonColors(containerColor = TradeProfit),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(54.dp)
                                    .testTag("place_buy_order")
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("BUY (LONG)", color = SpaceBlack, fontWeight = FontWeight.Black, fontSize = 14.sp)
                                    Text("MARKET EXECUTION", color = SpaceBlack.copy(alpha = 0.7f), fontSize = 9.sp)
                                }
                            }

                            Button(
                                onClick = {
                                    val size = lotSizeInput.toDoubleOrNull() ?: 0.1
                                    viewModel.placeBrokerTrade(selectedAsset, "SHORT", size, currentPrice) { res ->
                                        res.onSuccess { msg ->
                                            executionSuccessMsg = msg
                                            executionErrorMsg = null
                                        }.onFailure { err ->
                                            executionErrorMsg = err.localizedMessage
                                            executionSuccessMsg = null
                                        }
                                    }
                                },
                                enabled = activeLocks.isEmpty() && !isLoading,
                                colors = ButtonDefaults.buttonColors(containerColor = TradeLoss),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(54.dp)
                                    .testTag("place_sell_order")
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("SELL (SHORT)", color = OffWhite, fontWeight = FontWeight.Black, fontSize = 14.sp)
                                    Text("MARKET EXECUTION", color = OffWhite.copy(alpha = 0.7f), fontSize = 9.sp)
                                }
                            }
                        }

                        // Success/Failure feedback overlays inside card
                        AnimatedVisibility(visible = executionSuccessMsg != null) {
                            executionSuccessMsg?.let { msg ->
                                Surface(
                                    color = TradeProfit.copy(alpha = 0.15f),
                                    border = BorderStroke(1.dp, TradeProfit),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 12.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = TradeProfit)
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text("ORDER FILLED SUCCESSFULLY", color = TradeProfit, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                            Text(msg, color = OffWhite, fontSize = 11.sp)
                                        }
                                        IconButton(onClick = { executionSuccessMsg = null }) {
                                            Icon(Icons.Default.Close, contentDescription = null, tint = MutedSteel)
                                        }
                                    }
                                }
                            }
                        }

                        AnimatedVisibility(visible = executionErrorMsg != null) {
                            executionErrorMsg?.let { msg ->
                                Surface(
                                    color = TradeLoss.copy(alpha = 0.15f),
                                    border = BorderStroke(1.dp, TradeLoss),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 12.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(Icons.Default.Error, contentDescription = null, tint = TradeLoss)
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text("EXECUTION REJECTED / FAILURE", color = TradeLoss, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                            Text(msg, color = OffWhite, fontSize = 11.sp)
                                        }
                                        IconButton(onClick = { executionErrorMsg = null }) {
                                            Icon(Icons.Default.Close, contentDescription = null, tint = MutedSteel)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // --- 5. Open Positions / Realized Position Table ---
            item {
                Text(
                    text = "ACTIVE OPEN POSITIONS (${if (isPaperMode) paperPositions.size else openPositionsCount})",
                    color = MutedSteel,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                    letterSpacing = 1.2.sp,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            if (isPaperMode) {
                if (paperPositions.isEmpty()) {
                    item {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = SlateDark),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(24.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "No open positions. Use the Execution Ticket above to place a Paper market order.",
                                    color = MutedSteel,
                                    fontSize = 12.sp,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                } else {
                    itemsIndexed(paperPositions) { index, pos ->
                        val assetPrice = livePrices[pos.instrument] ?: pos.entryPrice
                        val pnl = if (pos.direction == "LONG") {
                            (assetPrice - pos.entryPrice) * pos.units * (if (pos.instrument.contains("JPY")) 0.01 else if (pos.instrument.contains("XAU")) 1.0 else 1.0) * 0.1
                        } else {
                            (pos.entryPrice - assetPrice) * pos.units * (if (pos.instrument.contains("JPY")) 0.01 else if (pos.instrument.contains("XAU")) 1.0 else 1.0) * 0.1
                        }

                        Card(
                            colors = CardDefaults.cardColors(containerColor = SlateCard),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("paper_position_item_$index")
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Surface(
                                            color = if (pos.direction == "LONG") TradeProfit.copy(alpha = 0.2f) else TradeLoss.copy(alpha = 0.2f),
                                            shape = RoundedCornerShape(4.dp)
                                        ) {
                                            Text(
                                                text = pos.direction,
                                                color = if (pos.direction == "LONG") TradeProfit else TradeLoss,
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = pos.instrument,
                                            color = OffWhite,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "Entry: ${String.format(Locale.US, "%.5f", pos.entryPrice)} | Size: ${String.format(Locale.US, "%.2f", pos.units / 100000.0)} Lots",
                                        color = MutedSteel,
                                        fontSize = 11.sp
                                    )
                                }

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Column(
                                        horizontalAlignment = Alignment.End,
                                        modifier = Modifier.padding(end = 12.dp)
                                    ) {
                                        Text(
                                            text = String.format(Locale.US, "$%.2f", pnl),
                                            color = if (pnl >= 0) TradeProfit else TradeLoss,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp,
                                            fontFamily = FontFamily.Monospace
                                        )
                                        Text(
                                            text = "Float P&L",
                                            color = MutedSteel,
                                            fontSize = 9.sp
                                        )
                                    }

                                    Button(
                                        onClick = { viewModel.closePaperPositionByIndex(index, assetPrice) },
                                        colors = ButtonDefaults.buttonColors(containerColor = SlateDark),
                                        shape = RoundedCornerShape(8.dp),
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                        modifier = Modifier.height(32.dp)
                                    ) {
                                        Text("Close", color = OffWhite, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = SlateDark),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Default.CloudSync, contentDescription = null, tint = CosmicTeal, modifier = Modifier.size(32.dp))
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "OANDA live position queries are updated in real-time on your OANDA account dashboard.",
                                    color = MutedSteel,
                                    fontSize = 11.sp,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}
