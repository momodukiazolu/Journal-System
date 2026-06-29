@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class, androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
package com.example.ui.screens

import androidx.compose.animation.*
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
import com.example.data.TradingPlanEntity
import com.example.ui.JournalViewModel
import com.example.ui.theme.*

@Composable
fun TradingPlanScreen(
    viewModel: JournalViewModel,
    onSuccess: () -> Unit,
    onBack: () -> Unit
) {
    val plan by viewModel.tradingPlan.collectAsState()

    var balanceInput by remember { mutableStateOf("") }
    var riskInput by remember { mutableStateOf("") }
    var maxDailyLossInput by remember { mutableStateOf("") }
    var maxWeeklyLossInput by remember { mutableStateOf("") }
    var maxDrawdownInput by remember { mutableStateOf("") }
    var maxTradesInput by remember { mutableStateOf("") }
    var maxConsecutiveLossesInput by remember { mutableStateOf("") }
    var resetTimeInput by remember { mutableStateOf("") }
    var cooldownDuration by remember { mutableStateOf("Rest of Day") }

    // Required SMC confluences
    var reqLiquiditySweep by remember { mutableStateOf(true) }
    var reqBos by remember { mutableStateOf(true) }
    var reqChoch by remember { mutableStateOf(true) }
    var reqFvg by remember { mutableStateOf(false) }
    var minRrInput by remember { mutableStateOf("2.0") }

    // Prop Firm Mode Settings
    var propFirmMode by remember { mutableStateOf(false) }
    var propFirmPreset by remember { mutableStateOf("Custom") } // FTMO, FundedNext, Custom
    var propFirmAccountSizeInput by remember { mutableStateOf("10000.0") }
    var propFirmDailyLimitInput by remember { mutableStateOf("5.0") }
    var propFirmMaxLimitInput by remember { mutableStateOf("10.0") }
    var propFirmTargetInput by remember { mutableStateOf("8.0") }

    // Populate values from saved plan
    LaunchedEffect(plan) {
        balanceInput = plan.accountBalance.toString()
        riskInput = plan.riskPerTradePercent.toString()
        maxDailyLossInput = plan.maxDailyLossPercent.toString()
        maxWeeklyLossInput = plan.maxWeeklyLossPercent.toString()
        maxDrawdownInput = plan.maxDrawdownPercent.toString()
        maxTradesInput = plan.maxTradesPerDay.toString()
        maxConsecutiveLossesInput = plan.maxConsecutiveLosses.toString()
        resetTimeInput = plan.dailyResetTime
        cooldownDuration = plan.cooldownDuration

        reqLiquiditySweep = plan.reqLiquiditySweep
        reqBos = plan.reqBos
        reqChoch = plan.reqChoch
        reqFvg = plan.reqFvg
        minRrInput = plan.minRr.toString()

        propFirmMode = plan.propFirmMode
        propFirmPreset = plan.propFirmPreset
        propFirmAccountSizeInput = plan.propFirmAccountSize.toString()
        propFirmDailyLimitInput = plan.propFirmDailyDrawdownLimit.toString()
        propFirmMaxLimitInput = plan.propFirmMaxDrawdownLimit.toString()
        propFirmTargetInput = plan.propFirmProfitTarget.toString()
    }

    // Apply Prop Firm Preset Defaults
    fun applyPreset(presetName: String) {
        propFirmPreset = presetName
        when (presetName) {
            "FTMO" -> {
                propFirmDailyLimitInput = "5.0"
                propFirmMaxLimitInput = "10.0"
                propFirmTargetInput = "10.0"
                maxDailyLossInput = "5.0"
                maxDrawdownInput = "10.0"
            }
            "FundedNext" -> {
                propFirmDailyLimitInput = "5.0"
                propFirmMaxLimitInput = "10.0"
                propFirmTargetInput = "8.0"
                maxDailyLossInput = "5.0"
                maxDrawdownInput = "10.0"
            }
        }
    }

    Scaffold(
        topBar = {
            OptIn(ExperimentalMaterial3Api::class)
            TopAppBar(
                title = { Text("TRADING PLAN & RULES", fontWeight = FontWeight.Bold, fontSize = 16.sp, letterSpacing = 1.sp) },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("plan_back_button")) {
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
            // Section 1: Account Settings & Limits Card
            Card(
                colors = CardDefaults.cardColors(containerColor = SlateDark),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(text = "PERSONAL RISK SETTINGS", color = MutedSteel, fontWeight = FontWeight.Bold, fontSize = 11.sp, letterSpacing = 1.sp)

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedTextField(
                            value = balanceInput,
                            onValueChange = { balanceInput = it },
                            label = { Text("Account Balance ($)") },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("plan_balance_input"),
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
                            value = riskInput,
                            onValueChange = { riskInput = it },
                            label = { Text("Risk Per Trade %") },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("plan_risk_input"),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = CosmicTeal,
                                unfocusedBorderColor = SlateCard,
                                focusedTextColor = OffWhite,
                                unfocusedTextColor = OffWhite
                            )
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedTextField(
                            value = maxDailyLossInput,
                            onValueChange = { maxDailyLossInput = it },
                            label = { Text("Max Daily Loss %") },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("plan_max_daily_loss_input"),
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
                            value = maxTradesInput,
                            onValueChange = { maxTradesInput = it },
                            label = { Text("Max Trades / Day") },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("plan_max_trades_input"),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = CosmicTeal,
                                unfocusedBorderColor = SlateCard,
                                focusedTextColor = OffWhite,
                                unfocusedTextColor = OffWhite
                            )
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedTextField(
                            value = maxConsecutiveLossesInput,
                            onValueChange = { maxConsecutiveLossesInput = it },
                            label = { Text("Max Consecutive Losses") },
                            modifier = Modifier
                                .weight(1.2f)
                                .testTag("plan_max_consecutive_losses_input"),
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
                            value = resetTimeInput,
                            onValueChange = { resetTimeInput = it },
                            label = { Text("Daily Reset") },
                            modifier = Modifier
                                .weight(0.8f)
                                .testTag("plan_reset_time_input"),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = CosmicTeal,
                                unfocusedBorderColor = SlateCard,
                                focusedTextColor = OffWhite,
                                unfocusedTextColor = OffWhite
                            )
                        )
                    }

                    // Cooldown Duration Picker
                    Column {
                        Text("Consecutive Loss Cooldown Duration", color = MutedSteel, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf("1 hour", "4 hours", "Rest of Day").forEach { duration ->
                                val selected = cooldownDuration == duration
                                Surface(
                                    color = if (selected) CosmicTeal else SlateCard,
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable { cooldownDuration = duration }
                                        .testTag("cooldown_chip_$duration")
                                ) {
                                    Text(
                                        text = duration,
                                        color = if (selected) SpaceBlack else OffWhite,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp,
                                        modifier = Modifier.padding(vertical = 8.dp),
                                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Section 2: SMC Setup Guidelines Compliance
            Card(
                colors = CardDefaults.cardColors(containerColor = SlateDark),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(text = "REQUIRED PLAN ENTRY RULES", color = MutedSteel, fontWeight = FontWeight.Bold, fontSize = 11.sp, letterSpacing = 1.sp)
                    Text("These confluences are required to make a trade plan-compliant:", color = MutedSteel, fontSize = 11.sp)

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Liquidity Sweep Required", color = OffWhite, fontSize = 13.sp)
                        Switch(
                            checked = reqLiquiditySweep,
                            onCheckedChange = { reqLiquiditySweep = it },
                            modifier = Modifier.testTag("switch_req_liquidity"),
                            colors = SwitchDefaults.colors(checkedThumbColor = CosmicTeal, checkedTrackColor = CosmicTeal.copy(alpha = 0.5f))
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("BOS Required", color = OffWhite, fontSize = 13.sp)
                        Switch(
                            checked = reqBos,
                            onCheckedChange = { reqBos = it },
                            modifier = Modifier.testTag("switch_req_bos"),
                            colors = SwitchDefaults.colors(checkedThumbColor = CosmicTeal, checkedTrackColor = CosmicTeal.copy(alpha = 0.5f))
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("CHOCH Required", color = OffWhite, fontSize = 13.sp)
                        Switch(
                            checked = reqChoch,
                            onCheckedChange = { reqChoch = it },
                            modifier = Modifier.testTag("switch_req_choch"),
                            colors = SwitchDefaults.colors(checkedThumbColor = CosmicTeal, checkedTrackColor = CosmicTeal.copy(alpha = 0.5f))
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("FVG Required", color = OffWhite, fontSize = 13.sp)
                        Switch(
                            checked = reqFvg,
                            onCheckedChange = { reqFvg = it },
                            modifier = Modifier.testTag("switch_req_fvg"),
                            colors = SwitchDefaults.colors(checkedThumbColor = CosmicTeal, checkedTrackColor = CosmicTeal.copy(alpha = 0.5f))
                        )
                    }

                    Divider(color = SlateCard, modifier = Modifier.padding(vertical = 4.dp))

                    OutlinedTextField(
                        value = minRrInput,
                        onValueChange = { minRrInput = it },
                        label = { Text("Minimum Setup Risk-To-Reward (R:R)") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("plan_min_rr_input"),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = CosmicTeal,
                            unfocusedBorderColor = SlateCard,
                            focusedTextColor = OffWhite,
                            unfocusedTextColor = OffWhite
                        )
                    )
                }
            }

            // Section 3: Prop Firm Mode (Module 9)
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
                            Text(text = "PROP FIRM COMPLIANCE MODE", color = CosmicBlue, fontWeight = FontWeight.Bold, fontSize = 11.sp, letterSpacing = 1.sp)
                            Text(text = "Evaluation Drawdown Guard", color = OffWhite, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        }
                        Switch(
                            checked = propFirmMode,
                            onCheckedChange = { propFirmMode = it },
                            modifier = Modifier.testTag("switch_prop_firm_mode"),
                            colors = SwitchDefaults.colors(checkedThumbColor = CosmicBlue, checkedTrackColor = CosmicBlue.copy(alpha = 0.5f))
                        )
                    }

                    AnimatedVisibility(
                        visible = propFirmMode,
                        enter = expandVertically() + fadeIn(),
                        exit = shrinkVertically() + fadeOut()
                    ) {
                        Column(
                            modifier = Modifier.padding(top = 16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                text = "Select Firm Preset Template:",
                                color = MutedSteel,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                listOf("FTMO", "FundedNext", "Custom").forEach { preset ->
                                    val selected = propFirmPreset == preset
                                    Surface(
                                        color = if (selected) CosmicBlue else SlateCard,
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier
                                            .weight(1f)
                                            .clickable { applyPreset(preset) }
                                            .testTag("preset_chip_$preset")
                                    ) {
                                        Text(
                                            text = preset,
                                            color = if (selected) SpaceBlack else OffWhite,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 11.sp,
                                            modifier = Modifier.padding(vertical = 8.dp),
                                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(4.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                OutlinedTextField(
                                    value = propFirmAccountSizeInput,
                                    onValueChange = { propFirmAccountSizeInput = it },
                                    label = { Text("Firm Capital Size ($)") },
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("prop_firm_size_input"),
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    singleLine = true,
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = CosmicBlue,
                                        unfocusedBorderColor = SlateCard,
                                        focusedTextColor = OffWhite,
                                        unfocusedTextColor = OffWhite
                                    )
                                )

                                OutlinedTextField(
                                    value = propFirmTargetInput,
                                    onValueChange = { propFirmTargetInput = it },
                                    label = { Text("Profit Target %") },
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("prop_firm_target_input"),
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    singleLine = true,
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = CosmicBlue,
                                        unfocusedBorderColor = SlateCard,
                                        focusedTextColor = OffWhite,
                                        unfocusedTextColor = OffWhite
                                    )
                                )
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                OutlinedTextField(
                                    value = propFirmDailyLimitInput,
                                    onValueChange = { propFirmDailyLimitInput = it },
                                    label = { Text("Firm Daily Loss %") },
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("prop_firm_daily_loss_input"),
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    singleLine = true,
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = CosmicBlue,
                                        unfocusedBorderColor = SlateCard,
                                        focusedTextColor = OffWhite,
                                        unfocusedTextColor = OffWhite
                                    )
                                )

                                OutlinedTextField(
                                    value = propFirmMaxLimitInput,
                                    onValueChange = { propFirmMaxLimitInput = it },
                                    label = { Text("Firm Max Drawdown %") },
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("prop_firm_max_loss_input"),
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    singleLine = true,
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = CosmicBlue,
                                        unfocusedBorderColor = SlateCard,
                                        focusedTextColor = OffWhite,
                                        unfocusedTextColor = OffWhite
                                    )
                                )
                            }

                            Text(
                                text = "⚠️ Note: Manual override bypass is completely locked in Prop Firm Mode to protect evaluation credentials.",
                                color = TradeWarning,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }
                    }
                }
            }

            // Save Rules Button
            Button(
                onClick = {
                    val currentPlan = TradingPlanEntity(
                        id = 1,
                        accountBalance = balanceInput.toDoubleOrNull() ?: 10000.0,
                        riskPerTradePercent = riskInput.toDoubleOrNull() ?: 1.0,
                        maxDailyLossPercent = maxDailyLossInput.toDoubleOrNull() ?: 5.0,
                        maxWeeklyLossPercent = maxWeeklyLossInput.toDoubleOrNull() ?: 10.0,
                        maxDrawdownPercent = maxDrawdownInput.toDoubleOrNull() ?: 10.0,
                        maxTradesPerDay = maxTradesInput.toIntOrNull() ?: 3,
                        maxConsecutiveLosses = maxConsecutiveLossesInput.toIntOrNull() ?: 2,
                        dailyResetTime = resetTimeInput,
                        cooldownDuration = cooldownDuration,
                        reqLiquiditySweep = reqLiquiditySweep,
                        reqBos = reqBos,
                        reqChoch = reqChoch,
                        reqFvg = reqFvg,
                        minRr = minRrInput.toDoubleOrNull() ?: 2.0,
                        propFirmMode = propFirmMode,
                        propFirmPreset = propFirmPreset,
                        propFirmAccountSize = propFirmAccountSizeInput.toDoubleOrNull() ?: 10000.0,
                        propFirmDailyDrawdownLimit = propFirmDailyLimitInput.toDoubleOrNull() ?: 5.0,
                        propFirmMaxDrawdownLimit = propFirmMaxLimitInput.toDoubleOrNull() ?: 10.0,
                        propFirmProfitTarget = propFirmTargetInput.toDoubleOrNull() ?: 8.0
                    )
                    viewModel.updateTradingPlan(currentPlan)
                    onSuccess()
                },
                colors = ButtonDefaults.buttonColors(containerColor = CosmicTeal, contentColor = SpaceBlack),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("save_trading_plan_button")
            ) {
                Icon(Icons.Default.Save, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("SAVE PLAN & RISK PARAMETERS", fontWeight = FontWeight.Bold, fontSize = 14.sp)
            }
        }
    }
}
