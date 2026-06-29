@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class, androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.JournalViewModel
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun PsychologyScreen(
    viewModel: JournalViewModel,
    onSuccess: () -> Unit,
    onBack: () -> Unit
) {
    var emotionalState by remember { mutableStateOf("Neutral") }
    var confidenceLevel by remember { mutableStateOf(5f) }
    var notesFeeling by remember { mutableStateOf("") }
    var notesInfluence by remember { mutableStateOf("") }
    var notesLearn by remember { mutableStateOf("") }

    // Active tracking tags
    val selectedTags = remember { mutableStateListOf<String>() }
    val emotionalTags = listOf("Fear", "Greed", "FOMO", "Revenge Trading", "Overconfidence", "Patience", "Discipline")

    Scaffold(
        topBar = {
            OptIn(ExperimentalMaterial3Api::class)
            TopAppBar(
                title = { Text("DAILY REFLECTION JOURNAL", fontWeight = FontWeight.Bold, fontSize = 16.sp, letterSpacing = 1.sp) },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("psychology_back_button")) {
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
            // Section 1: Emotional State Selector
            Card(
                colors = CardDefaults.cardColors(containerColor = SlateDark),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "EMOTIONAL BIAS STATE",
                        color = MutedSteel,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        letterSpacing = 1.sp,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        val states = listOf(
                            "Excellent" to "😊",
                            "Good" to "🙂",
                            "Neutral" to "😐",
                            "Stressed" to "😟",
                            "Emotional" to "😡"
                        )
                        states.forEach { (stateName, emoji) ->
                            val selected = emotionalState == stateName
                            Surface(
                                color = if (selected) CosmicTeal else SlateCard,
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { emotionalState = stateName }
                                    .testTag("emotion_chip_$stateName")
                            ) {
                                Column(
                                    modifier = Modifier.padding(vertical = 8.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(text = emoji, fontSize = 20.sp)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = stateName,
                                        color = if (selected) SpaceBlack else MutedSteel,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 9.sp,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Section 2: Confidence Rating Slider
            Card(
                colors = CardDefaults.cardColors(containerColor = SlateDark),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = "SESSION CONFIDENCE", color = MutedSteel, fontWeight = FontWeight.Bold, fontSize = 11.sp, letterSpacing = 1.sp)
                        Text(text = "${confidenceLevel.toInt()} / 10", color = CosmicTeal, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                    
                    Slider(
                        value = confidenceLevel,
                        onValueChange = { confidenceLevel = it },
                        valueRange = 1f..10f,
                        steps = 8,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("psychology_confidence_slider"),
                        colors = SliderDefaults.colors(
                            thumbColor = CosmicTeal,
                            activeTrackColor = CosmicTeal,
                            inactiveTrackColor = SlateCard
                        )
                    )
                }
            }

            // Section 3: Reflective Prompts
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
                        text = "REFLECTIVE DISCIPLINE WRITING",
                        color = MutedSteel,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        letterSpacing = 1.sp
                    )

                    OutlinedTextField(
                        value = notesFeeling,
                        onValueChange = { notesFeeling = it },
                        label = { Text("What was I feeling during open hours?") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(100.dp)
                            .testTag("notes_feeling_input"),
                        maxLines = 4,
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
                        value = notesInfluence,
                        onValueChange = { notesInfluence = it },
                        label = { Text("What external factors influenced my execution?") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(100.dp)
                            .testTag("notes_influence_input"),
                        maxLines = 4,
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
                        value = notesLearn,
                        onValueChange = { notesLearn = it },
                        label = { Text("What is my primary lesson for today's session?") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(100.dp)
                            .testTag("notes_learn_input"),
                        maxLines = 4,
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
            }

            // Section 4: Passive Emotional Tags Toggle
            Card(
                colors = CardDefaults.cardColors(containerColor = SlateDark),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "EMOTIONAL & SYSTEM COGNITIVE TAGS",
                        color = MutedSteel,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        letterSpacing = 1.sp,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    OptIn(ExperimentalLayoutApi::class)
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        emotionalTags.forEach { tag ->
                            val selected = selectedTags.contains(tag)
                            FilterChip(
                                selected = selected,
                                onClick = {
                                    if (selected) selectedTags.remove(tag) else selectedTags.add(tag)
                                },
                                modifier = Modifier.testTag("tag_chip_$tag"),
                                label = { Text(tag) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = CosmicTeal,
                                    selectedLabelColor = SpaceBlack,
                                    containerColor = SlateCard,
                                    labelColor = OffWhite
                                )
                            )
                        }
                    }
                }
            }

            // Save reflection button
            Button(
                onClick = {
                    viewModel.savePsychologyEntry(
                        emotionalState = emotionalState,
                        confidenceLevel = confidenceLevel.toInt(),
                        feeling = notesFeeling,
                        influence = notesInfluence,
                        learn = notesLearn,
                        tags = selectedTags.joinToString(",")
                    )
                    onSuccess()
                },
                colors = ButtonDefaults.buttonColors(containerColor = CosmicTeal, contentColor = SpaceBlack),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("submit_psychology_button")
            ) {
                Icon(Icons.Default.Check, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("SUBMIT PROCESS REFLECTION", fontWeight = FontWeight.Bold, fontSize = 14.sp)
            }
        }
    }
}
