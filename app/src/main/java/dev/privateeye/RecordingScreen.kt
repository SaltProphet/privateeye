package dev.privateeye

import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.ui.platform.LocalContext
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
 * RecordingScreen - MediaProjection-based screen recording interface
 */
@Composable
fun RecordingScreen(
    viewModel: RecordingViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val activity = context as? Activity
    
    val isRecording by viewModel.isRecording.collectAsState()
    val consoleLogs by viewModel.consoleLogs.collectAsState()
    val hasOverlayPermission by viewModel.hasOverlayPermission.collectAsState()
    
    // MediaProjection permission launcher
    val mediaProjectionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        viewModel.handleMediaProjectionResult(result.resultCode, result.data)
    }
    
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
            RecordingHeader(isRecording = isRecording)
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Controls
            RecordingControls(
                isRecording = isRecording,
                hasOverlayPermission = hasOverlayPermission,
                onRequestOverlay = {
                    activity?.let { viewModel.requestOverlayPermission(it) }
                },
                onRequestMediaProjection = {
                    viewModel.requestMediaProjection(mediaProjectionLauncher)
                },
                onStartOverlay = { viewModel.startOverlayService() },
                onStopOverlay = { viewModel.stopOverlayService() },
                onStartRecording = { viewModel.startRecording() },
                onStopRecording = { viewModel.stopRecording() },
                onClearLogs = { viewModel.clearLogs() }
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Console
            RecordingConsole(
                logs = consoleLogs,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            )
        }
    }
}

@Composable
private fun RecordingHeader(isRecording: Boolean) {
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
        
        // Recording status LED
        Box(
            modifier = Modifier
                .size(16.dp)
                .background(
                    color = if (isRecording) Color.Red else MatrixGreen,
                    shape = CircleShape
                )
        )
    }
}

@Composable
private fun RecordingControls(
    isRecording: Boolean,
    hasOverlayPermission: Boolean,
    onRequestOverlay: () -> Unit,
    onRequestMediaProjection: () -> Unit,
    onStartOverlay: () -> Unit,
    onStopOverlay: () -> Unit,
    onStartRecording: () -> Unit,
    onStopRecording: () -> Unit,
    onClearLogs: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Overlay Permission Button
        if (!hasOverlayPermission) {
            OutlinedButton(
                onClick = onRequestOverlay,
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = MatrixGreen
                ),
                border = androidx.compose.foundation.BorderStroke(1.dp, MatrixGreen),
                modifier = Modifier.fillMaxWidth(0.8f)
            ) {
                Text(
                    text = "GRANT OVERLAY PERMISSION",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 14.sp
                )
            }
        }
        
        // MediaProjection Permission Button
        OutlinedButton(
            onClick = onRequestMediaProjection,
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = MatrixGreen
            ),
            border = androidx.compose.foundation.BorderStroke(1.dp, MatrixGreen),
            modifier = Modifier.fillMaxWidth(0.8f)
        ) {
            Text(
                text = "SETUP SCREEN CAPTURE",
                fontFamily = FontFamily.Monospace,
                fontSize = 14.sp
            )
        }
        
        // Overlay Service Controls
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            OutlinedButton(
                onClick = onStartOverlay,
                enabled = hasOverlayPermission,
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = MatrixGreen
                ),
                border = androidx.compose.foundation.BorderStroke(1.dp, MatrixGreen)
            ) {
                Text(
                    text = "START OVERLAY",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 12.sp
                )
            }
            
            OutlinedButton(
                onClick = onStopOverlay,
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = MatrixGreen
                ),
                border = androidx.compose.foundation.BorderStroke(1.dp, MatrixGreen)
            ) {
                Text(
                    text = "STOP OVERLAY",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 12.sp
                )
            }
        }
        
        // Recording Controls
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            Button(
                onClick = onStartRecording,
                enabled = !isRecording,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MatrixGreen,
                    contentColor = PureBlack
                )
            ) {
                Text(
                    text = "START RECORDING",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            
            Button(
                onClick = onStopRecording,
                enabled = isRecording,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.Red,
                    contentColor = Color.White
                )
            ) {
                Text(
                    text = "STOP RECORDING",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
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

@Composable
private fun RecordingConsole(
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
