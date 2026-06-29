package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.RiskLockEventEntity
import com.example.data.TradeEntity
import com.example.data.TradingPlanEntity
import com.example.ui.JournalViewModel
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.abs

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DashboardScreen(
    viewModel: JournalViewModel,
    onNavigateToLogTrade: () -> Unit,
    onNavigateToCalculator: () -> Unit,
    onNavigateToPsychology: () -> Unit,
    onNavigateToPlan: () -> Unit,
    onNavigateToHistory: () -> Unit,
    onNavigateToTradeDetail: (Int) -> Unit,
    onNavigateToBroker: () -> Unit
) {
    val plan by viewModel.tradingPlan.collectAsState()
    val trades by viewModel.trades.collectAsState()
    val activeLocks by viewModel.activeLocks.collectAsState()
    val scores by viewModel.disciplineScores.collectAsState()

    var showOverrideDialog by remember { mutableStateOf(false) }
    var overridePhraseInput by remember { mutableStateOf("") }
    val correctOverridePhrase = "BYPASS RISK LOCKS"

    // Today's Date Info
    val todayDateStr = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date()) }
    val todayScore = scores.firstOrNull { it.dateString == todayDateStr }
    val scoreVal = todayScore?.score ?: 100.0

    val todayTrades = remember(trades) {
        trades.filter {
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            sdf.format(Date(it.date)) == todayDateStr
        }
    }

    val closedToday = todayTrades.filter { it.status == "CLOSED" }
    
    // Limits Calculations
    val dailyLossLimit = plan.accountBalance * (plan.maxDailyLossPercent / 100.0)
    val realizedLossToday = closedToday.filter { (it.profitLoss ?: 0.0) < 0.0 }.sumOf { it.profitLoss ?: 0.0 }
    val dailyLossRemaining = (dailyLossLimit - abs(realizedLossToday)).coerceAtLeast(0.0)

    val tradesRemainingToday = (plan.maxTradesPerDay - todayTrades.size).coerceAtLeast(0)

    // Consecutive Losses today
    var consecutiveLosses = 0
    val sortedClosedToday = closedToday.sortedBy { it.date }
    for (trade in sortedClosedToday) {
        val pnl = trade.profitLoss ?: 0.0
        if (pnl < 0.0) {
            consecutiveLosses++
        } else if (pnl > 0.0) {
            consecutiveLosses = 0
        }
    }
    val consecutiveLossRemaining = (plan.maxConsecutiveLosses - consecutiveLosses).coerceAtLeast(0)

    // Overall metrics for Stats Strip
    val closedTrades = trades.filter { it.status == "CLOSED" }
    val winningTrades = closedTrades.filter { (it.profitLoss ?: 0.0) > 0.0 }
    val winRate = if (closedTrades.isNotEmpty()) {
        (winningTrades.size.toDouble() / closedTrades.size) * 100.0
    } else 0.0

    val totalProfit = closedTrades.filter { (it.profitLoss ?: 0.0) > 0.0 }.sumOf { it.profitLoss ?: 0.0 }
    val totalLoss = closedTrades.filter { (it.profitLoss ?: 0.0) < 0.0 }.sumOf { it.profitLoss ?: 0.0 }
    val netPnl = totalProfit + totalLoss

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(SpaceBlack)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // --- 1. Active Risk Lock Banner ---
        if (activeLocks.isNotEmpty()) {
            item {
                ActiveLocksCard(
                    activeLocks = activeLocks,
                    onBypassRequest = { showOverrideDialog = true }
                )
            }
        }

        // --- 2. Title & Date Header ---
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "APEX SMC",
                        color = CosmicTeal,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.5.sp
                    )
                    Text(
                        text = "PROCESS & RISK ENGINE",
                        color = MutedSteel,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
                
                Surface(
                    color = SlateDark,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = SimpleDateFormat("EEE, MMM dd", Locale.getDefault()).format(Date()),
                        color = OffWhite,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }

        // --- 3. Discipline Score Hero ---
        item {
            DisciplineScoreHeroCard(
                score = scoreVal,
                followedRisk = todayScore?.followedRisk ?: true,
                followedSmc = todayScore?.followedSmc ?: true,
                noOvertrading = todayScore?.noOvertrading ?: true,
                respectedDailyLimit = todayScore?.respectedDailyLimit ?: true,
                journalCompleted = todayScore?.journalCompleted ?: true
            )
        }

        // --- 4. Stat Strip Row ---
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                StatItemCard(
                    title = "Win Rate",
                    value = String.format(Locale.US, "%.1f%%", winRate),
                    subText = "${winningTrades.size} / ${closedTrades.size} Trades",
                    icon = Icons.Default.TrendingUp,
                    color = TradeProfit,
                    modifier = Modifier.weight(1f)
                )
                StatItemCard(
                    title = "Net P&L",
                    value = String.format(Locale.US, "$%.2f", netPnl),
                    subText = "Lifetime metrics",
                    icon = Icons.Default.MonetizationOn,
                    color = if (netPnl >= 0.0) TradeProfit else TradeLoss,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // --- 5. Daily Capacity / Risk Dashboard ---
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = SlateDark),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "DAILY RISK CAPACITY",
                        color = OffWhite,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        letterSpacing = 1.sp,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    // Loss Capacity
                    LimitProgressBar(
                        title = "Daily Loss Capacity",
                        remainingText = String.format(Locale.US, "$%.2f Left", dailyLossRemaining),
                        limitText = String.format(Locale.US, "Max $%.2f", dailyLossLimit),
                        progress = (dailyLossRemaining / dailyLossLimit).coerceIn(0.0, 1.0).toFloat(),
                        color = if (dailyLossRemaining < dailyLossLimit * 0.3) TradeLoss else TradeProfit
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Trade Limits
                    LimitProgressBar(
                        title = "Daily Trade Capacity",
                        remainingText = "$tradesRemainingToday Left",
                        limitText = "Max ${plan.maxTradesPerDay}",
                        progress = (tradesRemainingToday.toFloat() / plan.maxTradesPerDay).coerceIn(0f, 1f),
                        color = if (tradesRemainingToday == 0) TradeLoss else CosmicBlue
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Consecutive Losses
                    LimitProgressBar(
                        title = "Consecutive Loss Capacity",
                        remainingText = "$consecutiveLossRemaining Left",
                        limitText = "Max ${plan.maxConsecutiveLosses}",
                        progress = (consecutiveLossRemaining.toFloat() / plan.maxConsecutiveLosses).coerceIn(0f, 1f),
                        color = if (consecutiveLossRemaining == 0) TradeLoss else CosmicPurple
                    )
                }
            }
        }

        // --- 5.5. Live Broker Banner ---
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = SlateDark),
                border = BorderStroke(1.dp, CosmicTeal.copy(alpha = 0.4f)),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onNavigateToBroker() }
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Surface(
                            color = CosmicTeal.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.size(44.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.CloudQueue,
                                    contentDescription = null,
                                    tint = CosmicTeal,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                        
                        Spacer(modifier = Modifier.width(16.dp))
                        
                        Column {
                            Text(
                                text = "LIVE EXECUTION CENTER",
                                color = OffWhite,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                letterSpacing = 0.5.sp
                            )
                            Text(
                                text = "Login & place trades in real-time",
                                color = MutedSteel,
                                fontSize = 11.sp
                            )
                        }
                    }
                    
                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = null,
                        tint = MutedSteel,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }

        // --- 6. Quick Actions Grid ---
        item {
            Column {
                Text(
                    text = "QUICK ACTIONS",
                    color = MutedSteel,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                    letterSpacing = 1.2.sp,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    maxItemsInEachRow = 3,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    QuickActionButton(
                        text = "Log Trade",
                        icon = Icons.Default.AddCircle,
                        color = CosmicTeal,
                        enabled = activeLocks.isEmpty(),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("quick_action_log_trade"),
                        onClick = onNavigateToLogTrade
                    )
                    QuickActionButton(
                        text = "Calculator",
                        icon = Icons.Default.Calculate,
                        color = CosmicBlue,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("quick_action_calculator"),
                        onClick = onNavigateToCalculator
                    )
                    QuickActionButton(
                        text = "Reflect",
                        icon = Icons.Default.Book,
                        color = CosmicPurple,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("quick_action_reflect"),
                        onClick = onNavigateToPsychology
                    )
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = onNavigateToHistory,
                        colors = ButtonDefaults.buttonColors(containerColor = SlateCard),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp)
                            .testTag("quick_action_journal_history")
                    ) {
                        Icon(Icons.Default.History, contentDescription = null, tint = CosmicTeal, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Trade Journal", color = OffWhite, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }

                    Button(
                        onClick = onNavigateToPlan,
                        colors = ButtonDefaults.buttonColors(containerColor = SlateCard),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp)
                            .testTag("quick_action_trading_plan")
                    ) {
                        Icon(Icons.Default.Settings, contentDescription = null, tint = CosmicBlue, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("SMC Plan", color = OffWhite, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                }
            }
        }

        // --- 7. Pending Corrections/Nudge List ---
        val pendingTrades = trades.filter { !it.isCorrectionCompleted && it.status == "CLOSED" }
        if (pendingTrades.isNotEmpty()) {
            item {
                Text(
                    text = "AWAITING BEHAVIORAL REVIEW (${pendingTrades.size})",
                    color = TradeWarning,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                    letterSpacing = 1.2.sp,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            items(pendingTrades) { trade ->
                PendingCorrectionRow(
                    trade = trade,
                    onClick = { onNavigateToTradeDetail(trade.id) }
                )
            }
        }
    }

    // --- Override Friction Dialog ---
    if (showOverrideDialog) {
        AlertDialog(
            onDismissRequest = {
                showOverrideDialog = false
                overridePhraseInput = ""
            },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Warning, contentDescription = null, tint = TradeLoss)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("CRITICAL: Manual Override", color = OffWhite, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                }
            },
            text = {
                Column {
                    Text(
                        text = "Bypassing active risk locks violates trading discipline. A -20pt penalty will be applied to today's Discipline Score.",
                        color = OffWhite,
                        fontSize = 14.sp
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Type \"$correctOverridePhrase\" to bypass:",
                        color = MutedSteel,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = overridePhraseInput,
                        onValueChange = { overridePhraseInput = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("override_phrase_input"),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = TradeLoss,
                            unfocusedBorderColor = SlateCard,
                            focusedTextColor = OffWhite,
                            unfocusedTextColor = OffWhite
                        )
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (overridePhraseInput.trim().uppercase() == correctOverridePhrase) {
                            viewModel.bypassRiskLocksManualOverride()
                            showOverrideDialog = false
                            overridePhraseInput = ""
                        }
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = TradeLoss),
                    enabled = overridePhraseInput.trim().uppercase() == correctOverridePhrase,
                    modifier = Modifier.testTag("confirm_override_button")
                ) {
                    Text("BYPASS RULES", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showOverrideDialog = false
                    overridePhraseInput = ""
                }) {
                    Text("CANCEL", color = MutedSteel)
                }
            },
            containerColor = SlateDark
        )
    }
}

@Composable
fun ActiveLocksCard(
    activeLocks: List<RiskLockEventEntity>,
    onBypassRequest: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = TradeLoss.copy(alpha = 0.15f)),
        border = BorderStroke(1.dp, TradeLoss),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Lock, contentDescription = "Locked", tint = TradeLoss, modifier = Modifier.size(24.dp))
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "ACTIVE RISK LOCKS ENFORCED",
                    color = TradeLoss,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    letterSpacing = 1.sp
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            activeLocks.forEach { lock ->
                val typeLabel = when (lock.lockType) {
                    "DAILY_LOSS" -> "Maximum Daily Loss Reached"
                    "TRADE_LIMIT" -> "Daily Trade Allowance Depleted"
                    "CONSECUTIVE_LOSS" -> "Consecutive Losses Cooldown Activated"
                    else -> "Risk Lock Triggered"
                }
                
                val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
                val unlockTimeStr = if (lock.unlockTime > 0L) sdf.format(Date(lock.unlockTime)) else "Tomorrow Reset"

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(text = typeLabel, color = OffWhite, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                        Text(
                            text = "Unlocks at $unlockTimeStr",
                            color = MutedSteel,
                            fontSize = 11.sp
                        )
                    }
                    Icon(Icons.Default.LockClock, contentDescription = null, tint = TradeLoss, modifier = Modifier.size(16.dp))
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            
            Button(
                onClick = onBypassRequest,
                colors = ButtonDefaults.buttonColors(containerColor = TradeLoss),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier
                    .align(Alignment.End)
                    .height(36.dp)
                    .testTag("override_locks_button")
            ) {
                Text("Manual Override", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = SpaceBlack)
            }
        }
    }
}

@Composable
fun DisciplineScoreHeroCard(
    score: Double,
    followedRisk: Boolean,
    followedSmc: Boolean,
    noOvertrading: Boolean,
    respectedDailyLimit: Boolean,
    journalCompleted: Boolean
) {
    val grade = when {
        score >= 90.0 -> "Elite Trader"
        score >= 80.0 -> "Consistent Trader"
        score >= 70.0 -> "Developing"
        else -> "Improvement"
    }

    val gradeColor = when {
        score >= 90.0 -> CosmicTeal
        score >= 80.0 -> CosmicBlue
        score >= 70.0 -> TradeWarning
        else -> TradeLoss
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = SlateDark),
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Circular Score Display
                Box(
                    modifier = Modifier
                        .size(90.dp)
                        .background(SlateCard, CircleShape)
                        .padding(4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        progress = { (score / 100.0).toFloat() },
                        modifier = Modifier.fillMaxSize(),
                        color = gradeColor,
                        strokeWidth = 6.dp,
                        trackColor = SlateCard,
                    )
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = score.toInt().toString(),
                            color = OffWhite,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Black
                        )
                        Text(
                            text = "PTS",
                            color = MutedSteel,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.width(20.dp))

                // Score Details & Grade
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "TODAY'S PROCESS GRADE",
                        color = MutedSteel,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = grade.uppercase(),
                        color = gradeColor,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 0.5.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Discipline scores reward process over profits. Protect your process.",
                        color = OffWhite.copy(alpha = 0.7f),
                        fontSize = 11.sp,
                        lineHeight = 15.sp
                    )
                }
            }

            Divider(color = SlateCard, modifier = Modifier.padding(vertical = 16.dp))

            // 5 Process Checkboxes (System Verified)
            Text(
                text = "SYSTEM DISCIPLINE METRICS",
                color = MutedSteel,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.2.sp,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                ProcessIndicatorChip(label = "Risk", active = followedRisk)
                ProcessIndicatorChip(label = "SMC", active = followedSmc)
                ProcessIndicatorChip(label = "Volume", active = noOvertrading)
                ProcessIndicatorChip(label = "Limits", active = respectedDailyLimit)
                ProcessIndicatorChip(label = "Journal", active = journalCompleted)
            }
        }
    }
}

@Composable
fun ProcessIndicatorChip(label: String, active: Boolean) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = if (active) Icons.Default.CheckCircle else Icons.Default.Cancel,
            contentDescription = null,
            tint = if (active) TradeProfit else TradeLoss,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            color = if (active) OffWhite else MutedSteel,
            fontSize = 10.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
fun StatItemCard(
    title: String,
    value: String,
    subText: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = SlateDark),
        shape = RoundedCornerShape(16.dp),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = title.uppercase(), color = MutedSteel, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(16.dp))
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(text = value, color = OffWhite, fontSize = 20.sp, fontWeight = FontWeight.Black)
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = subText, color = MutedSteel, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
fun LimitProgressBar(
    title: String,
    remainingText: String,
    limitText: String,
    progress: Float,
    color: Color
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = title, color = OffWhite, fontSize = 12.sp, fontWeight = FontWeight.Medium)
            Text(text = remainingText, color = color, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(modifier = Modifier.height(6.dp))
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(CircleShape),
            color = color,
            trackColor = SlateCard
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = limitText,
            color = MutedSteel,
            fontSize = 10.sp,
            modifier = Modifier.align(Alignment.End)
        )
    }
}

@Composable
fun QuickActionButton(
    text: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color,
    enabled: Boolean = true,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (enabled) SlateCard else SlateCard.copy(alpha = 0.5f)
        ),
        shape = RoundedCornerShape(12.dp),
        modifier = modifier
            .height(80.dp)
            .clickable(enabled = enabled, onClick = onClick)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                icon,
                contentDescription = text,
                tint = if (enabled) color else MutedSteel,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = text,
                color = if (enabled) OffWhite else MutedSteel,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun PendingCorrectionRow(
    trade: TradeEntity,
    onClick: () -> Unit
) {
    val dateStr = remember(trade.date) {
        SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(trade.date))
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = SlateDark),
        border = BorderStroke(1.dp, TradeWarning),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = trade.pair,
                        color = OffWhite,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Surface(
                        color = if (trade.direction == "LONG") TradeProfit.copy(alpha = 0.15f) else TradeLoss.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = trade.direction,
                            color = if (trade.direction == "LONG") TradeProfit else TradeLoss,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Black,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Awaiting execution correction log",
                    color = MutedSteel,
                    fontSize = 11.sp
                )
            }
            
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = dateStr,
                    color = MutedSteel,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(end = 8.dp)
                )
                Icon(
                    Icons.Default.ArrowForwardIos,
                    contentDescription = null,
                    tint = TradeWarning,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}
