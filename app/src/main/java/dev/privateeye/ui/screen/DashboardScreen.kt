package dev.privateeye.ui.screen

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import dev.privateeye.ui.component.TerminalConsole

/**
 * Main dashboard screen for PrivateEye
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel = hiltViewModel()
) {
    val logs by viewModel.logs.collectAsState()
    val shizukuStatus by viewModel.shizukuStatus.collectAsState()
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        "PrivateEye",
                        style = MaterialTheme.typography.titleLarge
                    ) 
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.primary,
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Status Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Shizuku Status",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    
                    StatusRow("Installed", shizukuStatus.isInstalled)
                    StatusRow("Running", shizukuStatus.isRunning)
                    StatusRow("Permission", shizukuStatus.hasPermission)
                    StatusRow("Service Bound", shizukuStatus.isServiceBound)
                }
            }
            
            // Control Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = { viewModel.requestShizukuPermission() },
                    modifier = Modifier.weight(1f),
                    enabled = shizukuStatus.isRunning && !shizukuStatus.hasPermission
                ) {
                    Text("Request Permission")
                }
                
                Button(
                    onClick = { viewModel.bindService() },
                    modifier = Modifier.weight(1f),
                    enabled = shizukuStatus.hasPermission && !shizukuStatus.isServiceBound
                ) {
                    Text("Bind Service")
                }
            }
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = { viewModel.captureScreen() },
                    modifier = Modifier.weight(1f),
                    enabled = shizukuStatus.isServiceBound
                ) {
                    Text("Capture Screen")
                }
                
                OutlinedButton(
                    onClick = { viewModel.clearLogs() },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Clear Logs")
                }
            }
            
            // Terminal Console
            Text(
                text = "Console",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )
            
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                TerminalConsole(
                    logs = logs,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}

@Composable
private fun StatusRow(label: String, isActive: Boolean) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
        )
        
        Surface(
            shape = MaterialTheme.shapes.small,
            color = if (isActive) MaterialTheme.colorScheme.primary 
                   else MaterialTheme.colorScheme.error
        ) {
            Text(
                text = if (isActive) "✓" else "✗",
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onPrimary
            )
        }
    }
}
