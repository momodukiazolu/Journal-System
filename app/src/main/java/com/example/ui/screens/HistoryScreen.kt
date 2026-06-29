@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class, androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.TradeEntity
import com.example.ui.JournalViewModel
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun HistoryScreen(
    viewModel: JournalViewModel,
    onNavigateToTradeDetail: (Int) -> Unit,
    onBack: () -> Unit
) {
    val trades by viewModel.trades.collectAsState()
    var filterState by remember { mutableStateOf("All") } // "All", "Open", "Closed"

    val filteredTrades = remember(trades, filterState) {
        when (filterState) {
            "Open" -> trades.filter { it.status == "OPEN" }
            "Closed" -> trades.filter { it.status == "CLOSED" }
            else -> trades
        }
    }

    Scaffold(
        topBar = {
            OptIn(ExperimentalMaterial3Api::class)
            TopAppBar(
                title = { Text("TRADE JOURNAL LOGS", fontWeight = FontWeight.Bold, fontSize = 16.sp, letterSpacing = 1.sp) },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("history_back_button")) {
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
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Filter Tabs Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(SlateDark, RoundedCornerShape(12.dp))
                    .padding(4.dp)
            ) {
                listOf("All", "Open", "Closed").forEach { tab ->
                    val selected = filterState == tab
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(38.dp)
                            .background(
                                if (selected) CosmicTeal else androidx.compose.ui.graphics.Color.Transparent,
                                RoundedCornerShape(8.dp)
                            )
                            .clickable { filterState = tab }
                            .testTag("history_filter_chip_$tab"),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "$tab Trades",
                            color = if (selected) SpaceBlack else OffWhite,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }
                }
            }

            // Journal List
            if (filteredTrades.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.DoneAll, contentDescription = null, tint = MutedSteel, modifier = Modifier.size(48.dp))
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("No recorded trades found", color = OffWhite, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Text("Open positions or closed setups will appear here.", color = MutedSteel, fontSize = 11.sp, modifier = Modifier.padding(top = 4.dp))
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(filteredTrades, key = { it.id }) { trade ->
                        TradeHistoryRow(
                            trade = trade,
                            onClick = { onNavigateToTradeDetail(trade.id) },
                            onDelete = { viewModel.deleteTrade(trade.id) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun TradeHistoryRow(
    trade: TradeEntity,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    val dateStr = remember(trade.date) {
        SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault()).format(Date(trade.date))
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = SlateDark),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Row 1: Header (Pair + Dir + Delete)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = trade.pair,
                        color = OffWhite,
                        fontWeight = FontWeight.Black,
                        fontSize = 18.sp
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
                    
                    Spacer(modifier = Modifier.width(8.dp))
                    
                    // Compliance Badge
                    Surface(
                        color = if (trade.planCompliant) TradeProfit.copy(alpha = 0.15f) else TradeWarning.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = if (trade.planCompliant) "COMPLIANT" else "NON-COMPLIANT",
                            color = if (trade.planCompliant) TradeProfit else TradeWarning,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.testTag("delete_trade_${trade.id}")
                ) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MutedSteel, modifier = Modifier.size(20.dp))
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Row 2: Metrics Grid
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("ENTRY / LOTS", color = MutedSteel, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    Text("${trade.entryPrice} (${trade.lotSize} Lots)", color = OffWhite, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                }

                Column {
                    Text("STOP LOSS", color = MutedSteel, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    Text(trade.stopLoss.toString(), color = OffWhite, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text("REALIZED P&L", color = MutedSteel, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    if (trade.status == "OPEN") {
                        Text("OPEN POSITION", color = CosmicBlue, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    } else {
                        val pnl = trade.profitLoss ?: 0.0
                        Text(
                            text = String.format(Locale.US, "%s$%.2f", if (pnl >= 0.0) "+" else "", pnl),
                            color = if (pnl >= 0.0) TradeProfit else TradeLoss,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Divider(color = SlateCard, modifier = Modifier.padding(vertical = 12.dp))

            // Row 3: Bottom metadata
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = "Setup Score: ${trade.entryScore}/10", color = OffWhite, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Text(text = "$dateStr (${trade.session})", color = MutedSteel, fontSize = 11.sp)
            }
        }
    }
}
