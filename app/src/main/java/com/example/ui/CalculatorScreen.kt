package com.example.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.database.CalculationHistory
import com.example.viewmodel.CalculatorViewModel

// Distinct Modern Dark Slate Colors
val BackColor = Color(0xFF10121A)
val CardColor = Color(0xFF1A1D2A)
val NumberKeyBg = Color(0xFF23273A)
val OperatorKeyBg = Color(0xFF7C4DFF) // Vibrant Purple
val UtilityKeyBg = Color(0xFF32374E)  // Muted Slate
val EqualKeyBg = Color(0xFF00C853)     // Premium Clean Green

val NumberKeyText = Color(0xFFFFFFFF)
val OperatorKeyText = Color(0xFFFFFFFF)
val UtilityKeyText = Color(0xFF90CAF9)  // High Contrast blue-accent for functional keys
val EqualKeyText = Color(0xFFFFFFFF)

@Composable
fun CalculatorScreen(
    viewModel: CalculatorViewModel,
    modifier: Modifier = Modifier
) {
    val expr by viewModel.expression.collectAsState()
    val previewText by viewModel.previewResult.collectAsState()
    val showHistory by viewModel.showHistory.collectAsState()
    val historyList by viewModel.historyList.collectAsState()
    val vibrationEnabled by viewModel.vibrationEnabled.collectAsState()

    val haptic = LocalHapticFeedback.current

    // Common click execution with haptics
    val performKeyClick: (String) -> Unit = { key ->
        if (vibrationEnabled) {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        }
        viewModel.onKeyPress(key)
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(BackColor)
            .statusBarsPadding()
            .navigationBarsPadding(),
        contentAlignment = Alignment.Center
    ) {
        // Enforce max container width on wide device layouts (Adaptive layout support)
        Column(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 480.dp) // Perfect ergonomic phone sizing, even on wide viewports (tablets)
                .background(BackColor),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // 1. Top Bar Header Controls
            TopToolbarHeader(
                vibrationOn = vibrationEnabled,
                onToggleVibration = {
                    if (vibrationEnabled) {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    }
                    viewModel.toggleVibration()
                },
                onOpenHistory = {
                    if (vibrationEnabled) {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    }
                    viewModel.setShowHistory(true)
                }
            )

            // 2. High-fidelity display viewport (takes generous negative space)
            DisplayViewport(
                expressionText = expr,
                previewText = previewText,
                onBackspaceClick = {
                    performKeyClick("⌫")
                }
            )

            // 3. Ergonomic key keyboard grids
            KeyboardMatrix(
                onKeyClick = performKeyClick
            )
        }

        // 4. Overlaid beautiful history bottom panel
        AnimatedVisibility(
            visible = showHistory,
            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
            modifier = Modifier.fillMaxSize()
        ) {
            HistoryPanelOverlay(
                historyList = historyList,
                onClose = { viewModel.setShowHistory(false) },
                onSelect = { item ->
                    if (vibrationEnabled) {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    }
                    viewModel.selectHistoryItem(item)
                },
                onDelete = { item ->
                    if (vibrationEnabled) {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    }
                    viewModel.deleteHistoryItem(item)
                },
                onClearAll = {
                    if (vibrationEnabled) {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    }
                    viewModel.clearHistory()
                }
            )
        }
    }
}

@Composable
fun TopToolbarHeader(
    vibrationOn: Boolean,
    onToggleVibration: () -> Unit,
    onOpenHistory: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "Calculator",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            letterSpacing = (-0.5).sp,
            modifier = Modifier.testTag("app_title")
        )

        Row {
            // Vibration setting indicator with clean dynamic text styling
            IconButton(
                onClick = onToggleVibration,
                modifier = Modifier.testTag("vibration_toggle_button")
            ) {
                Text(
                    text = if (vibrationOn) "📳" else "📴",
                    fontSize = 20.sp,
                    textAlign = TextAlign.Center
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            // History menu launcher button
            Box(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(UtilityKeyBg)
                    .clickable(onClick = onOpenHistory)
                    .padding(horizontal = 14.dp, vertical = 8.dp)
                    .testTag("history_launcher_button")
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "History",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White
                    )
                }
            }
        }
    }
}

@Composable
fun DisplayViewport(
    expressionText: String,
    previewText: String?,
    onBackspaceClick: () -> Unit
) {
    val scrollState = rememberScrollState()

    // Automatically scroll text to the end on content updates
    LaunchedEffect(expressionText) {
        scrollState.animateScrollTo(scrollState.maxValue)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        colors = CardDefaults.cardColors(containerColor = CardColor),
        shape = RoundedCornerShape(24.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Active formula expression container (fills, scrolls horizontally)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(68.dp),
                contentAlignment = Alignment.CenterEnd
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(scrollState),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = expressionText.ifEmpty { "0" },
                        fontSize = if (expressionText.length > 12) 36.sp else 46.sp,
                        fontWeight = FontWeight.Light,
                        color = Color.White,
                        textAlign = TextAlign.End,
                        maxLines = 1,
                        modifier = Modifier.testTag("expression_display_text")
                    )
                }
            }

            // Real-time live output calculation preview container
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Backspacing trigger visually docked strictly next to preview elements
                AnimatedVisibility(
                    visible = expressionText.isNotEmpty(),
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(UtilityKeyBg)
                            .clickable(onClick = onBackspaceClick)
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                            .testTag("backspace_inline_button")
                    ) {
                        Text(
                            text = "⌫",
                            color = Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                if (previewText != null && expressionText != previewText) {
                    Text(
                        text = "= $previewText",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color.White.copy(alpha = 0.5f),
                        textAlign = TextAlign.End,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("preview_display_text")
                    )
                } else {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
fun KeyboardMatrix(
    onKeyClick: (String) -> Unit
) {
    // Array map keys structure matching standard calculator
    val keyRows = listOf(
        listOf("C" to UtilityKeyBg, "()" to UtilityKeyBg, "%" to UtilityKeyBg, "÷" to OperatorKeyBg),
        listOf("7" to NumberKeyBg, "8" to NumberKeyBg, "9" to NumberKeyBg, "×" to OperatorKeyBg),
        listOf("4" to NumberKeyBg, "5" to NumberKeyBg, "6" to NumberKeyBg, "-" to OperatorKeyBg),
        listOf("1" to NumberKeyBg, "2" to NumberKeyBg, "3" to NumberKeyBg, "+" to OperatorKeyBg),
        listOf("+/-" to NumberKeyBg, "0" to NumberKeyBg, "." to NumberKeyBg, "=" to EqualKeyBg)
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        for (row in keyRows) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                for (key in row) {
                    val symbol = key.first
                    val background = key.second

                    val textColor = when (background) {
                        OperatorKeyBg -> OperatorKeyText
                        UtilityKeyBg -> UtilityKeyText
                        EqualKeyBg -> EqualKeyText
                        else -> NumberKeyText
                    }

                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .weight(1f)
                            .aspectRatio(1f)
                            .clip(CircleShape)
                            .background(background)
                            .clickable { onKeyClick(symbol) }
                            .testTag("key_$symbol")
                    ) {
                        Text(
                            text = symbol,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = textColor,
                            fontFamily = FontFamily.Default
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun HistoryPanelOverlay(
    historyList: List<CalculationHistory>,
    onClose: () -> Unit,
    onSelect: (CalculationHistory) -> Unit,
    onDelete: (CalculationHistory) -> Unit,
    onClearAll: () -> Unit
) {
    // Elegant deep overlay container
    Surface(
        color = Color.Black.copy(alpha = 0.5f),
        modifier = Modifier
            .fillMaxSize()
            .clickable(onClick = onClose) // Tap outside to dismiss
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Bottom
        ) {
            Card(
                colors = CardDefaults.cardColors(containerColor = CardColor),
                shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.75f)
                    .clickable(enabled = false, onClick = {}) // Intercept clicks inside card
                    .testTag("history_panel_card")
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp)
                ) {
                    // Header controls backstack
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Calculation History",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )

                        Row {
                            if (historyList.isNotEmpty()) {
                                IconButton(
                                    onClick = onClearAll,
                                    modifier = Modifier.testTag("clear_all_history_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = "Clear all logs",
                                        tint = Color.Red.copy(alpha = 0.8f)
                                    )
                                }
                            }

                            IconButton(
                                onClick = onClose,
                                modifier = Modifier.testTag("close_history_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Dismiss logs overlay",
                                    tint = Color.White
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    if (historyList.isEmpty()) {
                        // Empty states following visual guideline instruction
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth(),
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "📭",
                                fontSize = 48.sp,
                                modifier = Modifier.padding(bottom = 12.dp)
                            )
                            Text(
                                text = "No past calculations recorded",
                                fontSize = 16.sp,
                                color = Color.White.copy(alpha = 0.5f),
                                style = MaterialTheme.typography.bodyMedium,
                                textAlign = TextAlign.Center
                            )
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            items(historyList, key = { it.id }) { item ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(16.dp))
                                        .background(NumberKeyBg)
                                        .clickable { onSelect(item) }
                                        .padding(16.dp)
                                        .testTag("history_item_${item.id}"),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(
                                        modifier = Modifier.weight(1.0f)
                                    ) {
                                        Text(
                                            text = item.expression,
                                            fontSize = 16.sp,
                                            color = Color.White.copy(alpha = 0.6f),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = "= ${item.result}",
                                            fontSize = 20.sp,
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }

                                    IconButton(
                                        onClick = { onDelete(item) },
                                        modifier = Modifier.testTag("delete_history_item_${item.id}")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Delete,
                                            contentDescription = "Delete record",
                                            tint = Color.White.copy(alpha = 0.3f)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
