package dev.privateeye.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import dev.privateeye.common.log.LogEntry
import dev.privateeye.common.log.LogLevel
import dev.privateeye.ui.theme.TerminalBackground
import dev.privateeye.ui.theme.TerminalError
import dev.privateeye.ui.theme.TerminalSuccess
import dev.privateeye.ui.theme.TerminalText
import dev.privateeye.ui.theme.TerminalWarning
import kotlinx.coroutines.launch

/**
 * Terminal-style console component for displaying logs
 */
@Composable
fun TerminalConsole(
    logs: List<LogEntry>,
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
    
    LazyColumn(
        state = listState,
        modifier = modifier
            .fillMaxWidth()
            .background(TerminalBackground)
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        if (logs.isEmpty()) {
            item {
                Text(
                    text = "$ Waiting for logs...",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TerminalText.copy(alpha = 0.5f)
                )
            }
        }
        
        items(logs) { log ->
            LogLine(log = log)
        }
    }
}

@Composable
private fun LogLine(log: LogEntry) {
    val logColor = when (log.level) {
        LogLevel.DEBUG -> TerminalText
        LogLevel.INFO -> TerminalSuccess
        LogLevel.WARNING -> TerminalWarning
        LogLevel.ERROR -> TerminalError
    }
    
    Text(
        text = log.formatForConsole(),
        style = MaterialTheme.typography.bodyMedium,
        color = logColor,
        modifier = Modifier.fillMaxWidth()
    )
}
