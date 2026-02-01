package dev.privateeye

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import kotlinx.coroutines.launch

// Colors
private val PureBlack = Color(0xFF000000)
private val MatrixGreen = Color(0xFF00FF00)
private val ConsoleGreen = Color(0xFF00FF00).copy(alpha = 0.9f)

/**
 * Main screen for PrivateEye
 * Features a terminal-style interface with Ghost Mode toggle and console logs
 */
@Composable
fun MainScreen(
    viewModel: MainViewModel = hiltViewModel()
) {
    val isGhostModeActive by viewModel.isGhostModeActive.collectAsState()
    val isShizukuConnected by viewModel.isShizukuConnected.collectAsState()
    val consoleLogs by viewModel.consoleLogs.collectAsState()
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(PureBlack)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            // Header
            Header(isShizukuConnected = isShizukuConnected)
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Controls
            Controls(
                isGhostModeActive = isGhostModeActive,
                onToggleGhostMode = { viewModel.toggleGhostMode() },
                onClearLogs = { viewModel.clearLogs() }
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Console
            Console(
                logs = consoleLogs,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            )
        }
    }
}

/**
 * Header with app title and connection status LED
 */
@Composable
private fun Header(isShizukuConnected: Boolean) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "PRIVATE EYE",
            color = MatrixGreen,
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
            letterSpacing = 2.sp
        )
        
        // Connection status LED
        Box(
            modifier = Modifier
                .size(16.dp)
                .background(
                    color = if (isShizukuConnected) MatrixGreen else Color.Red,
                    shape = CircleShape
                )
        )
    }
}

/**
 * Control section with Ghost Mode toggle and Clear Logs button
 */
@Composable
private fun Controls(
    isGhostModeActive: Boolean,
    onToggleGhostMode: () -> Unit,
    onClearLogs: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Ghost Mode Toggle
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "GHOST MODE",
                color = MatrixGreen,
                fontSize = 18.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(end = 16.dp)
            )
            
            Switch(
                checked = isGhostModeActive,
                onCheckedChange = { onToggleGhostMode() },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = MatrixGreen,
                    checkedTrackColor = MatrixGreen.copy(alpha = 0.5f),
                    uncheckedThumbColor = Color.Gray,
                    uncheckedTrackColor = Color.DarkGray
                )
            )
        }
        
        // Clear Logs Button
        OutlinedButton(
            onClick = onClearLogs,
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = MatrixGreen
            ),
            border = androidx.compose.foundation.BorderStroke(1.dp, MatrixGreen)
        ) {
            Text(
                text = "CLEAR LOGS",
                fontFamily = FontFamily.Monospace,
                fontSize = 14.sp
            )
        }
    }
}

/**
 * Console component - scrollable terminal-style log display
 */
@Composable
private fun Console(
    logs: List<String>,
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    
    // Auto-scroll to bottom when new logs arrive
    LaunchedEffect(logs.size) {
        if (logs.isNotEmpty()) {
            coroutineScope.launch {
                listState.animateScrollToItem(logs.size - 1)
            }
        }
    }
    
    Column(modifier = modifier) {
        Text(
            text = ">> CONSOLE",
            color = MatrixGreen,
            fontSize = 16.sp,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = PureBlack,
            border = androidx.compose.foundation.BorderStroke(1.dp, MatrixGreen.copy(alpha = 0.3f))
        ) {
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(8.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                if (logs.isEmpty()) {
                    item {
                        Text(
                            text = "$ Waiting for input...",
                            color = ConsoleGreen.copy(alpha = 0.5f),
                            fontSize = 14.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
                
                items(logs) { log ->
                    Text(
                        text = log,
                        color = ConsoleGreen,
                        fontSize = 14.sp,
                        fontFamily = FontFamily.Monospace,
                        lineHeight = 20.sp
                    )
                }
            }
        }
    }
}
