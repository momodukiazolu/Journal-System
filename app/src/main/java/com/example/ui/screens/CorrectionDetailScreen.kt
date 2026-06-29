@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class, androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.TradeEntity
import com.example.ui.JournalViewModel
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CorrectionDetailScreen(
    viewModel: JournalViewModel,
    tradeId: Int,
    onSuccess: () -> Unit,
    onBack: () -> Unit
) {
    val trades by viewModel.trades.collectAsState()
    val trade = remember(trades, tradeId) { trades.firstOrNull { it.id == tradeId } }

    if (trade == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = CosmicTeal)
        }
        return
    }

    // Exit logging states (used if OPEN)
    var exitPriceInput by remember { mutableStateOf("") }
    var notesInput by remember { mutableStateOf("") }
    
    // Execution Questions
    var qHtf by remember { mutableStateOf(true) }
    var qLiquidity by remember { mutableStateOf(true) }
    var qConfirmed by remember { mutableStateOf(true) }
    var qRisk by remember { mutableStateOf(true) }
    var qEmotionSl by remember { mutableStateOf(false) }
    var qOvertrade by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            OptIn(ExperimentalMaterial3Api::class)
            TopAppBar(
                title = { Text("TRADE DETAILS & CORRECTION", fontWeight = FontWeight.Bold, fontSize = 16.sp, letterSpacing = 1.sp) },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("detail_back_button")) {
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
            // Section 1: Trade Header (Pair, Status, Direction)
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
                        Column {
                            Text(
                                text = trade.pair,
                                color = OffWhite,
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Black
                            )
                            val dateStr = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date(trade.date))
                            Text(text = "Logged $dateStr", color = MutedSteel, fontSize = 11.sp)
                        }

                        Surface(
                            color = if (trade.status == "OPEN") CosmicBlue.copy(alpha = 0.15f) else TradeProfit.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, if (trade.status == "OPEN") CosmicBlue else TradeProfit)
                        ) {
                            Text(
                                text = trade.status,
                                color = if (trade.status == "OPEN") CosmicBlue else TradeProfit,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Details grid
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("DIRECTION", color = MutedSteel, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            Text(
                                text = trade.direction,
                                color = if (trade.direction == "LONG") TradeProfit else TradeLoss,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        }

                        Column {
                            Text("ENTRY / LOTS", color = MutedSteel, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            Text("${trade.entryPrice} (${trade.lotSize} Lots)", color = OffWhite, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Text("STOP LOSS", color = MutedSteel, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            Text(trade.stopLoss.toString(), color = OffWhite, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }
                    }
                }
            }

            // Section 2: If OPEN, render CLOSE PANEL
            if (trade.status == "OPEN") {
                Card(
                    colors = CardDefaults.cardColors(containerColor = SlateDark),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, CosmicTeal),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "CLOSE POSITION & BEHAVIORAL REVIEW",
                            color = CosmicTeal,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            letterSpacing = 1.sp
                        )

                        OutlinedTextField(
                            value = exitPriceInput,
                            onValueChange = { exitPriceInput = it },
                            label = { Text("Exit Price") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("exit_price_input"),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = CosmicTeal,
                                unfocusedBorderColor = SlateCard,
                                focusedTextColor = OffWhite,
                                unfocusedTextColor = OffWhite
                            )
                        )

                        OutlinedTextField(
                            value = notesInput,
                            onValueChange = { notesInput = it },
                            label = { Text("Execution notes / insights") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(80.dp)
                                .testTag("exit_notes_input"),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = CosmicTeal,
                                unfocusedBorderColor = SlateCard,
                                focusedTextColor = OffWhite,
                                unfocusedTextColor = OffWhite
                            )
                        )

                        Divider(color = SlateCard, modifier = Modifier.padding(vertical = 4.dp))

                        Text(
                            text = "EXECUTION PROCESS CONFORMITY",
                            color = MutedSteel,
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.sp,
                            letterSpacing = 1.sp
                        )

                        QuestionSwitchRow(label = "Did I follow HTF Bias guidelines?", checked = qHtf, onCheckedChange = { qHtf = it }, testTag = "q_htf")
                        QuestionSwitchRow(label = "Did I wait for liquidity sweep?", checked = qLiquidity, onCheckedChange = { qLiquidity = it }, testTag = "q_liquidity")
                        QuestionSwitchRow(label = "Was entry confirmation met?", checked = qConfirmed, onCheckedChange = { qConfirmed = it }, testTag = "q_confirmed")
                        QuestionSwitchRow(label = "Did I follow risk/lot limit parameters?", checked = qRisk, onCheckedChange = { qRisk = it }, testTag = "q_risk")
                        QuestionSwitchRow(label = "Did I move SL emotionally during live hold?", checked = qEmotionSl, onCheckedChange = { qEmotionSl = it }, testTag = "q_emotion_sl")
                        QuestionSwitchRow(label = "Did I overtrade or bypass plan?", checked = qOvertrade, onCheckedChange = { qOvertrade = it }, testTag = "q_overtrade")

                        Button(
                            onClick = {
                                val exitPrice = exitPriceInput.toDoubleOrNull() ?: 0.0
                                viewModel.closeTrade(
                                    tradeId = trade.id,
                                    exitPrice = exitPrice,
                                    notes = notesInput,
                                    qFollowedHtfBias = qHtf,
                                    qWaitLiquidity = qLiquidity,
                                    qEntryConfirmed = qConfirmed,
                                    qFollowedRiskRules = qRisk,
                                    qMoveStopLossEmotionally = qEmotionSl,
                                    qOvertrade = qOvertrade
                                )
                                onSuccess()
                            },
                            enabled = exitPriceInput.toDoubleOrNull() != null,
                            colors = ButtonDefaults.buttonColors(containerColor = CosmicTeal, contentColor = SpaceBlack),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("close_position_submit_button")
                        ) {
                            Text("SAVE EXIT & TRIGGER CORRECTION ENGINE", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }
                }
            } else {
                // Section 3: Closed position metrics (Realized P&L & Mistakes)
                val pnl = trade.profitLoss ?: 0.0
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
                            Column {
                                Text("REALIZED P&L OUTCOME", color = MutedSteel, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                Text(
                                    text = String.format(Locale.US, "%s$%.2f", if (pnl >= 0.0) "+" else "", pnl),
                                    color = if (pnl >= 0.0) TradeProfit else TradeLoss,
                                    fontSize = 24.sp,
                                    fontWeight = FontWeight.Black
                                )
                            }
                            
                            Column(horizontalAlignment = Alignment.End) {
                                Text("EXIT PRICE", color = MutedSteel, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                Text(trade.exitPrice.toString(), color = OffWhite, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        // Mistake tags
                        val tags = trade.mistakeTags.split(",").filter { it.isNotEmpty() }
                        if (tags.isNotEmpty()) {
                            Divider(color = SlateCard, modifier = Modifier.padding(vertical = 12.dp))
                            Text(text = "DETECTED PROCESS VIOLATIONS", color = TradeLoss, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                            Spacer(modifier = Modifier.height(8.dp))
                            FlowRow(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                tags.forEach { tag ->
                                    Surface(
                                        color = TradeLoss.copy(alpha = 0.15f),
                                        shape = RoundedCornerShape(8.dp),
                                        border = BorderStroke(1.dp, TradeLoss)
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(Icons.Default.Info, contentDescription = null, tint = TradeLoss, modifier = Modifier.size(14.dp))
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(text = tag, color = TradeLoss, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Section 4: AI Feedback Card
                Card(
                    colors = CardDefaults.cardColors(containerColor = SlateDark),
                    shape = RoundedCornerShape(20.dp),
                    border = BorderStroke(1.dp, CosmicTeal),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "COSMIC AI REVIEW ENGINE",
                                color = CosmicTeal,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.2.sp
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Strengths
                        Text(text = "PROCESS STRENGTHS", color = TradeProfit, fontWeight = FontWeight.Bold, fontSize = 11.sp, letterSpacing = 1.sp)
                        Spacer(modifier = Modifier.height(6.dp))
                        trade.aiStrengths.split("\n").filter { it.isNotEmpty() }.forEach { bullet ->
                            Text(text = "• $bullet", color = OffWhite, fontSize = 13.sp, modifier = Modifier.padding(bottom = 6.dp))
                        }

                        Divider(color = SlateCard, modifier = Modifier.padding(vertical = 12.dp))

                        // Weaknesses
                        Text(text = "PROCESS WEAKNESSES", color = TradeLoss, fontWeight = FontWeight.Bold, fontSize = 11.sp, letterSpacing = 1.sp)
                        Spacer(modifier = Modifier.height(6.dp))
                        trade.aiWeaknesses.split("\n").filter { it.isNotEmpty() }.forEach { bullet ->
                            Text(text = "• $bullet", color = OffWhite, fontSize = 13.sp, modifier = Modifier.padding(bottom = 6.dp))
                        }

                        Divider(color = SlateCard, modifier = Modifier.padding(vertical = 12.dp))

                        // Recommendation
                        Text(text = "CORE ACTIONABLE DISCIPLINE RECOMMENDATION", color = CosmicBlue, fontWeight = FontWeight.Bold, fontSize = 11.sp, letterSpacing = 1.sp)
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = trade.aiRecommendation,
                            color = OffWhite,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            lineHeight = 18.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun QuestionSwitchRow(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    testTag: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, color = OffWhite, fontSize = 12.sp)
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            modifier = Modifier.testTag(testTag),
            colors = SwitchDefaults.colors(
                checkedThumbColor = CosmicTeal,
                checkedTrackColor = CosmicTeal.copy(alpha = 0.5f)
            )
        )
    }
}
