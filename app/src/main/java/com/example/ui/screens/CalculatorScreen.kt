@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class, androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.JournalViewModel
import com.example.ui.theme.*
import java.util.*
import kotlin.math.abs

@Composable
fun CalculatorScreen(
    viewModel: JournalViewModel,
    onNavigateToLogTradeWithParams: (String, String, Double, Double, Double) -> Unit,
    onBack: () -> Unit
) {
    val plan by viewModel.tradingPlan.collectAsState()

    var balanceInput by remember { mutableStateOf("") }
    var riskInput by remember { mutableStateOf("") }
    var entryPriceInput by remember { mutableStateOf("") }
    var stopLossInput by remember { mutableStateOf("") }
    var direction by remember { mutableStateOf("LONG") } // "LONG" or "SHORT"
    var instrument by remember { mutableStateOf("Forex") } // "Forex", "Gold (XAU)", "Indices", "Crypto"

    // Seed defaults from plan
    LaunchedEffect(plan) {
        if (balanceInput.isEmpty()) {
            balanceInput = plan.accountBalance.toString()
        }
        if (riskInput.isEmpty()) {
            riskInput = plan.riskPerTradePercent.toString()
        }
    }

    val balance = balanceInput.toDoubleOrNull() ?: 0.0
    val riskPercent = riskInput.toDoubleOrNull() ?: 0.0
    val entryPrice = entryPriceInput.toDoubleOrNull() ?: 0.0
    val stopLoss = stopLossInput.toDoubleOrNull() ?: 0.0

    // Calculations
    val riskAmount = balance * (riskPercent / 100.0)
    val stopDistance = abs(entryPrice - stopLoss)

    // Validations
    var isStopLossValid = true
    var validationError = ""

    if (entryPrice > 0.0 && stopLoss > 0.0) {
        if (direction == "LONG" && stopLoss >= entryPrice) {
            isStopLossValid = false
            validationError = "Stop Loss must be below Entry Price for Long positions."
        } else if (direction == "SHORT" && stopLoss <= entryPrice) {
            isStopLossValid = false
            validationError = "Stop Loss must be above Entry Price for Short positions."
        }
    }

    val calculatedLotSize = if (isStopLossValid && entryPrice > 0.0 && stopLoss > 0.0 && riskAmount > 0.0) {
        viewModel.calculatePositionSize(
            balance = balance,
            riskPercent = riskPercent,
            entryPrice = entryPrice,
            stopLoss = stopLoss,
            instrument = instrument,
            direction = direction
        )
    } else 0.0

    Scaffold(
        topBar = {
            OptIn(ExperimentalMaterial3Api::class)
            TopAppBar(
                title = { Text("POSITION SIZE CALCULATOR", fontWeight = FontWeight.Bold, fontSize = 16.sp, letterSpacing = 1.sp) },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("calculator_back_button")) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = SlateDark,
                    titleContentColor = OffWhite,
                    navigationIconContentColor = CosmicTeal
                )
            )
        },
        containerColor = SpaceBlack
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(SpaceBlack)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Section 1: Direction Selection
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = { direction = "LONG" },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (direction == "LONG") TradeProfit else SlateDark
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("direction_long_button")
                ) {
                    Icon(Icons.Default.TrendingUp, contentDescription = null, tint = if (direction == "LONG") SpaceBlack else TradeProfit)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("LONG / BUY", color = if (direction == "LONG") SpaceBlack else OffWhite, fontWeight = FontWeight.Bold)
                }

                Button(
                    onClick = { direction = "SHORT" },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (direction == "SHORT") TradeLoss else SlateDark
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("direction_short_button")
                ) {
                    Icon(Icons.Default.TrendingDown, contentDescription = null, tint = if (direction == "SHORT") SpaceBlack else TradeLoss)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("SHORT / SELL", color = if (direction == "SHORT") SpaceBlack else OffWhite, fontWeight = FontWeight.Bold)
                }
            }

            // Section 2: Instrument Selection
            Card(
                colors = CardDefaults.cardColors(containerColor = SlateDark),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "INSTRUMENT FAMILY",
                        color = MutedSteel,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        letterSpacing = 1.sp,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("Forex", "Gold", "Indices", "Crypto").forEach { inst ->
                            val selected = instrument == inst
                            Surface(
                                color = if (selected) CosmicTeal else SlateCard,
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { instrument = inst }
                                    .testTag("instrument_chip_$inst")
                            ) {
                                Text(
                                    text = inst,
                                    color = if (selected) SpaceBlack else OffWhite,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp,
                                    modifier = Modifier.padding(vertical = 8.dp, horizontal = 4.dp),
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                )
                            }
                        }
                    }
                }
            }

            // Section 3: Parameters Input
            Card(
                colors = CardDefaults.cardColors(containerColor = SlateDark),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "PLANNING METRICS",
                        color = MutedSteel,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        letterSpacing = 1.sp
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedTextField(
                            value = balanceInput,
                            onValueChange = { balanceInput = it },
                            label = { Text("Account Balance ($)") },
                            modifier = Modifier
                                .weight(1.2f)
                                .testTag("calculator_balance_input"),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = CosmicTeal,
                                unfocusedBorderColor = SlateCard,
                                focusedTextColor = OffWhite,
                                unfocusedTextColor = OffWhite,
                                focusedLabelColor = CosmicTeal,
                                unfocusedLabelColor = MutedSteel
                            )
                        )

                        OutlinedTextField(
                            value = riskInput,
                            onValueChange = { riskInput = it },
                            label = { Text("Risk %") },
                            modifier = Modifier
                                .weight(0.8f)
                                .testTag("calculator_risk_input"),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = CosmicTeal,
                                unfocusedBorderColor = SlateCard,
                                focusedTextColor = OffWhite,
                                unfocusedTextColor = OffWhite,
                                focusedLabelColor = CosmicTeal,
                                unfocusedLabelColor = MutedSteel
                            )
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedTextField(
                            value = entryPriceInput,
                            onValueChange = { entryPriceInput = it },
                            label = { Text("Entry Price") },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("calculator_entry_price_input"),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = CosmicBlue,
                                unfocusedBorderColor = SlateCard,
                                focusedTextColor = OffWhite,
                                unfocusedTextColor = OffWhite,
                                focusedLabelColor = CosmicBlue,
                                unfocusedLabelColor = MutedSteel
                            )
                        )

                        OutlinedTextField(
                            value = stopLossInput,
                            onValueChange = { stopLossInput = it },
                            label = { Text("Stop Loss") },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("calculator_stop_loss_input"),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = CosmicPurple,
                                unfocusedBorderColor = SlateCard,
                                focusedTextColor = OffWhite,
                                unfocusedTextColor = OffWhite,
                                focusedLabelColor = CosmicPurple,
                                unfocusedLabelColor = MutedSteel
                            )
                        )
                    }

                    if (!isStopLossValid) {
                        Text(
                            text = validationError,
                            color = TradeLoss,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
            }

            // Section 4: Output Metrics Display
            Card(
                colors = CardDefaults.cardColors(containerColor = SlateDark),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        text = "CALCULATED LOT ALLOCATION",
                        color = MutedSteel,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        letterSpacing = 1.2.sp,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "RECOMMENDED POSITION",
                                color = MutedSteel,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = String.format(Locale.US, "%.2f Lots", calculatedLotSize),
                                color = if (calculatedLotSize > 0.0) CosmicTeal else MutedSteel,
                                fontSize = 28.sp,
                                fontWeight = FontWeight.Black
                            )
                        }

                        Icon(
                            Icons.Default.VerifiedUser,
                            contentDescription = null,
                            tint = if (calculatedLotSize > 0.0) CosmicTeal else MutedSteel,
                            modifier = Modifier.size(48.dp)
                        )
                    }

                    Divider(color = SlateCard, modifier = Modifier.padding(vertical = 16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(text = "TOTAL RISK AMOUNT", color = MutedSteel, fontSize = 11.sp)
                            Text(text = String.format(Locale.US, "$%.2f", riskAmount), color = OffWhite, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Text(text = "STOP DISTANCE", color = MutedSteel, fontSize = 11.sp)
                            Text(
                                text = if (stopDistance > 0.0) String.format(Locale.US, "%.5f", stopDistance) else "--",
                                color = OffWhite,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                        }
                    }
                }
            }

            // Section 5: Log Trade Forward Button
            Button(
                onClick = {
                    onNavigateToLogTradeWithParams(
                        instrument,
                        direction,
                        entryPrice,
                        stopLoss,
                        calculatedLotSize
                    )
                },
                enabled = calculatedLotSize > 0.0 && isStopLossValid,
                colors = ButtonDefaults.buttonColors(
                    containerColor = CosmicTeal,
                    contentColor = SpaceBlack
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("calculator_log_trade_button")
            ) {
                Icon(Icons.Default.Assignment, contentDescription = null, tint = SpaceBlack)
                Spacer(modifier = Modifier.width(8.dp))
                Text("LOG THIS TRADE INTO JOURNAL", fontWeight = FontWeight.Bold, fontSize = 14.sp)
            }
        }
    }
}
