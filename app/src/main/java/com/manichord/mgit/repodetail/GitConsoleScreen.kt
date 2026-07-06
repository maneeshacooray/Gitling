package com.manichord.mgit.repodetail

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import me.sheimi.sgit.database.models.Repo

private val QUICK_COMMANDS = listOf(
    "status",
    "log --oneline -10",
    "log --oneline --graph -10",
    "diff",
    "diff --staged",
    "branch",
    "branch -a",
    "stash list",
    "stash push",
    "stash pop",
    "fetch",
    "pull",
)

@Composable
fun GitConsoleScreen(
    viewModel: RepoDetailViewModel,
    repo: Repo,
    modifier: Modifier = Modifier
) {
    val entries by viewModel.consoleEntries.observeAsState(emptyList())
    val running by viewModel.consoleRunning.observeAsState(false)
    var inputText by remember { mutableStateOf("") }
    val commandHistory = remember { mutableStateListOf<String>() }
    var historyIndex by remember { mutableIntStateOf(-1) }
    val listState = rememberLazyListState()

    LaunchedEffect(entries.size) {
        if (entries.isNotEmpty()) listState.animateScrollToItem(entries.size - 1)
    }

    fun submit(cmd: String) {
        val trimmed = cmd.trim()
        if (trimmed.isEmpty()) return
        if (commandHistory.isEmpty() || commandHistory.last() != trimmed) {
            commandHistory.add(trimmed)
        }
        historyIndex = -1
        inputText = ""
        viewModel.runConsoleCommand(repo, trimmed)
    }

    Column(modifier = modifier.fillMaxSize()) {
        // Output area
        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (entries.isEmpty()) {
                item {
                    Text(
                        text = "Git command console\nType a command below or use the quick-action chips.\nType 'help' to see supported commands.",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            }
            items(entries) { entry ->
                ConsoleEntryRow(entry)
            }
            if (running) {
                item {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 2.dp)
                        Spacer(Modifier.width(8.dp))
                        Text("running…", fontFamily = FontFamily.Monospace, fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }

        HorizontalDivider()

        // Quick-action chips
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 8.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            QUICK_COMMANDS.forEach { cmd ->
                SuggestionChip(
                    onClick = { submit(cmd) },
                    label = { Text(cmd, fontSize = 11.sp) },
                    enabled = !running
                )
            }
        }

        // Input row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 8.dp, end = 8.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Up/Down history buttons
            IconButton(
                onClick = {
                    if (commandHistory.isEmpty()) return@IconButton
                    historyIndex = (historyIndex + 1).coerceAtMost(commandHistory.size - 1)
                    inputText = commandHistory[commandHistory.size - 1 - historyIndex]
                },
                modifier = Modifier.size(36.dp),
                enabled = commandHistory.isNotEmpty()
            ) {
                Text("↑", fontSize = 16.sp)
            }
            IconButton(
                onClick = {
                    if (historyIndex <= 0) { historyIndex = -1; inputText = "" }
                    else { historyIndex--; inputText = commandHistory[commandHistory.size - 1 - historyIndex] }
                },
                modifier = Modifier.size(36.dp),
                enabled = historyIndex >= 0
            ) {
                Text("↓", fontSize = 16.sp)
            }

            OutlinedTextField(
                value = inputText,
                onValueChange = { inputText = it; historyIndex = -1 },
                modifier = Modifier.weight(1f),
                placeholder = { Text("git status, log, diff…", fontSize = 13.sp, fontFamily = FontFamily.Monospace) },
                singleLine = true,
                textStyle = LocalTextStyle.current.copy(fontFamily = FontFamily.Monospace, fontSize = 13.sp),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                keyboardActions = KeyboardActions(onSend = { submit(inputText) }),
                enabled = !running,
                trailingIcon = {
                    if (inputText.isNotEmpty()) {
                        IconButton(onClick = { inputText = "" }) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear", modifier = Modifier.size(18.dp))
                        }
                    }
                }
            )

            IconButton(
                onClick = { submit(inputText) },
                enabled = inputText.isNotBlank() && !running
            ) {
                Icon(Icons.Default.Send, contentDescription = "Run")
            }
        }
    }
}

@Composable
private fun ConsoleEntryRow(entry: RepoDetailViewModel.ConsoleEntry) {
    val promptColor = MaterialTheme.colorScheme.primary
    Column {
        // Command prompt line
        Text(
            text = "$ ${entry.command}",
            fontFamily = FontFamily.Monospace,
            fontSize = 13.sp,
            color = promptColor,
            modifier = Modifier.fillMaxWidth()
        )
        // Output
        if (entry.output.isNotEmpty()) {
            Text(
                text = entry.output,
                fontFamily = FontFamily.Monospace,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurface,
                lineHeight = 18.sp,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 2.dp)
            )
        }
    }
}
