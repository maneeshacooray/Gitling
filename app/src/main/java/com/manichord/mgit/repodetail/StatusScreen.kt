package com.manichord.mgit.repodetail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.QuestionMark
import androidx.compose.material.icons.filled.RemoveCircle
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

enum class StatusChangeType { ADDED, MODIFIED, REMOVED, MISSING, UNTRACKED }

data class StatusFileEntry(val path: String, val changeType: StatusChangeType)

private fun StatusChangeType.icon() = when (this) {
    StatusChangeType.ADDED -> Icons.Default.AddCircle
    StatusChangeType.MODIFIED -> Icons.Default.Edit
    StatusChangeType.REMOVED, StatusChangeType.MISSING -> Icons.Default.RemoveCircle
    StatusChangeType.UNTRACKED -> Icons.Default.QuestionMark
}

@Composable
private fun StatusChangeType.tint() = when (this) {
    StatusChangeType.ADDED -> Color(0xFF4CAF50)
    StatusChangeType.MODIFIED -> Color(0xFFFFA000)
    StatusChangeType.REMOVED, StatusChangeType.MISSING -> MaterialTheme.colorScheme.error
    StatusChangeType.UNTRACKED -> MaterialTheme.colorScheme.onSurfaceVariant
}

@Composable
fun StatusScreen(
    isLoading: Boolean,
    isClean: Boolean,
    stagedFiles: List<StatusFileEntry>,
    unstagedFiles: List<StatusFileEntry>,
    conflictingFiles: List<String>,
    onStageFile: (String) -> Unit,
    onUnstageFile: (String) -> Unit,
    onViewStagedDiff: () -> Unit,
    onViewUnstagedDiff: () -> Unit
) {
    Box(modifier = Modifier.fillMaxSize()) {
        when {
            isLoading -> CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            isClean -> Text(
                "Nothing to commit, working directory clean",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.align(Alignment.Center).padding(32.dp)
            )
            else -> LazyColumn(modifier = Modifier.fillMaxSize()) {
                if (conflictingFiles.isNotEmpty()) {
                    item { SectionHeader("Conflicts") }
                    items(conflictingFiles) { path ->
                        ListItem(
                            headlineContent = { Text(path, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                            leadingContent = {
                                Icon(Icons.Default.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                            },
                            colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                        )
                    }
                }
                if (stagedFiles.isNotEmpty()) {
                    item { SectionHeader("Staged Changes", onViewDiff = onViewStagedDiff) }
                    items(stagedFiles, key = { "staged:" + it.path }) { entry ->
                        FileRow(entry, actionIcon = Icons.Default.RemoveCircle, actionDescription = "Unstage", onAction = { onUnstageFile(entry.path) })
                    }
                }
                if (unstagedFiles.isNotEmpty()) {
                    item { SectionHeader("Unstaged Changes", onViewDiff = onViewUnstagedDiff) }
                    items(unstagedFiles, key = { "unstaged:" + it.path }) { entry ->
                        FileRow(entry, actionIcon = Icons.Default.AddCircle, actionDescription = "Stage", onAction = { onStageFile(entry.path) })
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionHeader(title: String, onViewDiff: (() -> Unit)? = null) {
    Surface(color = MaterialTheme.colorScheme.surfaceVariant) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            if (onViewDiff != null) {
                TextButton(onClick = onViewDiff) { Text("View diff") }
            }
        }
    }
    HorizontalDivider()
}

@Composable
private fun FileRow(
    entry: StatusFileEntry,
    actionIcon: androidx.compose.ui.graphics.vector.ImageVector,
    actionDescription: String,
    onAction: () -> Unit
) {
    ListItem(
        headlineContent = { Text(entry.path, maxLines = 1, overflow = TextOverflow.Ellipsis) },
        leadingContent = {
            Icon(entry.changeType.icon(), contentDescription = entry.changeType.name, tint = entry.changeType.tint())
        },
        trailingContent = {
            IconButton(onClick = onAction) {
                Icon(actionIcon, contentDescription = actionDescription)
            }
        },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent)
    )
}
