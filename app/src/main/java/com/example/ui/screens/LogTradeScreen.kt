@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class, androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.ui.graphics.Color
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.JournalViewModel
import com.example.ui.theme.*
import java.util.*

@Composable
fun LogTradeScreen(
    viewModel: JournalViewModel,
    prefilledPair: String? = null,
    prefilledDirection: String? = null,
    prefilledEntry: Double? = null,
    prefilledSl: Double? = null,
    prefilledLot: Double? = null,
    onSuccess: () -> Unit,
    onBack: () -> Unit
) {
    var pairInput by remember { mutableStateOf(prefilledPair ?: "EURUSD") }
    var direction by remember { mutableStateOf(prefilledDirection ?: "LONG") }
    var entryPriceInput by remember { mutableStateOf(prefilledEntry?.toString() ?: "") }
    var stopLossInput by remember { mutableStateOf(prefilledSl?.toString() ?: "") }
    var takeProfitInput by remember { mutableStateOf("") }
    var lotSizeInput by remember { mutableStateOf(prefilledLot?.toString() ?: "0.1") }
    var session by remember { mutableStateOf("New York") }

    // Checklist states
    var htfBias by remember { mutableStateOf(false) }
    var liquiditySweep by remember { mutableStateOf(false) }
    var bos by remember { mutableStateOf(false) }
    var choch by remember { mutableStateOf(false) }
    var orderBlock by remember { mutableStateOf(false) }
    var fvg by remember { mutableStateOf(false) }
    var mitigationBlock by remember { mutableStateOf(false) }
    var premiumZone by remember { mutableStateOf(false) }
    var discountZone by remember { mutableStateOf(false) }
    var sessionAlignment by remember { mutableStateOf(false) }

    // Dynamic Score Calculation
    val checklistScore = listOf(
        htfBias, liquiditySweep, bos, choch, orderBlock, fvg,
        mitigationBlock, premiumZone, discountZone, sessionAlignment
    ).count { it }

    val grade = when {
        checklistScore >= 8 -> "A+ (Premium)"
        checklistScore >= 6 -> "B (Consistent)"
        checklistScore >= 4 -> "C (Developing)"
        else -> "Poor Setup"
    }

    val gradeColor = when {
        checklistScore >= 8 -> CosmicTeal
        checklistScore >= 6 -> CosmicBlue
        checklistScore >= 4 -> TradeWarning
        else -> TradeLoss
    }

    Scaffold(
        topBar = {
            OptIn(ExperimentalMaterial3Api::class)
            TopAppBar(
                title = { Text("LOG NEW TRADE JOURNAL", fontWeight = FontWeight.Bold, fontSize = 16.sp, letterSpacing = 1.sp) },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("log_trade_back_button")) {
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
            // Section 1: Core Parameters Card
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
                        text = "TRADE PARAMETERS",
                        color = MutedSteel,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        letterSpacing = 1.sp
                    )

                    // Pair & Direction row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = pairInput,
                            onValueChange = { pairInput = it.uppercase() },
                            label = { Text("Pair / Asset") },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("log_trade_pair_input"),
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

                        // Segmented control style for Direction
                        Row(
                            modifier = Modifier
                                .weight(1f)
                                .height(56.dp)
                                .background(SlateCard, RoundedCornerShape(8.dp))
                                .padding(4.dp),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .weight(1f)
                                    .background(
                                        if (direction == "LONG") TradeProfit else Color.Transparent,
                                        RoundedCornerShape(6.dp)
                                    )
                                    .clickable { direction = "LONG" }
                                    .testTag("direction_long_chip"),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "LONG",
                                    color = if (direction == "LONG") SpaceBlack else OffWhite,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .weight(1f)
                                    .background(
                                        if (direction == "SHORT") TradeLoss else Color.Transparent,
                                        RoundedCornerShape(6.dp)
                                    )
                                    .clickable { direction = "SHORT" }
                                    .testTag("direction_short_chip"),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "SHORT",
                                    color = if (direction == "SHORT") SpaceBlack else OffWhite,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }

                    // Prices row
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
                                .testTag("log_trade_entry_input"),
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
                                .testTag("log_trade_sl_input"),
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

                    // Take Profit & Lot size row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedTextField(
                            value = takeProfitInput,
                            onValueChange = { takeProfitInput = it },
                            label = { Text("Take Profit") },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("log_trade_tp_input"),
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
                            value = lotSizeInput,
                            onValueChange = { lotSizeInput = it },
                            label = { Text("Lot Size") },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("log_trade_lot_input"),
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

                    // Session dropdown / select row
                    Column {
                        Text("Trading Session", color = MutedSteel, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf("New York", "London", "Asian", "Overlap").forEach { s ->
                                val selected = session == s
                                Surface(
                                    color = if (selected) CosmicTeal else SlateCard,
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable { session = s }
                                        .testTag("session_chip_$s")
                                ) {
                                    Text(
                                        text = s,
                                        color = if (selected) SpaceBlack else OffWhite,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp,
                                        modifier = Modifier.padding(vertical = 8.dp),
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Section 2: SMC Checklist Card
            Card(
                colors = CardDefaults.cardColors(containerColor = SlateDark),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    // Header with dynamic Setup Score
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(text = "SMC PRE-TRADE CHECKLIST", color = MutedSteel, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                            Text(text = "Market Structure Confluences", color = OffWhite, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                        }

                        // Score Badge
                        Surface(
                            color = gradeColor.copy(alpha = 0.15f),
                            border = BorderStroke(1.dp, gradeColor),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = "$checklistScore/10 - $grade",
                                color = gradeColor,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    Text("Evaluate structural confluences before pressing enter:", color = MutedSteel, fontSize = 11.sp)
                    Spacer(modifier = Modifier.height(12.dp))

                    // Checklist grid columns
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        SmcChecklistRow(label = "Higher Timeframe Bias Aligned", checked = htfBias, onCheckedChange = { htfBias = it }, testTag = "chk_htf_bias")
                        SmcChecklistRow(label = "Liquidity Sweep Identified", checked = liquiditySweep, onCheckedChange = { liquiditySweep = it }, testTag = "chk_liquidity_sweep")
                        SmcChecklistRow(label = "Break of Structure (BOS) Confirmed", checked = bos, onCheckedChange = { bos = it }, testTag = "chk_bos")
                        SmcChecklistRow(label = "Change of Character (CHOCH) Detected", checked = choch, onCheckedChange = { choch = it }, testTag = "chk_choch")
                        SmcChecklistRow(label = "Valid Order Block (OB) Mapped", checked = orderBlock, onCheckedChange = { orderBlock = it }, testTag = "chk_order_block")
                        SmcChecklistRow(label = "Fair Value Gap (FVG) Open", checked = fvg, onCheckedChange = { fvg = it }, testTag = "chk_fvg")
                        SmcChecklistRow(label = "Mitigation / Breaker Block Zone", checked = mitigationBlock, onCheckedChange = { mitigationBlock = it }, testTag = "chk_mitigation_block")
                        SmcChecklistRow(label = "Premium Price Range (Short Entry)", checked = premiumZone, onCheckedChange = { premiumZone = it }, testTag = "chk_premium_zone")
                        SmcChecklistRow(label = "Discount Price Range (Long Entry)", checked = discountZone, onCheckedChange = { discountZone = it }, testTag = "chk_discount_zone")
                        SmcChecklistRow(label = "Prime Trading Session Window", checked = sessionAlignment, onCheckedChange = { sessionAlignment = it }, testTag = "chk_session_alignment")
                    }
                }
            }

            // Action submit button
            val isFormComplete = pairInput.isNotEmpty() &&
                    entryPriceInput.toDoubleOrNull() != null &&
                    stopLossInput.toDoubleOrNull() != null &&
                    lotSizeInput.toDoubleOrNull() != null

            Button(
                onClick = {
                    val entryPrice = entryPriceInput.toDoubleOrNull() ?: 0.0
                    val stopLoss = stopLossInput.toDoubleOrNull() ?: 0.0
                    val takeProfit = takeProfitInput.toDoubleOrNull()
                    val lotSize = lotSizeInput.toDoubleOrNull() ?: 0.1

                    viewModel.logTrade(
                        pair = pairInput,
                        direction = direction,
                        entryPrice = entryPrice,
                        stopLoss = stopLoss,
                        takeProfit = takeProfit,
                        lotSize = lotSize,
                        session = session,
                        checklistHtfBias = htfBias,
                        checklistLiquiditySweep = liquiditySweep,
                        checklistBos = bos,
                        checklistChoch = choch,
                        checklistOrderBlock = orderBlock,
                        checklistFvg = fvg,
                        checklistMitigationBlock = mitigationBlock,
                        checklistPremiumZone = premiumZone,
                        checklistDiscountZone = discountZone,
                        checklistSessionAlignment = sessionAlignment
                    )
                    onSuccess()
                },
                enabled = isFormComplete,
                colors = ButtonDefaults.buttonColors(containerColor = CosmicTeal, contentColor = SpaceBlack),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("submit_log_trade_button")
            ) {
                Icon(Icons.Default.Check, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("SAVE LIVE OPEN TRADE", fontWeight = FontWeight.Bold, fontSize = 14.sp)
            }
        }
    }
}

@Composable
fun SmcChecklistRow(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    testTag: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(
            checked = checked,
            onCheckedChange = onCheckedChange,
            modifier = Modifier.testTag(testTag),
            colors = CheckboxDefaults.colors(
                checkedColor = CosmicTeal,
                uncheckedColor = SlateCard
            )
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(text = label, color = if (checked) OffWhite else MutedSteel, fontSize = 13.sp)
    }
}
